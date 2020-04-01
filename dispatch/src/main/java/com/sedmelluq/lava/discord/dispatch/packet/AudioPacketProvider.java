package com.sedmelluq.lava.discord.dispatch.packet;

import com.sedmelluq.lava.discord.dispatch.AudioSendSystem;
import com.sedmelluq.lava.discord.dispatch.AudioSendSystemFactory;
import com.sedmelluq.lava.discord.dispatch.OpusFrameProvider;
import com.sedmelluq.lava.discord.dispatch.SocketAddressInfo;
import java.awt.TextComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

@ThreadSafe
public class AudioPacketProvider {
  private static final Logger log = LoggerFactory.getLogger(AudioPacketProvider.class);

  private static final int OPUS_FRAME_SIZE = 960;
  private static final byte[] SILENCE_BYTES = new byte[] {(byte)0xF8, (byte)0xFF, (byte)0xFE};

  private final Consumer<Boolean> speakingStateHandler;
  private final AudioSendSystemFactory sendSystemFactory;
  private final AudioPacketBuilder packetBuilder;
  private final SocketAddressInfo addressInfo;

  private final Object systemLock = new Object();

  private volatile OpusFrameProvider frameProvider;

  @GuardedBy("systemLock")
  private AudioSendSystem sendSystem;
  @GuardedBy("systemLock")
  private boolean shutdown = false;
  @GuardedBy("packetBuilder")
  private boolean speaking;
  @GuardedBy("packetBuilder")
  private int silenceCounter = 0;
  @GuardedBy("packetBuilder")
  private boolean sentSilenceOnConnect;
  @GuardedBy("packetBuilder")
  private char sequenceNumber = 0;
  @GuardedBy("packetBuilder")
  private int timestamp = 0;

  public AudioPacketProvider(AudioSendSystemFactory sendSystemFactory, InetSocketAddress address, byte[] secretKey,
                             int sourceIdentifier, AudioPacketBuilder.NonceStrategy nonceStrategy,
                             Consumer<Boolean> speakingStateHandler, long explicitSocketHandle) {

    this.sendSystemFactory = sendSystemFactory;
    this.speakingStateHandler = speakingStateHandler;
    addressInfo = new SocketAddressInfo(address, explicitSocketHandle);
    packetBuilder = new AudioPacketBuilder(secretKey, sourceIdentifier, nonceStrategy);
  }

  public void initialize() {
    setSpeaking(true);
  }

  public SocketAddressInfo getAddressInfo() {
    return addressInfo;
  }

  public void setFrameProvider(OpusFrameProvider newFrameProvider) {
    synchronized (systemLock) {
      if (!shutdown) {
        OpusFrameProvider previousProvider = frameProvider;
        frameProvider = newFrameProvider;

        try {
          if (newFrameProvider != null) {
            if (sendSystem == null) {
              sendSystem = sendSystemFactory.create(this);
              sendSystem.start();
            }
          } else {
            stopSendSystem();
          }
        } finally {
          if (previousProvider != newFrameProvider) {
            closeFrameProvider(previousProvider);
          }
        }
      } else {
        closeFrameProvider(newFrameProvider);
      }
    }
  }

  public void shutdown() {
    synchronized (systemLock) {
      try {
        shutdown = true;
        stopSendSystem();
      } finally {
        closeFrameProvider(frameProvider);
        frameProvider = null;
      }
    }
  }

  public boolean providePacket(ByteBuffer buffer, boolean realTime) {
    synchronized (packetBuilder) {
      boolean packetReady = false;

      try {
        if (sentSilenceOnConnect && frameProvider != null &&
            frameProvider.provideOpusFrame(packetBuilder.getPayloadBuffer())) {

          silenceCounter = -1;

          packetReady = true;

          if (!speaking) {
            setSpeaking(true);
          }
        } else if (silenceCounter > -1) {
          packetBuilder.getPayloadBuffer().put(SILENCE_BYTES);
          packetReady = true;

          if (++silenceCounter > 10) {
            silenceCounter = -1;
            sentSilenceOnConnect = true;
          }
        } else if (speaking && realTime) {
          setSpeaking(false);
        }

        if (packetReady) {
          sequenceNumber++;
          timestamp += OPUS_FRAME_SIZE;

          packetBuilder.writeEncrypted(buffer, sequenceNumber, timestamp);
          return true;
        }
      } catch (Exception e) {
        log.error("Providing a packet failed.", e);
      }
    }

    return false;
  }

  private void closeFrameProvider(OpusFrameProvider provider) {
    try {
      if (provider != null) {
        provider.close();
      }
    } catch (Exception e) {
      log.error("Failed to close frame provider.", e);
    }
  }

  private void stopSendSystem() {
    if (sendSystem != null) {
      sendSystem.shutdown();
      sendSystem = null;
    }
  }

  private void setSpeaking(boolean speaking) {
    this.speaking = speaking;

    speakingStateHandler.accept(speaking);

    if (!speaking) {
      silenceCounter = 0;
    }
  }
}
