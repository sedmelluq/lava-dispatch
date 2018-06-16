package com.sedmelluq.lava.discord.reactor.udp;

import org.xnio.OptionMap;
import org.xnio.XnioWorker;
import org.xnio.channels.MulticastMessageChannel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class UdpChannelFactory {
  private static final InetSocketAddress IPV4_ANY = new InetSocketAddress(createIpv4AnyAddress(), 0);
  private static final InetSocketAddress IPV6_ANY = new InetSocketAddress(createIpv6AnyAddress(), 0);

  private final XnioWorker nioWorker;

  public UdpChannelFactory(XnioWorker nioWorker) {
    this.nioWorker = nioWorker;
  }

  public MulticastMessageChannel create(boolean ipv6) throws IOException {
    return nioWorker.createUdpServer(getBindAddress(ipv6), OptionMap.EMPTY);
  }

  public MulticastMessageChannel createFor(InetSocketAddress targetAddress) throws IOException {
    return nioWorker.createUdpServer(getBindAddress(targetAddress), OptionMap.EMPTY);
  }

  private static InetSocketAddress getBindAddress(InetSocketAddress targetAddress) {
    return getBindAddress(targetAddress.getAddress().getAddress().length != 4);
  }

  private static InetSocketAddress getBindAddress(boolean ipv6) {
    return ipv6 ? IPV6_ANY : IPV4_ANY;
  }

  private static InetAddress createIpv6AnyAddress() {
    try {
      return InetAddress.getByAddress("::", new byte[16]);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static InetAddress createIpv4AnyAddress() {
    try {
      return InetAddress.getByAddress("0.0.0.0", new byte[4]);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}