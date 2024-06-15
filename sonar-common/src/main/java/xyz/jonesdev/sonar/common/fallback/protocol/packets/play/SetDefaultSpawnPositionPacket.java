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

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_14;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_17;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class SetDefaultSpawnPositionPacket implements FallbackPacket {
  private int x, y, z;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      byteBuf.writeInt(x);
      byteBuf.writeInt(y);
      byteBuf.writeInt(z);
    } else {
      final long encoded = protocolVersion.compareTo(MINECRAFT_1_14) < 0
        ? ((x & 0x3FFFFFFL) << 38) | ((y & 0xFFFL) << 26) | (z & 0x3FFFFFFL)
        : ((x & 0x3FFFFFFL) << 38) | ((y & 0x3FFFFFFL) << 12) | (z & 0xFFFL);

      byteBuf.writeLong(encoded);

      if (protocolVersion.compareTo(MINECRAFT_1_17) >= 0) {
        byteBuf.writeFloat(0f);
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
