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

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarInt;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class SetPlayerPositionRotationPacket implements FallbackPacket {
  private double x, y, z;
  private float yaw, pitch;
  private int teleportId, relativeMask;
  private boolean onGround;
  private boolean dismountVehicle;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    byteBuf.writeDouble(x);
    // Account for the minimum Y bounding box issue on 1.7.2-1.7.10
    byteBuf.writeDouble(protocolVersion.greaterThanOrEquals(MINECRAFT_1_8) ? y : y + 1.62f);
    byteBuf.writeDouble(z);
    byteBuf.writeFloat(yaw);
    byteBuf.writeFloat(pitch);
    byteBuf.writeByte(relativeMask);

    if (protocolVersion.greaterThan(MINECRAFT_1_8)) {
      writeVarInt(byteBuf, teleportId);

      if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_17)
        && protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
        byteBuf.writeBoolean(dismountVehicle);
      }
    }
  }

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    x = byteBuf.readDouble();
    y = byteBuf.readDouble();
    if (protocolVersion.lessThan(MINECRAFT_1_8)) {
      // 1.7.2-1.7.10 send the minimum bounding box Y coordinate
      byteBuf.readDouble();
    }
    z = byteBuf.readDouble();
    yaw = byteBuf.readFloat();
    pitch = byteBuf.readFloat();
    onGround = byteBuf.readBoolean();
  }

  @Override
  public int expectedMaxLength(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    return protocolVersion.lessThan(MINECRAFT_1_8) ? 41 : 33;
  }

  @Override
  public int expectedMinLength(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    return 33;
  }
}
