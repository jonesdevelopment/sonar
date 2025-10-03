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
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class SetDefaultSpawnPositionPacket implements FallbackPacket {
  private String dimensionName;
  private int x, y, z;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      byteBuf.writeInt(x);
      byteBuf.writeInt(y);
      byteBuf.writeInt(z);
    } else {
      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_9)) {
        ProtocolUtil.writeString(byteBuf, dimensionName);
      }

      final long encoded = protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_14)
        ? ((x & 0x3FFFFFFL) << 38) | ((y & 0xFFFL) << 26) | (z & 0x3FFFFFFL)
        : ((x & 0x3FFFFFFL) << 38) | ((y & 0x3FFFFFFL) << 12) | (z & 0xFFFL);

      byteBuf.writeLong(encoded);

      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_17)) {
        byteBuf.writeFloat(0f);

        if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_9)) {
          byteBuf.writeFloat(0f); // pitch
        }
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
