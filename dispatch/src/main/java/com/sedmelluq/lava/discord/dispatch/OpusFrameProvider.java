package com.sedmelluq.lava.discord.dispatch;

import java.nio.ByteBuffer;

public interface OpusFrameProvider extends AutoCloseable {
  boolean provideOpusFrame(ByteBuffer buffer);

  @Override
  default void close() throws Exception {
    // Nothing to do.
  }
}
