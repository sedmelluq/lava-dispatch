package com.sedmelluq.lava.discord.dispatch.packet;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static com.sedmelluq.lava.discord.dispatch.packet.AudioPacketBuilder.NonceStrategy.INCREMENTING_INT;
import static com.sedmelluq.lava.discord.dispatch.packet.AudioPacketBuilder.NonceStrategy.PACKET_HEADER;
import static com.sedmelluq.lava.discord.dispatch.packet.AudioPacketBuilder.NonceStrategy.RANDOM_SEQUENCE;

@NotThreadSafe
public class AudioPacketBuilder {
  private static final int MAXIMUM_PACKET_SIZE = 1024;

  private static final int RTP_HEADER_BYTE_LENGTH = 12;
  private static final int XSALSA20_NONCE_LENGTH = 24;
  private static final byte RTP_VERSION_PAD_EXTEND = (byte) 0x80;
  private static final byte RTP_PAYLOAD_TYPE = (byte) 0x78;

  private static final int RTP_VERSION_PAD_EXTEND_INDEX = 0;
  private static final int RTP_PAYLOAD_INDEX = 1;
  private static final int SEQ_INDEX = 2;
  private static final int TIMESTAMP_INDEX = 4;
  private static final int SSRC_INDEX = 8;

  private final NonceStrategy nonceStrategy;
  private final byte[] packetHeader;
  private final byte[] nonceBytes;
  private final byte[] messageBytes;
  private final byte[] cipherBytes;
  private final ByteBuffer messageBuffer;
  private final AudioPacketEncryption.Poly1305 poly1305;
  private final AudioPacketEncryption.Salsa20 salsa20;
  private int nonceCounter;

  public AudioPacketBuilder(byte[] secretKey, int sourceIdentifier, NonceStrategy nonceStrategy) {
    this.nonceStrategy = nonceStrategy;
    this.packetHeader = new byte[XSALSA20_NONCE_LENGTH];
    this.nonceBytes = nonceStrategy == PACKET_HEADER ? packetHeader : new byte[XSALSA20_NONCE_LENGTH];
    this.messageBytes = new byte[MAXIMUM_PACKET_SIZE + 32];
    this.cipherBytes = new byte[MAXIMUM_PACKET_SIZE + 32];
    this.messageBuffer = ByteBuffer.wrap(messageBytes);
    this.poly1305 = new AudioPacketEncryption.Poly1305();
    this.salsa20 = new AudioPacketEncryption.Salsa20(secretKey);
    this.nonceCounter = 0;

    packetHeader[RTP_VERSION_PAD_EXTEND_INDEX] = RTP_VERSION_PAD_EXTEND;
    packetHeader[RTP_PAYLOAD_INDEX] = RTP_PAYLOAD_TYPE;
    packetHeader[SSRC_INDEX] = (byte) (sourceIdentifier >> 24);
    packetHeader[SSRC_INDEX + 1] = (byte) (sourceIdentifier >> 16);
    packetHeader[SSRC_INDEX + 2] = (byte) (sourceIdentifier >> 8);
    packetHeader[SSRC_INDEX + 3] = (byte) sourceIdentifier;
  }

  public ByteBuffer getPayloadBuffer() {
    messageBuffer.position(32);
    messageBuffer.limit(messageBuffer.capacity());
    return messageBuffer;
  }

  public void writeEncrypted(ByteBuffer output, char sequenceNumber, int timestamp) {
    int messageLength = messageBuffer.position();

    packetHeader[SEQ_INDEX] = (byte) (sequenceNumber >> 8);
    packetHeader[SEQ_INDEX + 1] = (byte) sequenceNumber;
    packetHeader[TIMESTAMP_INDEX] = (byte) (timestamp >> 24);
    packetHeader[TIMESTAMP_INDEX + 1] = (byte) (timestamp >> 16);
    packetHeader[TIMESTAMP_INDEX + 2] = (byte) (timestamp >> 8);
    packetHeader[TIMESTAMP_INDEX + 3] = (byte) timestamp;

    if (nonceStrategy == INCREMENTING_INT) {
      nonceCounter++;
      nonceBytes[0] = (byte) (nonceCounter >> 24);
      nonceBytes[1] = (byte) (nonceCounter >> 16);
      nonceBytes[2] = (byte) (nonceCounter >> 8);
      nonceBytes[3] = (byte) nonceCounter;
    } else if (nonceStrategy == RANDOM_SEQUENCE) {
      ThreadLocalRandom.current().nextBytes(nonceBytes);
    }

    salsa20.process(nonceBytes, cipherBytes, messageBytes, messageLength);
    poly1305.process(cipherBytes, cipherBytes, 32, messageLength - 32, cipherBytes, 16);

    output.put(packetHeader, 0, RTP_HEADER_BYTE_LENGTH);
    output.put(cipherBytes, 16, messageLength - 16);

    if (nonceStrategy == INCREMENTING_INT) {
      output.put(nonceBytes, 0, 4);
    } else if (nonceStrategy == RANDOM_SEQUENCE) {
      output.put(nonceBytes);
    }
  }

  public enum NonceStrategy {
    INCREMENTING_INT,
    RANDOM_SEQUENCE,
    PACKET_HEADER
  }
}
