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
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarInt;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class MapDataPacket implements FallbackPacket {
  private int[] buffer;
  private int x, y;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    writeVarInt(byteBuf, 0);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      byteBuf.writeShort(buffer.length + 3);
      byteBuf.writeByte(0); // scaling
      byteBuf.writeByte(x);
      byteBuf.writeByte(y);
      writeArray(byteBuf, buffer);
    } else {
      byteBuf.writeByte(0); // scaling

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0
        && protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17) < 0) {
        byteBuf.writeBoolean(false);
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
        byteBuf.writeBoolean(false);
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
        byteBuf.writeBoolean(false);
      } else {
        writeVarInt(byteBuf, 0);
      }

      byteBuf.writeByte(128); // rows
      byteBuf.writeByte(128); // columns
      byteBuf.writeByte(x);
      byteBuf.writeByte(y);

      writeVarInt(byteBuf, buffer.length);
      writeArray(byteBuf, buffer);
    }
  }

  private static void writeArray(final ByteBuf byteBuf, final int @NotNull [] array) {
    for (final int i : array) {
      byteBuf.writeByte(i);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
