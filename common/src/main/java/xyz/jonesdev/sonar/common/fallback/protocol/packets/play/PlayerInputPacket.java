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
public final class PlayerInputPacket implements FallbackPacket {
  private float sideways, forward;
  private boolean jump, sneak;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
      final byte mask = byteBuf.readByte();
      jump = (mask & 16) != 0;
      sneak = (mask & 32) != 0;
      return;
    }

    sideways = byteBuf.readFloat();
    forward = byteBuf.readFloat();

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      jump = byteBuf.readBoolean();
      sneak = byteBuf.readBoolean();
    } else {
      final byte flags = byteBuf.readByte();
      jump = (flags & 0x01) != 0;
      sneak = (flags & 0x02) != 0;
    }
  }
}
