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
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.map.MapInfo;

import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarInt;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class MapDataPacket implements FallbackPacket {
  private MapInfo mapInfo;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    writeVarInt(byteBuf, 0);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      byteBuf.writeShort(mapInfo.getBuffer().length + 3);
      byteBuf.writeByte(0); // scaling
      byteBuf.writeByte(mapInfo.getX());
      byteBuf.writeByte(mapInfo.getY());

      byteBuf.writeBytes(mapInfo.getBuffer());
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

      byteBuf.writeByte(mapInfo.getColumns());
      byteBuf.writeByte(mapInfo.getRows());
      byteBuf.writeByte(mapInfo.getX());
      byteBuf.writeByte(mapInfo.getY());

      writeVarInt(byteBuf, mapInfo.getBuffer().length);
      byteBuf.writeBytes(mapInfo.getBuffer());
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
