package com.sedmelluq.lava.discord.dispatch.queue;

import com.sedmelluq.discord.lavaplayer.natives.NativeResourceHolder;
import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManagerLibrary;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;

/**
 * Manages sending out queues of UDP packets at a fixed interval.
 */
@ThreadSafe
public class UdpQueueManager extends NativeResourceHolder {
  private final int bufferCapacity;
  private final UdpQueueManagerLibrary library;
  private final long instance;
  private boolean released;

  /**
   * @param bufferCapacity Maximum number of packets in one queue
   * @param packetInterval Time interval between packets in a queue
   */
  public UdpQueueManager(int bufferCapacity, long packetInterval) {
    this.bufferCapacity = bufferCapacity;
    library = UdpQueueManagerLibrary.getInstance();
    instance = library.create(bufferCapacity, packetInterval);
  }

  /**
   * If the queue does not exist yet, returns the maximum number of packets in a queue.
   *
   * @param key Unique queue identifier
   * @return Number of empty packet slots in the specified queue
   */
  public int getRemainingCapacity(long key) {
    synchronized (library) {
      if (released) {
        return 0;
      }

      return library.getRemainingCapacity(instance, key);
    }
  }

  /**
   * @return Total capacity used for queues in this manager.
   */
  public int getCapacity() {
    return bufferCapacity;
  }

  /**
   * Adds one packet to the specified queue. Will fail if the maximum size of the queue is reached. There is no need to
   * manually create a queue, it is automatically created when the first packet is added to it and deleted when it
   * becomes empty.
   *
   * @param key Unique queue identifier
   * @param packet Packet to add to the queue
   * @return True if adding the packet to the queue succeeded
   */
  public boolean queuePacket(long key, String hostAddress, int port, ByteBuffer buffer) {
    synchronized (library) {
      if (released) {
        return false;
      }

      return library.queuePacket(instance, key, hostAddress, port, buffer, buffer.limit());
    }
  }

  /**
   * This is the method that should be called to start processing the queues. It will use the current thread and return
   * only when close() method is called on the queue manager.
   */
  public void process() {
    library.process(instance);
  }

  @Override
  protected void freeResources() {
    synchronized (library) {
      released = true;
      library.destroy(instance);
    }
  }
}
