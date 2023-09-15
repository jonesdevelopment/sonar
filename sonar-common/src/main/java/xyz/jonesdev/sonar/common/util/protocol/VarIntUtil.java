/*
 * Copyright (C) 2023 Sonar Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.jonesdev.sonar.common.util.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VarIntUtil {
  private static final int[] EXACT_BYTE_LENGTHS = new int[33];

  static {
    for (int i = 0; i <= 32; ++i) {
      EXACT_BYTE_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
    }
    EXACT_BYTE_LENGTHS[32] = 1;
  }

  public static int varIntBytes(final int value) {
    return EXACT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(value)];
  }

  public static void write21BitVarInt(final ByteBuf byteBuf, final int value) {
    // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
    int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
    byteBuf.writeMedium(w);
  }

  public static int readVarInt(final ByteBuf byteBuf) {
    int read = readVarIntSafely(byteBuf);
    if (read == Integer.MIN_VALUE) {
      throw new CorruptedFrameException("Corrupt VarInt");
    }
    return read;
  }

  public static int readVarIntSafely(final ByteBuf byteBuf) {
    int i = 0;
    int maxRead = Math.min(5, byteBuf.readableBytes());
    for (int j = 0; j < maxRead; j++) {
      int k = byteBuf.readByte();
      i |= (k & 0x7F) << j * 7;
      if ((k & 0x80) != 128) {
        return i;
      }
    }
    return Integer.MIN_VALUE;
  }

  public static void writeVarInt(final ByteBuf byteBuf, final int value) {
    // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
    // that the proxy will write, to improve inlining.
    if ((value & (0xFFFFFFFF << 7)) == 0) {
      byteBuf.writeByte(value);
    } else if ((value & (0xFFFFFFFF << 14)) == 0) {
      int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
      byteBuf.writeShort(w);
    } else {
      writeVarIntFull(byteBuf, value);
    }
  }

  private void writeVarIntFull(final ByteBuf byteBuf, final int value) {
    // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
    if ((value & (0xFFFFFFFF << 7)) == 0) {
      byteBuf.writeByte(value);
    } else if ((value & (0xFFFFFFFF << 14)) == 0) {
      int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
      byteBuf.writeShort(w);
    } else if ((value & (0xFFFFFFFF << 21)) == 0) {
      int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
      byteBuf.writeMedium(w);
    } else if ((value & (0xFFFFFFFF << 28)) == 0) {
      int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
        | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
      byteBuf.writeInt(w);
    } else {
      int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
        | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
      byteBuf.writeInt(w);
      byteBuf.writeByte(value >>> 28);
    }
  }

  public static void writeVarLong(final ByteBuf byteBuf, final long value) {
    // Peel the one and two byte count cases explicitly as they are the most common VarLong sizes
    // that the proxy will write, to improve inlining.
    if ((value & 0xFFFFFFFFFFFFFF80L) == 0L) {
      byteBuf.writeByte((byte) value);
    } else if ((value & 0xFFFFFFFFFFFFC000L) == 0L) {
      int w = (int) ((value & 0x7FL | 0x80L) << 8 | value >>> 7);
      byteBuf.writeShort(w);
    } else {
      writeVarLongFull(byteBuf, value);
    }
  }

  private void writeVarLongFull(final ByteBuf byteBuf, final long value) {
    if ((value & 0xFFFFFFFFFFFFFF80L) == 0L) {
      byteBuf.writeByte((byte) value);
    } else if ((value & 0xFFFFFFFFFFFFC000L) == 0L) {
      int w = (int) ((value & 0x7FL | 0x80L) << 8 | value >>> 7);
      byteBuf.writeShort(w);
    } else if ((value & 0xFFFFFFFFFFE00000L) == 0L) {
      int w = (int) ((value & 0x7FL | 0x80L) << 16 | (value >>> 7 & 0x7FL | 0x80L) << 8 | value >>> 14);
      byteBuf.writeMedium(w);
    } else if ((value & 0xFFFFFFFFF0000000L) == 0L) {
      int w =
        (int) ((value & 0x7FL | 0x80L) << 24 | (value >>> 7 & 0x7FL | 0x80L) << 16 | (value >>> 14 & 0x7FL | 0x80L) << 8 | value >>> 21);
      byteBuf.writeInt(w);
    } else {
      long l =
        (value & 0x7FL | 0x80L) << 24 | (value >>> 7 & 0x7FL | 0x80L) << 16 | (value >>> 14 & 0x7FL | 0x80L) << 8 | (value >>> 21 & 0x7FL | 0x80L);
      if ((value & 0xFFFFFFF800000000L) == 0L) {
        int w =
          (int) l;
        byteBuf.writeInt(w);
        byteBuf.writeByte((int) (value >>> 28));
      } else if ((value & 0xFFFFFC0000000000L) == 0L) {
        int w =
          (int) l;
        int w2 = (int) ((value >>> 28 & 0x7FL | 0x80L) << 8 | value >>> 35);
        byteBuf.writeInt(w);
        byteBuf.writeShort(w2);
      } else if ((value & 0xFFFE000000000000L) == 0L) {
        int w =
          (int) l;
        int w2 = (int) ((value >>> 28 & 0x7FL | 0x80L) << 16 | (value >>> 35 & 0x7FL | 0x80L) << 8 | value >>> 42);
        byteBuf.writeInt(w);
        byteBuf.writeMedium(w2);
      } else {
        long w =
          (value & 0x7FL | 0x80L) << 56 | (value >>> 7 & 0x7FL | 0x80L) << 48 | (value >>> 14 & 0x7FL | 0x80L) << 40 | (value >>> 21 & 0x7FL | 0x80L) << 32 | (value >>> 28 & 0x7FL | 0x80L) << 24 | (value >>> 35 & 0x7FL | 0x80L) << 16 | (value >>> 42 & 0x7FL | 0x80L) << 8 | value >>> 49;
        if ((value & 0xFF00000000000000L) == 0L) {
          byteBuf.writeLong(w);
        } else if ((value & Long.MIN_VALUE) == 0L) {
          byteBuf.writeLong(w);
          byteBuf.writeByte((byte) (value >>> 56));
        } else {
          int w2 = (int) ((value >>> 56 & 0x7FL | 0x80L) << 8 | value >>> 63);
          byteBuf.writeLong(w);
          byteBuf.writeShort(w2);
        }
      }
    }
  }
}
