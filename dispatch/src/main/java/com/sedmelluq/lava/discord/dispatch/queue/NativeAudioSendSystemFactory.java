package com.sedmelluq.lava.discord.dispatch.queue;

import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.common.tools.ExecutorTools;
import com.sedmelluq.lava.discord.dispatch.AudioSendSystem;
import com.sedmelluq.lava.discord.dispatch.AudioSendSystemFactory;
import com.sedmelluq.lava.discord.dispatch.packet.AudioPacketProvider;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public class NativeAudioSendSystemFactory implements AudioSendSystemFactory, AutoCloseable {
  private static final int DEFAULT_BUFFER_DURATION = 400;
  private static final int PACKET_INTERVAL = 20;

  private final int bufferDuration;
  private final SocketHandles socketHandles;
  private final AtomicLong identifierCounter = new AtomicLong();
  private final KeySetView<NativeAudioSendSystem, Boolean> systems = ConcurrentHashMap.newKeySet();
  private final Object lock = new Object();
  private volatile UdpQueueManager queueManager;
  private boolean closed = false;
  private ScheduledExecutorService scheduler;

  public NativeAudioSendSystemFactory() {
    this(DEFAULT_BUFFER_DURATION, null);
  }

  public NativeAudioSendSystemFactory(int bufferDuration) {
    this(bufferDuration, null);
  }

  public NativeAudioSendSystemFactory(int bufferDuration, SocketHandles socketHandles) {
    this.bufferDuration = bufferDuration;
    this.socketHandles = socketHandles;
  }

  private void initialiseQueueManager() {
    scheduler = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory("native-udp"));
    queueManager = new UdpQueueManager(bufferDuration / PACKET_INTERVAL,
        TimeUnit.MILLISECONDS.toNanos(PACKET_INTERVAL));

    scheduler.scheduleWithFixedDelay(this::populateQueues, 0, 40, TimeUnit.MILLISECONDS);

    Thread thread = new Thread(() -> {
      if (socketHandles != null) {
        queueManager.processWithSocket(socketHandles.ipv4SocketHandle, socketHandles.ipv6SocketHandle);
      } else {
        queueManager.process();
      }
    });
    thread.setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2);
    thread.setDaemon(true);
    thread.start();
  }

  private ScheduledExecutorService shutdownQueueManager() {
    queueManager.close();
    queueManager = null;

    ScheduledExecutorService currentScheduler = scheduler;
    scheduler = null;
    return currentScheduler;
  }

  @Override
  public AudioSendSystem create(AudioPacketProvider packetProvider) {
    return new NativeAudioSendSystem(identifierCounter.incrementAndGet(), this, packetProvider);
  }

  @Override
  public void close() {
    ScheduledExecutorService schedulerToShutDown = null;

    synchronized (lock) {
      closed = true;

      synchronized (lock) {
        if (queueManager != null) {
          schedulerToShutDown = shutdownQueueManager();
        }
      }

      if (schedulerToShutDown != null) {
        ExecutorTools.shutdownExecutor(schedulerToShutDown, "native udp queue populator");
      }
    }
  }

  void addInstance(NativeAudioSendSystem system) {
    synchronized (lock) {
      if (closed) {
        throw new IllegalStateException("Factory is closed");
      }

      systems.add(system);

      if (queueManager == null) {
        initialiseQueueManager();
      }
    }
  }

  void removeInstance(NativeAudioSendSystem system) {
    ScheduledExecutorService schedulerToShutDown = null;

    synchronized (lock) {
      UdpQueueManager manager = queueManager;

      if (manager != null) {
        system.deleteQueue(manager);
      }

      if (systems.remove(system) && systems.isEmpty() && manager != null) {
        schedulerToShutDown = shutdownQueueManager();
      }
    }

    if (schedulerToShutDown != null) {
      ExecutorTools.shutdownExecutor(schedulerToShutDown, "native udp queue populator");
    }
  }

  private void populateQueues() {
    UdpQueueManager manager = queueManager; /* avoid getfield opcode */

    if (manager != null) {
      for (NativeAudioSendSystem system : systems) {
        system.populateQueue(manager);
      }
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private SocketHandles socketHandles = null;
    private int bufferDuration = DEFAULT_BUFFER_DURATION;

    public Builder socketHandles(long ipv4SocketHandle, long ipv6SocketHandle) {
      this.socketHandles = new SocketHandles(ipv4SocketHandle, ipv6SocketHandle);
      return this;
    }

    public Builder socketHandles(SocketHandles socketHandles) {
      this.socketHandles = socketHandles;
      return this;
    }

    public Builder bufferDuration(int bufferDuration) {
      this.bufferDuration = bufferDuration;
      return this;
    }

    public NativeAudioSendSystemFactory build() {
      return new NativeAudioSendSystemFactory(bufferDuration, socketHandles);
    }
  }

  public static class SocketHandles {
    public final long ipv4SocketHandle;
    public final long ipv6SocketHandle;

    public SocketHandles(long ipv4SocketHandle, long ipv6SocketHandle) {
      this.ipv4SocketHandle = ipv4SocketHandle;
      this.ipv6SocketHandle = ipv6SocketHandle;
    }
  }
}
