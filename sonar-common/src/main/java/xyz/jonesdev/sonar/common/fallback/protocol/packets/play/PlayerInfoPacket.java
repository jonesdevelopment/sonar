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

import java.util.EnumSet;
import java.util.UUID;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class PlayerInfoPacket implements FallbackPacket {
  private String username;
  private UUID uuid;
  private int gamemode;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.compareTo(MINECRAFT_1_8) < 0) {
      writeString(byteBuf, username);
      byteBuf.writeBoolean(true); // online
      byteBuf.writeShort(0);
      return;
    }

    // https://wiki.vg/Protocol#player-info:player-actions
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) >= 0) {
      final EnumSet<Action> actions = EnumSet.noneOf(Action.class);
      actions.add(Action.ADD_PLAYER);
      actions.add(Action.UPDATE_LISTED);
      actions.add(Action.UPDATE_GAMEMODE);
      writeEnumSet(byteBuf, actions, Action.class);

      writeVarInt(byteBuf, 1); // size
      writeUUID(byteBuf, uuid);
      writeString(byteBuf, username);
      writeVarInt(byteBuf, 0); // properties

      byteBuf.writeBoolean(true); // update listed
      writeVarInt(byteBuf, gamemode);
      return;
    }

    writeVarInt(byteBuf, 0); // ADD_PLAYER
    writeVarInt(byteBuf, 1);
    writeUUID(byteBuf, uuid);
    writeString(byteBuf, username);
    writeVarInt(byteBuf, 0);
    writeVarInt(byteBuf, gamemode);
    writeVarInt(byteBuf, 60);
    byteBuf.writeBoolean(false);

    if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      byteBuf.writeBoolean(false);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public enum Action {
    ADD_PLAYER,
    INITIALIZE_CHAT,
    UPDATE_GAMEMODE,
    UPDATE_LISTED,
    UPDATE_LATENCY,
    UPDATE_DISPLAY_NAME
  }
}
