package com.sedmelluq.lava.discord.dispatch.queue;

import com.sedmelluq.lava.discord.dispatch.AudioSendSystem;
import com.sedmelluq.lava.discord.dispatch.SocketAddressInfo;
import com.sedmelluq.lava.discord.dispatch.packet.AudioPacketProvider;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;

@ThreadSafe
public class NativeAudioSendSystem implements AudioSendSystem {
  private final long queueKey;
  private final NativeAudioSendSystemFactory audioSendSystem;
  private final AudioPacketProvider packetProvider;
  private final ByteBuffer directBuffer;

  public NativeAudioSendSystem(long queueKey, NativeAudioSendSystemFactory audioSendSystem,
                               AudioPacketProvider packetProvider) {

    this.queueKey = queueKey;
    this.audioSendSystem = audioSendSystem;
    this.packetProvider = packetProvider;
    this.directBuffer = ByteBuffer.allocateDirect(1024);
  }

  @Override
  public void start() {
    audioSendSystem.addInstance(this);
  }

  @Override
  public void shutdown() {
    audioSendSystem.removeInstance(this);
  }

  public void populateQueue(UdpQueueManager queueManager) {
    int remaining = queueManager.getRemainingCapacity(queueKey);
    boolean emptyQueue = queueManager.getCapacity() - remaining > 0;

    SocketAddressInfo addressInfo = packetProvider.getAddressInfo();

    for (int i = 0; i < remaining; i++) {
      directBuffer.clear();

      if (packetProvider.providePacket(directBuffer, emptyQueue)) {
        directBuffer.flip();

        if (queueManager.queuePacket(queueKey, addressInfo.hostAddress, addressInfo.port, directBuffer,
            addressInfo.explicitSourceSocketHandle)) {
          continue;
        }
      }

      break;
    }
  }

  public void deleteQueue(UdpQueueManager queueManager) {
    queueManager.deleteQueue(queueKey);
  }
}
