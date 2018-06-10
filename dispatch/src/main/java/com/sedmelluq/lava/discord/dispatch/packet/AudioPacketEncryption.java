package com.sedmelluq.lava.discord.dispatch.packet;

import javax.annotation.concurrent.NotThreadSafe;

public class AudioPacketEncryption {
  @NotThreadSafe
  public static final class Salsa20 {
    private static final byte[] SIGMA = {101, 120, 112, 97, 110, 100, 32, 51, 50, 45, 98, 121, 116, 101, 32, 107};

    private final byte[] nonceKey = new byte[32];
    private final byte[] fixed = new byte[16];
    private final byte[] xor = new byte[64];
    private final byte[] secretKey;

    public Salsa20(byte[] secretKey) {
      this.secretKey = secretKey;
    }

    public void process(byte[] nonceBytes, byte[] cipherBytes, byte[] messageBytes, int messageLength) {
      salsa20(nonceKey, nonceBytes, secretKey, SIGMA, true);

      for (int i = 0; i < 8; i++) {
        fixed[i] = nonceBytes[i + 16];
        fixed[i + 8] = 0;
      }

      int cipherPosition = 0;
      int messagePosition = 0;
      int messageRemaining = messageLength;

      while (messageRemaining >= 64) {
        salsa20(xor, fixed, nonceKey, SIGMA, false);

        for (int i = 0; i < 64; i++) {
          cipherBytes[cipherPosition + i] = (byte) ((messageBytes[messagePosition + i] ^ xor[i]) & 0xff);
        }

        int u = 1;

        for (int i = 8; i < 16; i++) {
          u = u + (fixed[i] & 0xff);
          fixed[i] = (byte) (u & 0xff);
          u >>>= 8;
        }

        messageRemaining -= 64;
        cipherPosition += 64;
        messagePosition += 64;
      }

      if (messageRemaining > 0) {
        salsa20(xor, fixed, nonceKey, SIGMA, false);

        for (int i = 0; i < messageRemaining; i++) {
          cipherBytes[cipherPosition + i] = (byte) ((messageBytes[messagePosition + i] ^ xor[i]) & 0xff);
        }
      }
    }

    private static void salsa20(byte [] o, byte [] p, byte [] k, byte [] c, boolean half) {
      int j0  = c[ 0] & 0xff | (c[ 1] & 0xff)<<8 | (c[ 2] & 0xff)<<16 | (c[ 3] & 0xff)<<24,
          j1  = k[ 0] & 0xff | (k[ 1] & 0xff)<<8 | (k[ 2] & 0xff)<<16 | (k[ 3] & 0xff)<<24,
          j2  = k[ 4] & 0xff | (k[ 5] & 0xff)<<8 | (k[ 6] & 0xff)<<16 | (k[ 7] & 0xff)<<24,
          j3  = k[ 8] & 0xff | (k[ 9] & 0xff)<<8 | (k[10] & 0xff)<<16 | (k[11] & 0xff)<<24,
          j4  = k[12] & 0xff | (k[13] & 0xff)<<8 | (k[14] & 0xff)<<16 | (k[15] & 0xff)<<24,
          j5  = c[ 4] & 0xff | (c[ 5] & 0xff)<<8 | (c[ 6] & 0xff)<<16 | (c[ 7] & 0xff)<<24,
          j6  = p[ 0] & 0xff | (p[ 1] & 0xff)<<8 | (p[ 2] & 0xff)<<16 | (p[ 3] & 0xff)<<24,
          j7  = p[ 4] & 0xff | (p[ 5] & 0xff)<<8 | (p[ 6] & 0xff)<<16 | (p[ 7] & 0xff)<<24,
          j8  = p[ 8] & 0xff | (p[ 9] & 0xff)<<8 | (p[10] & 0xff)<<16 | (p[11] & 0xff)<<24,
          j9  = p[12] & 0xff | (p[13] & 0xff)<<8 | (p[14] & 0xff)<<16 | (p[15] & 0xff)<<24,
          j10 = c[ 8] & 0xff | (c[ 9] & 0xff)<<8 | (c[10] & 0xff)<<16 | (c[11] & 0xff)<<24,
          j11 = k[16] & 0xff | (k[17] & 0xff)<<8 | (k[18] & 0xff)<<16 | (k[19] & 0xff)<<24,
          j12 = k[20] & 0xff | (k[21] & 0xff)<<8 | (k[22] & 0xff)<<16 | (k[23] & 0xff)<<24,
          j13 = k[24] & 0xff | (k[25] & 0xff)<<8 | (k[26] & 0xff)<<16 | (k[27] & 0xff)<<24,
          j14 = k[28] & 0xff | (k[29] & 0xff)<<8 | (k[30] & 0xff)<<16 | (k[31] & 0xff)<<24,
          j15 = c[12] & 0xff | (c[13] & 0xff)<<8 | (c[14] & 0xff)<<16 | (c[15] & 0xff)<<24;

      int x0 = j0, x1 = j1, x2 = j2, x3 = j3, x4 = j4, x5 = j5, x6 = j6, x7 = j7,
          x8 = j8, x9 = j9, x10 = j10, x11 = j11, x12 = j12, x13 = j13, x14 = j14,
          x15 = j15, u;

      for (int i = 0; i < 20; i += 2) {
        u = x0 + x12;
        x4 ^= u<<7 | u>>>(32-7);
        u = x4 + x0;
        x8 ^= u<<9 | u>>>(32-9);
        u = x8 + x4;
        x12 ^= u<<13 | u>>>(32-13);
        u = x12 + x8;
        x0 ^= u<<18 | u>>>(32-18);

        u = x5 + x1;
        x9 ^= u<<7 | u>>>(32-7);
        u = x9 + x5;
        x13 ^= u<<9 | u>>>(32-9);
        u = x13 + x9;
        x1 ^= u<<13 | u>>>(32-13);
        u = x1 + x13;
        x5 ^= u<<18 | u>>>(32-18);

        u = x10 + x6;
        x14 ^= u<<7 | u>>>(32-7);
        u = x14 + x10;
        x2 ^= u<<9 | u>>>(32-9);
        u = x2 + x14;
        x6 ^= u<<13 | u>>>(32-13);
        u = x6 + x2;
        x10 ^= u<<18 | u>>>(32-18);

        u = x15 + x11;
        x3 ^= u<<7 | u>>>(32-7);
        u = x3 + x15;
        x7 ^= u<<9 | u>>>(32-9);
        u = x7 + x3;
        x11 ^= u<<13 | u>>>(32-13);
        u = x11 + x7;
        x15 ^= u<<18 | u>>>(32-18);

        u = x0 + x3;
        x1 ^= u<<7 | u>>>(32-7);
        u = x1 + x0;
        x2 ^= u<<9 | u>>>(32-9);
        u = x2 + x1;
        x3 ^= u<<13 | u>>>(32-13);
        u = x3 + x2;
        x0 ^= u<<18 | u>>>(32-18);

        u = x5 + x4;
        x6 ^= u<<7 | u>>>(32-7);
        u = x6 + x5;
        x7 ^= u<<9 | u>>>(32-9);
        u = x7 + x6;
        x4 ^= u<<13 | u>>>(32-13);
        u = x4 + x7;
        x5 ^= u<<18 | u>>>(32-18);

        u = x10 + x9;
        x11 ^= u<<7 | u>>>(32-7);
        u = x11 + x10;
        x8 ^= u<<9 | u>>>(32-9);
        u = x8 + x11;
        x9 ^= u<<13 | u>>>(32-13);
        u = x9 + x8;
        x10 ^= u<<18 | u>>>(32-18);

        u = x15 + x14;
        x12 ^= u<<7 | u>>>(32-7);
        u = x12 + x15;
        x13 ^= u<<9 | u>>>(32-9);
        u = x13 + x12;
        x14 ^= u<<13 | u>>>(32-13);
        u = x14 + x13;
        x15 ^= u<<18 | u>>>(32-18);
      }

      if (half) {
        o[0] = (byte) (x0 & 0xff);
        o[1] = (byte) (x0 >>> 8 & 0xff);
        o[2] = (byte) (x0 >>> 16 & 0xff);
        o[3] = (byte) (x0 >>> 24 & 0xff);

        o[4] = (byte) (x5 & 0xff);
        o[5] = (byte) (x5 >>> 8 & 0xff);
        o[6] = (byte) (x5 >>> 16 & 0xff);
        o[7] = (byte) (x5 >>> 24 & 0xff);

        o[8] = (byte) (x10 & 0xff);
        o[9] = (byte) (x10 >>> 8 & 0xff);
        o[10] = (byte) (x10 >>> 16 & 0xff);
        o[11] = (byte) (x10 >>> 24 & 0xff);

        o[12] = (byte) (x15 & 0xff);
        o[13] = (byte) (x15 >>> 8 & 0xff);
        o[14] = (byte) (x15 >>> 16 & 0xff);
        o[15] = (byte) (x15 >>> 24 & 0xff);

        o[16] = (byte) (x6 & 0xff);
        o[17] = (byte) (x6 >>> 8 & 0xff);
        o[18] = (byte) (x6 >>> 16 & 0xff);
        o[19] = (byte) (x6 >>> 24 & 0xff);

        o[20] = (byte) (x7 & 0xff);
        o[21] = (byte) (x7 >>> 8 & 0xff);
        o[22] = (byte) (x7 >>> 16 & 0xff);
        o[23] = (byte) (x7 >>> 24 & 0xff);

        o[24] = (byte) (x8 & 0xff);
        o[25] = (byte) (x8 >>> 8 & 0xff);
        o[26] = (byte) (x8 >>> 16 & 0xff);
        o[27] = (byte) (x8 >>> 24 & 0xff);

        o[28] = (byte) (x9 & 0xff);
        o[29] = (byte) (x9 >>> 8 & 0xff);
        o[30] = (byte) (x9 >>> 16 & 0xff);
        o[31] = (byte) (x9 >>> 24 & 0xff);
      } else {
        x0 += j0;
        x1 += j1;
        x2 += j2;
        x3 += j3;
        x4 += j4;
        x5 += j5;
        x6 += j6;
        x7 += j7;
        x8 += j8;
        x9 += j9;
        x10 += j10;
        x11 += j11;
        x12 += j12;
        x13 += j13;
        x14 += j14;
        x15 += j15;

        o[0] = (byte) (x0 & 0xff);
        o[1] = (byte) (x0 >>> 8 & 0xff);
        o[2] = (byte) (x0 >>> 16 & 0xff);
        o[3] = (byte) (x0 >>> 24 & 0xff);

        o[4] = (byte) (x1 & 0xff);
        o[5] = (byte) (x1 >>> 8 & 0xff);
        o[6] = (byte) (x1 >>> 16 & 0xff);
        o[7] = (byte) (x1 >>> 24 & 0xff);

        o[8] = (byte) (x2 & 0xff);
        o[9] = (byte) (x2 >>> 8 & 0xff);
        o[10] = (byte) (x2 >>> 16 & 0xff);
        o[11] = (byte) (x2 >>> 24 & 0xff);

        o[12] = (byte) (x3 & 0xff);
        o[13] = (byte) (x3 >>> 8 & 0xff);
        o[14] = (byte) (x3 >>> 16 & 0xff);
        o[15] = (byte) (x3 >>> 24 & 0xff);

        o[16] = (byte) (x4 & 0xff);
        o[17] = (byte) (x4 >>> 8 & 0xff);
        o[18] = (byte) (x4 >>> 16 & 0xff);
        o[19] = (byte) (x4 >>> 24 & 0xff);

        o[20] = (byte) (x5 & 0xff);
        o[21] = (byte) (x5 >>> 8 & 0xff);
        o[22] = (byte) (x5 >>> 16 & 0xff);
        o[23] = (byte) (x5 >>> 24 & 0xff);

        o[24] = (byte) (x6 & 0xff);
        o[25] = (byte) (x6 >>> 8 & 0xff);
        o[26] = (byte) (x6 >>> 16 & 0xff);
        o[27] = (byte) (x6 >>> 24 & 0xff);

        o[28] = (byte) (x7 & 0xff);
        o[29] = (byte) (x7 >>> 8 & 0xff);
        o[30] = (byte) (x7 >>> 16 & 0xff);
        o[31] = (byte) (x7 >>> 24 & 0xff);

        o[32] = (byte) (x8 & 0xff);
        o[33] = (byte) (x8 >>> 8 & 0xff);
        o[34] = (byte) (x8 >>> 16 & 0xff);
        o[35] = (byte) (x8 >>> 24 & 0xff);

        o[36] = (byte) (x9 & 0xff);
        o[37] = (byte) (x9 >>> 8 & 0xff);
        o[38] = (byte) (x9 >>> 16 & 0xff);
        o[39] = (byte) (x9 >>> 24 & 0xff);

        o[40] = (byte) (x10 & 0xff);
        o[41] = (byte) (x10 >>> 8 & 0xff);
        o[42] = (byte) (x10 >>> 16 & 0xff);
        o[43] = (byte) (x10 >>> 24 & 0xff);

        o[44] = (byte) (x11 & 0xff);
        o[45] = (byte) (x11 >>> 8 & 0xff);
        o[46] = (byte) (x11 >>> 16 & 0xff);
        o[47] = (byte) (x11 >>> 24 & 0xff);

        o[48] = (byte) (x12 & 0xff);
        o[49] = (byte) (x12 >>> 8 & 0xff);
        o[50] = (byte) (x12 >>> 16 & 0xff);
        o[51] = (byte) (x12 >>> 24 & 0xff);

        o[52] = (byte) (x13 & 0xff);
        o[53] = (byte) (x13 >>> 8 & 0xff);
        o[54] = (byte) (x13 >>> 16 & 0xff);
        o[55] = (byte) (x13 >>> 24 & 0xff);

        o[56] = (byte) (x14 & 0xff);
        o[57] = (byte) (x14 >>> 8 & 0xff);
        o[58] = (byte) (x14 >>> 16 & 0xff);
        o[59] = (byte) (x14 >>> 24 & 0xff);

        o[60] = (byte) (x15 & 0xff);
        o[61] = (byte) (x15 >>> 8 & 0xff);
        o[62] = (byte) (x15 >>> 16 & 0xff);
        o[63] = (byte) (x15 >>> 24 & 0xff);
      }
    }
  }

  @NotThreadSafe
  public static final class Poly1305 {
    private final int[] pad;
    private final int[] h;
    private final int[] g;

    public Poly1305() {
      this.pad = new int[8];
      this.h = new int[10];
      this.g = new int[10];
    }

    public void process(byte[] key, byte[] message, int messageOffset, int length, byte[] output, int outputOffset) {
      preparePad(key);
      blocks(key, message, messageOffset, length);
      complete();

      for (int i = 0; i < 8; i++) {
        output[outputOffset++] = (byte) (h[i] & 0xff);
        output[outputOffset++] = (byte) ((h[i] >>> 8) & 0xff);
      }
    }

    private void preparePad(byte[] key) {
      pad[0] = key[16] & 0xff | (key[17] & 0xff) << 8;
      pad[1] = key[18] & 0xff | (key[19] & 0xff) << 8;
      pad[2] = key[20] & 0xff | (key[21] & 0xff) << 8;
      pad[3] = key[22] & 0xff | (key[23] & 0xff) << 8;
      pad[4] = key[24] & 0xff | (key[25] & 0xff) << 8;
      pad[5] = key[26] & 0xff | (key[27] & 0xff) << 8;
      pad[6] = key[28] & 0xff | (key[29] & 0xff) << 8;
      pad[7] = key[30] & 0xff | (key[31] & 0xff) << 8;
    }

    private void blocks(byte[] key, byte[] message, int offset, int length) {
      int highBit = 1 << 11L;

      int h0 = 0, h1 = 0, h2 = 0, h3 = 0, h4 = 0, h5 = 0, h6 = 0, h7 = 0, h8 = 0, h9 = 0;

      int t0 = key[ 0] & 0xff | (key[ 1] & 0xff) << 8;
      int t1 = key[ 2] & 0xff | (key[ 3] & 0xff) << 8;
      int t2 = key[ 4] & 0xff | (key[ 5] & 0xff) << 8;
      int t3 = key[ 6] & 0xff | (key[ 7] & 0xff) << 8;
      int t4 = key[ 8] & 0xff | (key[ 9] & 0xff) << 8;
      int t5 = key[10] & 0xff | (key[11] & 0xff) << 8;
      int t6 = key[12] & 0xff | (key[13] & 0xff) << 8;
      int t7 = key[14] & 0xff | (key[15] & 0xff) << 8;

      int r0 = ( t0                     ) & 0x1fff;
      int r1 = ((t0 >>> 13) | (t1 <<  3)) & 0x1fff;
      int r2 = ((t1 >>> 10) | (t2 <<  6)) & 0x1f03;
      int r3 = ((t2 >>>  7) | (t3 <<  9)) & 0x1fff;
      int r4 = ((t3 >>>  4) | (t4 << 12)) & 0x00ff;
      int r5 = ((t4 >>>  1)) & 0x1ffe;
      int r6 = ((t4 >>> 14) | (t5 <<  2)) & 0x1fff;
      int r7 = ((t5 >>> 11) | (t6 <<  5)) & 0x1f81;
      int r8 = ((t6 >>>  8) | (t7 <<  8)) & 0x1fff;
      int r9 = ((t7 >>>  5)) & 0x007f;

      while (length > 0) {
        if (length < 16) {
          int end = offset + length;
          int alignedEnd = offset + 16;

          message[end++] = 1;

          for (; end < alignedEnd; end++) {
            message[end] = 0;
          }

          highBit = 0;
        }

        t0 = message[offset   ] & 0xff | (message[offset+ 1] & 0xff) << 8;
        t1 = message[offset+ 2] & 0xff | (message[offset+ 3] & 0xff) << 8;
        t2 = message[offset+ 4] & 0xff | (message[offset+ 5] & 0xff) << 8;
        t3 = message[offset+ 6] & 0xff | (message[offset+ 7] & 0xff) << 8;
        t4 = message[offset+ 8] & 0xff | (message[offset+ 9] & 0xff) << 8;
        t5 = message[offset+10] & 0xff | (message[offset+11] & 0xff) << 8;
        t6 = message[offset+12] & 0xff | (message[offset+13] & 0xff) << 8;
        t7 = message[offset+14] & 0xff | (message[offset+15] & 0xff) << 8;

        h0 += ( t0                     ) & 0x1fff;
        h1 += ((t0 >>> 13) | (t1 <<  3)) & 0x1fff;
        h2 += ((t1 >>> 10) | (t2 <<  6)) & 0x1fff;
        h3 += ((t2 >>>  7) | (t3 <<  9)) & 0x1fff;
        h4 += ((t3 >>>  4) | (t4 << 12)) & 0x1fff;
        h5 += ((t4 >>>  1)) & 0x1fff;
        h6 += ((t4 >>> 14) | (t5 <<  2)) & 0x1fff;
        h7 += ((t5 >>> 11) | (t6 <<  5)) & 0x1fff;
        h8 += ((t6 >>>  8) | (t7 <<  8)) & 0x1fff;
        h9 += ((t7 >>> 5)) | highBit;

        int c = 0;

        int d0 = c;
        d0 += h0 * r0;
        d0 += h1 * (5 * r9);
        d0 += h2 * (5 * r8);
        d0 += h3 * (5 * r7);
        d0 += h4 * (5 * r6);
        c = (d0 >>> 13); d0 &= 0x1fff;
        d0 += h5 * (5 * r5);
        d0 += h6 * (5 * r4);
        d0 += h7 * (5 * r3);
        d0 += h8 * (5 * r2);
        d0 += h9 * (5 * r1);
        c += (d0 >>> 13); d0 &= 0x1fff;

        int d1 = c;
        d1 += h0 * r1;
        d1 += h1 * r0;
        d1 += h2 * (5 * r9);
        d1 += h3 * (5 * r8);
        d1 += h4 * (5 * r7);
        c = (d1 >>> 13); d1 &= 0x1fff;
        d1 += h5 * (5 * r6);
        d1 += h6 * (5 * r5);
        d1 += h7 * (5 * r4);
        d1 += h8 * (5 * r3);
        d1 += h9 * (5 * r2);
        c += (d1 >>> 13); d1 &= 0x1fff;

        int d2 = c;
        d2 += h0 * r2;
        d2 += h1 * r1;
        d2 += h2 * r0;
        d2 += h3 * (5 * r9);
        d2 += h4 * (5 * r8);
        c = (d2 >>> 13); d2 &= 0x1fff;
        d2 += h5 * (5 * r7);
        d2 += h6 * (5 * r6);
        d2 += h7 * (5 * r5);
        d2 += h8 * (5 * r4);
        d2 += h9 * (5 * r3);
        c += (d2 >>> 13); d2 &= 0x1fff;

        int d3 = c;
        d3 += h0 * r3;
        d3 += h1 * r2;
        d3 += h2 * r1;
        d3 += h3 * r0;
        d3 += h4 * (5 * r9);
        c = (d3 >>> 13); d3 &= 0x1fff;
        d3 += h5 * (5 * r8);
        d3 += h6 * (5 * r7);
        d3 += h7 * (5 * r6);
        d3 += h8 * (5 * r5);
        d3 += h9 * (5 * r4);
        c += (d3 >>> 13); d3 &= 0x1fff;

        int d4 = c;
        d4 += h0 * r4;
        d4 += h1 * r3;
        d4 += h2 * r2;
        d4 += h3 * r1;
        d4 += h4 * r0;
        c = (d4 >>> 13); d4 &= 0x1fff;
        d4 += h5 * (5 * r9);
        d4 += h6 * (5 * r8);
        d4 += h7 * (5 * r7);
        d4 += h8 * (5 * r6);
        d4 += h9 * (5 * r5);
        c += (d4 >>> 13); d4 &= 0x1fff;

        int d5 = c;
        d5 += h0 * r5;
        d5 += h1 * r4;
        d5 += h2 * r3;
        d5 += h3 * r2;
        d5 += h4 * r1;
        c = (d5 >>> 13); d5 &= 0x1fff;
        d5 += h5 * r0;
        d5 += h6 * (5 * r9);
        d5 += h7 * (5 * r8);
        d5 += h8 * (5 * r7);
        d5 += h9 * (5 * r6);
        c += (d5 >>> 13); d5 &= 0x1fff;

        int d6 = c;
        d6 += h0 * r6;
        d6 += h1 * r5;
        d6 += h2 * r4;
        d6 += h3 * r3;
        d6 += h4 * r2;
        c = (d6 >>> 13); d6 &= 0x1fff;
        d6 += h5 * r1;
        d6 += h6 * r0;
        d6 += h7 * (5 * r9);
        d6 += h8 * (5 * r8);
        d6 += h9 * (5 * r7);
        c += (d6 >>> 13); d6 &= 0x1fff;

        int d7 = c;
        d7 += h0 * r7;
        d7 += h1 * r6;
        d7 += h2 * r5;
        d7 += h3 * r4;
        d7 += h4 * r3;
        c = (d7 >>> 13); d7 &= 0x1fff;
        d7 += h5 * r2;
        d7 += h6 * r1;
        d7 += h7 * r0;
        d7 += h8 * (5 * r9);
        d7 += h9 * (5 * r8);
        c += (d7 >>> 13); d7 &= 0x1fff;

        int d8 = c;
        d8 += h0 * r8;
        d8 += h1 * r7;
        d8 += h2 * r6;
        d8 += h3 * r5;
        d8 += h4 * r4;
        c = (d8 >>> 13); d8 &= 0x1fff;
        d8 += h5 * r3;
        d8 += h6 * r2;
        d8 += h7 * r1;
        d8 += h8 * r0;
        d8 += h9 * (5 * r9);
        c += (d8 >>> 13); d8 &= 0x1fff;

        int d9 = c;
        d9 += h0 * r9;
        d9 += h1 * r8;
        d9 += h2 * r7;
        d9 += h3 * r6;
        d9 += h4 * r5;
        c = (d9 >>> 13); d9 &= 0x1fff;
        d9 += h5 * r4;
        d9 += h6 * r3;
        d9 += h7 * r2;
        d9 += h8 * r1;
        d9 += h9 * r0;
        c += (d9 >>> 13); d9 &= 0x1fff;

        c += c << 2;
        c += d0;
        d0 = c & 0x1fff;
        c = (c >>> 13);
        d1 += c;

        h0 = d0;
        h1 = d1;
        h2 = d2;
        h3 = d3;
        h4 = d4;
        h5 = d5;
        h6 = d6;
        h7 = d7;
        h8 = d8;
        h9 = d9;

        offset += 16;
        length -= 16;
      }

      h[0] = h0;
      h[1] = h1;
      h[2] = h2;
      h[3] = h3;
      h[4] = h4;
      h[5] = h5;
      h[6] = h6;
      h[7] = h7;
      h[8] = h8;
      h[9] = h9;
    }

    private void complete() {
      int carry = 0;

      for (int i = 1; i < 10; i++) {
        h[i] += carry;
        carry = h[i] >>> 13;
        h[i] &= 0x1fff;
      }

      h[0] += (carry * 5);
      carry = h[0] >>> 13;
      h[0] &= 0x1fff;

      h[1] += carry;
      carry = h[1] >>> 13;
      h[1] &= 0x1fff;

      h[2] += carry;

      g[0] = h[0] + 5;
      carry = g[0] >>> 13;
      g[0] &= 0x1fff;

      for (int i = 1; i < 10; i++) {
        g[i] = h[i] + carry;
        carry = g[i] >>> 13;
        g[i] &= 0x1fff;
      }

      g[9] -= (1 << 13);
      g[9] &= 0xffff;

      int mask = ((g[9] >>> ((2 * 8) - 1)) - 1) & 0xffff;

      for (int i = 0; i < 10; i++) {
        g[i] &= mask;
      }

      mask = ~mask;

      for (int i = 0; i < 10; i++) {
        h[i] = (h[i] & mask) | g[i];
      }

      h[0] = ((h[0]       ) | (h[1] << 13)               ) & 0xffff;
      h[1] = ((h[1] >>>  3) | (h[2] << 10)               ) & 0xffff;
      h[2] = ((h[2] >>>  6) | (h[3] <<  7)               ) & 0xffff;
      h[3] = ((h[3] >>>  9) | (h[4] <<  4)               ) & 0xffff;
      h[4] = ((h[4] >>> 12) | (h[5] <<  1) | (h[6] << 14)) & 0xffff;
      h[5] = ((h[6] >>>  2) | (h[7] << 11)               ) & 0xffff;
      h[6] = ((h[7] >>>  5) | (h[8] <<  8)               ) & 0xffff;
      h[7] = ((h[8] >>>  8) | (h[9] <<  5)               ) & 0xffff;

      int f = h[0] + pad[0];
      h[0] = f & 0xffff;

      for (int i = 1; i < 8; i++) {
        f = h[i] + pad[i] + (f >>> 16);
        h[i] = f & 0xffff;
      }
    }
  }
}
