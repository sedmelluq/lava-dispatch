package com.sedmelluq.lava.discord.dispatch.queue;

import com.sedmelluq.lava.discord.dispatch.AudioSendSystem;
import com.sedmelluq.lava.discord.dispatch.AudioSendSystemFactory;
import com.sedmelluq.discord.lavaplayer.tools.DaemonThreadFactory;
import com.sedmelluq.discord.lavaplayer.tools.ExecutorTools;
import com.sedmelluq.lava.discord.dispatch.packet.AudioPacketProvider;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public class NativeAudioSendSystemFactory implements AudioSendSystemFactory {
  private static final int DEFAULT_BUFFER_DURATION = 400;
  private static final int PACKET_INTERVAL = 20;

  private final int bufferDuration;
  private final AtomicLong identifierCounter = new AtomicLong();
  private final KeySetView<NativeAudioSendSystem, Boolean> systems = ConcurrentHashMap.newKeySet();
  private final Object lock = new Object();
  private volatile UdpQueueManager queueManager;
  private ScheduledExecutorService scheduler;

  public NativeAudioSendSystemFactory() {
    this(DEFAULT_BUFFER_DURATION);
  }

  public NativeAudioSendSystemFactory(int bufferDuration) {
    this.bufferDuration = bufferDuration;
  }

  private void initialiseQueueManager() {
    scheduler = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory("native-udp"));
    queueManager = new UdpQueueManager(bufferDuration / PACKET_INTERVAL,
        TimeUnit.MILLISECONDS.toNanos(PACKET_INTERVAL));

    scheduler.scheduleWithFixedDelay(this::populateQueues, 0, 40, TimeUnit.MILLISECONDS);

    Thread thread = new Thread(process(queueManager));
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

  void addInstance(NativeAudioSendSystem system) {
    synchronized (lock) {
      systems.add(system);

      if (queueManager == null) {
        initialiseQueueManager();
      }
    }
  }

  void removeInstance(NativeAudioSendSystem system) {
    ScheduledExecutorService schedulerToShutDown = null;

    synchronized (lock) {
      if (systems.remove(system) && systems.isEmpty() && queueManager != null) {
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

  private static Runnable process(UdpQueueManager unbake) {
    return unbake::process;
  }
}
