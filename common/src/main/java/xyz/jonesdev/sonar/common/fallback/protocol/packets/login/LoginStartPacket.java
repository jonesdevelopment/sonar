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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.login;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class LoginStartPacket implements FallbackPacket {
  private String username;
  private @Nullable UUID uuid;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    username = ProtocolUtil.readString(byteBuf, 16);

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19)) {
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
        if (byteBuf.readBoolean()) {
          final long expiry = byteBuf.readLong();
          final long now = System.currentTimeMillis();
          if (expiry < now) {
            throw ProtocolUtil.DEBUG ? new DecoderException("Bad expiry: " + expiry) : QuietDecoderException.INSTANCE;
          }
          final byte[] key = ProtocolUtil.readByteArray(byteBuf);
          if (key.length == 0) {
            throw ProtocolUtil.DEBUG ? new DecoderException("Empty key") : QuietDecoderException.INSTANCE;
          }
          final byte[] signature = ProtocolUtil.readByteArray(byteBuf, 4096);
          if (signature.length == 0) {
            throw ProtocolUtil.DEBUG ? new DecoderException("Empty signature") : QuietDecoderException.INSTANCE;
          }
        }
      }

      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19_1)) {
        if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20_2) || byteBuf.readBoolean()) {
          uuid = ProtocolUtil.readUUID(byteBuf);
        }
      }
    }
  }
}
