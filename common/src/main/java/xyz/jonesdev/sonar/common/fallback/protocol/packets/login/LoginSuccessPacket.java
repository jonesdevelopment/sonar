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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.util.FastUuidSansHyphens;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class LoginSuccessPacket implements FallbackPacket {
  private UUID uuid;
  private String username;
  private boolean strictErrorHandling;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16)) {
      ProtocolUtil.writeUUID(byteBuf, uuid);
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_7_6)) {
      ProtocolUtil.writeString(byteBuf, uuid.toString());
    } else {
      ProtocolUtil.writeString(byteBuf, FastUuidSansHyphens.toString(uuid));
    }

    ProtocolUtil.writeString(byteBuf, username);

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19)) {
      // We don't need to send any properties to the client
      ProtocolUtil.writeVarInt(byteBuf, 0);
    }

    if (protocolVersion.equals(ProtocolVersion.MINECRAFT_1_20_5)
      || protocolVersion.equals(ProtocolVersion.MINECRAFT_1_21)) {
      // Whether the client should disconnect on its own if it receives invalid data from the server
      byteBuf.writeBoolean(strictErrorHandling);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
