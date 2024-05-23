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

import java.util.UUID;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class LoginSuccessPacket implements FallbackPacket {
  private UUID uuid;
  private String username;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      writeUUID(byteBuf, uuid);
    } else if (protocolVersion.compareTo(MINECRAFT_1_16) >= 0) {
      writeUUIDIntArray(byteBuf, uuid);
    } else if (protocolVersion.compareTo(MINECRAFT_1_7_6) >= 0) {
      writeString(byteBuf, uuid.toString());
    } else {
      writeString(byteBuf, FastUuidSansHyphens.toString(uuid));
    }

    writeString(byteBuf, username);

    if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      writeVarInt(byteBuf, 0); // properties
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20_5) >= 0) {
      byteBuf.writeBoolean(false); // should authenticate
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
