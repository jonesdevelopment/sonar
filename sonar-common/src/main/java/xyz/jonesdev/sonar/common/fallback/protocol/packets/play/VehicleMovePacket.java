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

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class VehicleMovePacket implements FallbackPacket {
  private double x, y, z;
  private float yaw, pitch;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeDouble(x);
    byteBuf.writeDouble(y);
    byteBuf.writeDouble(z);
    byteBuf.writeFloat(yaw);
    byteBuf.writeFloat(pitch);
  }

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    x = byteBuf.readDouble();
    y = byteBuf.readDouble();
    z = byteBuf.readDouble();
    yaw = byteBuf.readFloat();
    pitch = byteBuf.readFloat();
  }
}
