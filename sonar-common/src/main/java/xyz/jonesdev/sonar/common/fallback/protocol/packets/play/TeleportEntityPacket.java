/*
 * Copyright (C) 2024 Sonar Contributors
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

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_9;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarInt;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class TeleportEntityPacket implements FallbackPacket {
  private int entityId;
  private double x, y, z;
  private boolean onGround;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(MINECRAFT_1_8) >= 0) {
      writeVarInt(byteBuf, entityId);
    } else {
      byteBuf.writeInt(entityId);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_9) >= 0) {
      byteBuf.writeDouble(x);
      byteBuf.writeDouble(y);
      byteBuf.writeDouble(z);
    } else {
      byteBuf.writeInt((int) (x * 32D));
      byteBuf.writeInt((int) (y * 32D));
      byteBuf.writeInt((int) (z * 32D));
    }

    byteBuf.writeByte(0); // pitch or yaw
    byteBuf.writeByte(0); // yaw or pitch

    if (protocolVersion.compareTo(MINECRAFT_1_8) >= 0) {
      byteBuf.writeBoolean(onGround);
    }
  }

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }
}
