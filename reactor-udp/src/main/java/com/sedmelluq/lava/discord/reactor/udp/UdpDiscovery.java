package com.sedmelluq.lava.discord.reactor.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;
import org.xnio.channels.MulticastMessageChannel;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class UdpDiscovery {
  private static final Logger log = LoggerFactory.getLogger(UdpDiscovery.class);

  private final UdpChannelFactory channelFactory;

  public UdpDiscovery(XnioWorker xnioWorker) {
    this.channelFactory = new UdpChannelFactory(xnioWorker);
  }

  public Mono<InetSocketAddress> handleUdpDiscovery(InetSocketAddress targetAddress, int ssrc) {
    try {
      MulticastMessageChannel channel = channelFactory.createFor(targetAddress);

      return Mono.<InetSocketAddress>create(sink -> handleUdpPackets(sink, channel, targetAddress, ssrc))
          .doFinally(signal -> closeItem(channel));
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

  private static void handleUdpPackets(final MonoSink<InetSocketAddress> sink, final MulticastMessageChannel channel,
                                       final InetSocketAddress targetAddress, final int ssrc) {

    ByteBuffer buffer = ByteBuffer.allocate(70);

    channel.getReadSetter().set(readyChannel -> {
      try {
        buffer.clear();

        int receiveResult = readyChannel.receiveFrom(null, buffer);
        if (receiveResult != buffer.capacity()) {
          throw new IllegalStateException("Should have 70 bytes of data, but received: " + receiveResult);
        }

        sink.success(extractAddressFromMessage(buffer));
      } catch (Throwable e) {
        sink.error(e);
      }
    });

    try {
      buffer.putInt(0, ssrc);

      if (!channel.sendTo(targetAddress, buffer)) {
        throw new IllegalStateException("Channel is not writable.");
      }

      channel.resumeReads();
    } catch (Exception e) {
      sink.error(e);
    }
  }

  private static InetSocketAddress extractAddressFromMessage(ByteBuffer message) {
    int ipEnd = 4;

    while (message.get(ipEnd) != 0 && ipEnd < 68) {
      ipEnd++;
    }

    String ipAddress = new String(message.array(), 4, ipEnd - 4, StandardCharsets.US_ASCII);
    return new InetSocketAddress(ipAddress, Short.reverseBytes(message.getShort(68)) & 0xFFFF);
  }

  private static void closeItem(AutoCloseable closeable) {
    try {
      closeable.close();
    } catch (Exception e) {
      log.error("Failed to close {}.", e, closeable);
    }
  }
}
