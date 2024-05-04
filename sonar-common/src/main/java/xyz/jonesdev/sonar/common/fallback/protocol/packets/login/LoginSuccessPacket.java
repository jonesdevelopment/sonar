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
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import java.util.UUID;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.*;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarInt;

@Getter
@ToString
public final class LoginSuccessPacket implements FallbackPacket {
  // Use a cached UUID because the client actually does nothing with it...
  public static final UUID PLACEHOLDER_UUID = new UUID(1L, 1L);
  private static final String PLACEHOLDER_UUID_STRING = PLACEHOLDER_UUID.toString();
  private static final String PLACEHOLDER_UUID_LEGACY = "00000000000000010000000000000001";

  // Use a cached username because the client actually does nothing with it...
  private static final String PLACEHOLDER_USERNAME = "Sonar";

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      writeUUID(byteBuf, PLACEHOLDER_UUID);
    } else if (protocolVersion.compareTo(MINECRAFT_1_16) >= 0) {
      writeUUIDIntArray(byteBuf, PLACEHOLDER_UUID);
    } else if (protocolVersion.compareTo(MINECRAFT_1_7_6) >= 0) {
      writeString(byteBuf, PLACEHOLDER_UUID_STRING);
    } else {
      writeString(byteBuf, PLACEHOLDER_UUID_LEGACY);
    }

    writeString(byteBuf, PLACEHOLDER_USERNAME);

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
