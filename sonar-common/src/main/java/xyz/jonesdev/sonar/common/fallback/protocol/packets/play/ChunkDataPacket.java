/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class ChunkDataPacket implements FallbackPacket {
  private int x, z;

  private static final byte[] SECTION_BYTES = new byte[]{0, 0, 0, 0, 0, 0, 1, 0};
  private static final byte[] LIGHT_BYTES = new byte[]{1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 3, -1, -1, 0, 0};

  private static final byte[] LEGACY_FILLER_BYTES_17 = new byte[2];
  private static final byte[] LEGACY_FILLER_BYTES = new byte[256];
  private static final byte[] MODERN_FILLER_BYTES = new byte[256 * 4];

  // Prepare nbt for 1.18 and pre-1.18
  private static final CompoundBinaryTag MODERN_TAG, LEGACY_TAG;

  static {
    MODERN_TAG = prepareNBT(false);
    LEGACY_TAG = prepareNBT(true);
  }

  private static @NotNull CompoundBinaryTag prepareNBT(final boolean legacy) {
    final long[] arrayData = new long[legacy ? 36 : 37];
    final LongArrayBinaryTag longArray = LongArrayBinaryTag.longArrayBinaryTag(arrayData);

    final CompoundBinaryTag motion = CompoundBinaryTag.builder()
      .put("MOTION_BLOCKING", longArray)
      .build();
    return CompoundBinaryTag.builder()
      .put("root", motion)
      .build();
  }

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeInt(x);
    byteBuf.writeInt(z);

    if (protocolVersion.compareTo(MINECRAFT_1_17) >= 0) {
      if (protocolVersion.compareTo(MINECRAFT_1_17_1) <= 0) {
        writeVarInt(byteBuf, 0); // mask
      }
    } else {
      byteBuf.writeBoolean(true); // full chunk

      if (protocolVersion.compareTo(MINECRAFT_1_16) >= 0
        && protocolVersion.compareTo(MINECRAFT_1_16_2) < 0) {
        byteBuf.writeBoolean(true); // ignore old data
      }

      if (protocolVersion.compareTo(MINECRAFT_1_8) > 0) {
        writeVarInt(byteBuf, 0);
      } else {
        byteBuf.writeShort(1); // fix void chunk
      }
    }

    if (protocolVersion.compareTo(MINECRAFT_1_14) >= 0) {
      if (protocolVersion.compareTo(MINECRAFT_1_20_2) >= 0) {
        writeNamelessCompoundTag(byteBuf, MODERN_TAG);
      } else {
        writeCompoundTag(byteBuf, protocolVersion.compareTo(MINECRAFT_1_18) < 0 ? LEGACY_TAG : MODERN_TAG);
      }

      if (protocolVersion.compareTo(MINECRAFT_1_15) >= 0 && protocolVersion.compareTo(MINECRAFT_1_18) < 0) {
        if (protocolVersion.compareTo(MINECRAFT_1_16_2) >= 0) {
          writeVarInt(byteBuf, 1024);

          for (int i = 0; i < 1024; i++) {
            writeVarInt(byteBuf, 1);
          }
        } else {
          for (int i = 0; i < 1024; i++) {
            byteBuf.writeInt(0);
          }
        }
      }
    }

    if (protocolVersion.compareTo(MINECRAFT_1_13) < 0) {
      if (protocolVersion.compareTo(MINECRAFT_1_8) >= 0) {
        writeArray(byteBuf, LEGACY_FILLER_BYTES); // 1.8 - 1.12.2
      } else {
        byteBuf.writeInt(0); // compressed size
        byteBuf.writeBytes(LEGACY_FILLER_BYTES_17); // 1.7
      }
    } else if (protocolVersion.compareTo(MINECRAFT_1_15) < 0) {
      writeArray(byteBuf, MODERN_FILLER_BYTES); // 1.13 - 1.14.4
    } else if (protocolVersion.compareTo(MINECRAFT_1_18) < 0) {
      writeVarInt(byteBuf, 0); // 1.15 - 1.17.1
    } else {
      writeVarInt(byteBuf, SECTION_BYTES.length * 16);

      for (int i = 0; i < 16; i++) {
        byteBuf.writeBytes(SECTION_BYTES);
      }
    }

    if (protocolVersion.compareTo(MINECRAFT_1_9_4) >= 0) {
      writeVarInt(byteBuf, 0);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_18) >= 0) {
      byteBuf.ensureWritable(LIGHT_BYTES.length);

      if (protocolVersion.compareTo(MINECRAFT_1_20) >= 0) {
        byteBuf.writeBytes(LIGHT_BYTES, 1, LIGHT_BYTES.length - 1);
      } else {
        byteBuf.writeBytes(LIGHT_BYTES);
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
