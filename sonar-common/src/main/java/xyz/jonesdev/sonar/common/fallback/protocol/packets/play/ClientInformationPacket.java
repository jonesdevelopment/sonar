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

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readString;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readVarInt;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class ClientInformationPacket implements FallbackPacket {
  private String locale;
  private byte viewDistance;
  private int chatVisibility;
  private boolean chatColors;
  private byte difficulty; // 1.7
  private short skinParts;
  private int mainHand;
  private boolean chatFilteringEnabled; // Added in 1.17
  private boolean clientListingAllowed; // Added in 1.18, overwrites server-list "anonymous" mode

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    locale = readString(byteBuf, 16);
    viewDistance = byteBuf.readByte();
    chatVisibility = readVarInt(byteBuf);
    chatColors = byteBuf.readBoolean();

    if (protocolVersion.compareTo(MINECRAFT_1_7_6) <= 0) {
      difficulty = byteBuf.readByte();
    }

    skinParts = byteBuf.readUnsignedByte();

    if (protocolVersion.compareTo(MINECRAFT_1_9) >= 0) {
      mainHand = readVarInt(byteBuf);

      if (protocolVersion.compareTo(MINECRAFT_1_17) >= 0) {
        chatFilteringEnabled = byteBuf.readBoolean();

        if (protocolVersion.compareTo(MINECRAFT_1_18) >= 0) {
          clientListingAllowed = byteBuf.readBoolean();
        }
      }
    }
  }

  @Override
  public int expectedMinLength(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    return 2;
  }

  @Override
  public int expectedMaxLength(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    return 0xff + 1; // 256 as a hard-limit
  }
}
