package com.sedmelluq.lava.discord.dispatch.packet;

import com.sedmelluq.lava.discord.dispatch.AudioSendSystemFactory;
import com.sedmelluq.lava.discord.dispatch.OpusFrameProvider;
import com.sedmelluq.lava.discord.dispatch.packet.AudioPacketBuilder.NonceStrategy;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AudioPacketProviderHolder {
  private final Consumer<Boolean> speakingStateHandler;
  private final Supplier<OpusFrameProvider> frameProviderSupplier;
  private final AudioSendSystemFactory sendSystemFactory;
  private final AtomicReference<AudioPacketProvider> packetProvider;
  private final boolean requireExplicitSocketHandle;
  private volatile ConnectionDetailsBuilder connectionDetailsBuilder;

  public AudioPacketProviderHolder(Consumer<Boolean> speakingStateHandler,
                                   Supplier<OpusFrameProvider> frameProviderSupplier,
                                   AudioSendSystemFactory sendSystemFactory,
                                   boolean requireExplicitSocketHandle) {

    this.speakingStateHandler = speakingStateHandler;
    this.frameProviderSupplier = frameProviderSupplier;
    this.sendSystemFactory = sendSystemFactory;
    this.connectionDetailsBuilder = new ConnectionDetailsBuilder();
    this.packetProvider = new AtomicReference<>();
    this.requireExplicitSocketHandle = requireExplicitSocketHandle;
  }

  public void onFrameProviderChanged() {
    AudioPacketProvider provider = packetProvider.get();
    if (provider != null) {
      provider.setFrameProvider(frameProviderSupplier.get());
    }
  }

  public void onKeyAndStrategyChanged(byte[] secretKey, NonceStrategy strategy) {
    connectionDetailsUpdated(connectionDetailsBuilder
        .withSecretKey(secretKey)
        .withNonceStrategy(strategy));
  }

  public void onAddressAndSsrcChanged(InetSocketAddress targetAddress, int sourceIdentifier) {
    connectionDetailsUpdated(connectionDetailsBuilder
        .withAddress(targetAddress)
        .withSourceIdentifier(sourceIdentifier));
  }

  public void onExplicitSocketHandle(long explicitSocketHandle) {
    connectionDetailsUpdated(connectionDetailsBuilder
        .withExplicitSocketHandle(explicitSocketHandle));
  }

  public void shutdown() {
    AudioPacketProvider provider = packetProvider.getAndSet(null);

    if (provider != null) {
      provider.shutdown();
    }
  }

  private void connectionDetailsUpdated(ConnectionDetailsBuilder newDetails) {
    connectionDetailsBuilder = newDetails;

    if (newDetails.isComplete(requireExplicitSocketHandle)) {
      AudioPacketProvider newProvider = new AudioPacketProvider(
          sendSystemFactory,
          newDetails.address,
          newDetails.secretKey,
          newDetails.sourceIdentifier,
          newDetails.nonceStrategy,
          speakingStateHandler,
          newDetails.explicitSocketHandle
      );

      AudioPacketProvider oldProvider = packetProvider.getAndSet(newProvider);
      newProvider.setFrameProvider(frameProviderSupplier.get());
      newProvider.initialize();

      if (oldProvider != null) {
        oldProvider.shutdown();
      }
    }
  }

  private static class ConnectionDetailsBuilder {
    private final InetSocketAddress address;
    private final byte[] secretKey;
    private final Integer sourceIdentifier;
    private final NonceStrategy nonceStrategy;
    private final long explicitSocketHandle;

    private ConnectionDetailsBuilder() {
      this(null, null, null, null, -1);
    }

    private ConnectionDetailsBuilder(InetSocketAddress address, byte[] secretKey, Integer sourceIdentifier,
                                     NonceStrategy nonceStrategy, long explicitSocketHandle) {

      this.address = address;
      this.secretKey = secretKey;
      this.sourceIdentifier = sourceIdentifier;
      this.nonceStrategy = nonceStrategy;
      this.explicitSocketHandle = explicitSocketHandle;
    }

    private ConnectionDetailsBuilder withAddress(InetSocketAddress address) {
      return new ConnectionDetailsBuilder(address, secretKey, sourceIdentifier, nonceStrategy, explicitSocketHandle);
    }

    private ConnectionDetailsBuilder withSecretKey(byte[] secretKey) {
      return new ConnectionDetailsBuilder(address, secretKey, sourceIdentifier, nonceStrategy, explicitSocketHandle);
    }

    private ConnectionDetailsBuilder withSourceIdentifier(int sourceIdentifier) {
      return new ConnectionDetailsBuilder(address, secretKey, sourceIdentifier, nonceStrategy, explicitSocketHandle);
    }

    private ConnectionDetailsBuilder withNonceStrategy(NonceStrategy nonceStrategy) {
      return new ConnectionDetailsBuilder(address, secretKey, sourceIdentifier, nonceStrategy, explicitSocketHandle);
    }

    private ConnectionDetailsBuilder withExplicitSocketHandle(long explicitSocketHandle) {
      return new ConnectionDetailsBuilder(address, secretKey, sourceIdentifier, nonceStrategy, explicitSocketHandle);
    }

    private boolean isComplete(boolean requireExplicitSocketHandle) {
      return address != null && secretKey != null && sourceIdentifier != null && nonceStrategy != null &&
          (!requireExplicitSocketHandle || explicitSocketHandle != -1);
    }
  }
}
