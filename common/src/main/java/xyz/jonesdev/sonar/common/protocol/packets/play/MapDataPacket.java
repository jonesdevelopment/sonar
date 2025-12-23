/*
 * Copyright (C) 2025 Sonar Contributors
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

package xyz.jonesdev.sonar.common.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.SonarPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class MapDataPacket implements SonarPacket {
  private byte[] buffer;
  private int x, y;
  private int scaling;
  private boolean locked;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    ProtocolUtil.writeVarInt(byteBuf, 0); // item damage

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      byteBuf.writeShort(buffer.length + 3);
      byteBuf.writeByte(0);
      byteBuf.writeByte(x);
      byteBuf.writeByte(y);
      byteBuf.writeBytes(buffer);
      return;
    }

    byteBuf.writeByte(scaling);

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_9)
      && protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_17)) {
      byteBuf.writeBoolean(false); // no icon
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_14)) {
      byteBuf.writeBoolean(locked);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_17)) {
      byteBuf.writeBoolean(false); // no icon
    } else {
      ProtocolUtil.writeVarInt(byteBuf, 0); // no icon
    }

    byteBuf.writeByte(128); // rows
    byteBuf.writeByte(128); // columns
    byteBuf.writeByte(x);
    byteBuf.writeByte(y);

    ProtocolUtil.writeVarInt(byteBuf, buffer.length);
    byteBuf.writeBytes(buffer);
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
