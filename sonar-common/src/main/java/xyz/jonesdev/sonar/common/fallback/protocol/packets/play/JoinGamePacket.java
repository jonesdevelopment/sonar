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
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionRegistry.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.*;

// Mostly taken from
// https://github.com/Nan1t/NanoLimbo/blob/main/src/main/java/ua/nanit/limbo/protocol/packets/play/PacketJoinGame.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class JoinGamePacket implements FallbackPacket {
  private int entityId;
  private int gamemode;
  private long partialHashedSeed;
  private boolean isHardcore;
  private int viewDistance;
  private boolean reducedDebugInfo;
  private boolean showRespawnScreen;
  private boolean limitedCrafting;
  private String[] levelNames;
  private String levelName;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeInt(entityId);

    if (protocolVersion.inBetween(MINECRAFT_1_7_2, MINECRAFT_1_7_6)) {
      byteBuf.writeByte(gamemode == 3 ? 1 : gamemode);
      byteBuf.writeByte(DEFAULT_DIMENSION_1_16.getId());
      byteBuf.writeByte(0); // difficulty
      byteBuf.writeByte(0); // max players
      writeString(byteBuf, "flat"); // level type
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_8, MINECRAFT_1_9)) {
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(DEFAULT_DIMENSION_1_16.getId());
      byteBuf.writeByte(0); // difficulty
      byteBuf.writeByte(0); // max players
      writeString(byteBuf, "flat"); // level type
      byteBuf.writeBoolean(reducedDebugInfo);
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_9_1, MINECRAFT_1_13_2)) {
      byteBuf.writeByte(gamemode);
      byteBuf.writeInt(DEFAULT_DIMENSION_1_16.getId());
      byteBuf.writeByte(0); // difficulty
      byteBuf.writeByte(0); // max players
      writeString(byteBuf, "flat"); // level type
      byteBuf.writeBoolean(reducedDebugInfo);
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_14, MINECRAFT_1_14_4)) {
      byteBuf.writeByte(gamemode);
      byteBuf.writeInt(DEFAULT_DIMENSION_1_16.getId());
      byteBuf.writeByte(0); // max players
      writeString(byteBuf, "flat"); // level type
      writeVarInt(byteBuf, viewDistance);
      byteBuf.writeBoolean(reducedDebugInfo);
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_15, MINECRAFT_1_15_2)) {
      byteBuf.writeByte(gamemode);
      byteBuf.writeInt(DEFAULT_DIMENSION_1_16.getId());
      byteBuf.writeLong(partialHashedSeed);
      byteBuf.writeByte(0); // max players
      writeString(byteBuf, "flat"); // level type
      writeVarInt(byteBuf, viewDistance);
      byteBuf.writeBoolean(reducedDebugInfo);
      byteBuf.writeBoolean(showRespawnScreen);
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_16, MINECRAFT_1_16_1)) {
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(-1); // previous gamemode
      writeStringArray(byteBuf, levelNames);
      writeCompoundTag(byteBuf, OLD_CODEC);
      writeString(byteBuf, DEFAULT_DIMENSION_1_16.getIdentifier());
      writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      byteBuf.writeByte(0); // max players
      writeVarInt(byteBuf, viewDistance);
      byteBuf.writeBoolean(reducedDebugInfo);
      byteBuf.writeBoolean(showRespawnScreen);
      byteBuf.writeBoolean(false); // debug type
      byteBuf.writeBoolean(false); // flat
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_16_2, MINECRAFT_1_17_1)) {
      byteBuf.writeBoolean(isHardcore);
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(-1); // previous gamemode
      writeStringArray(byteBuf, levelNames);
      writeCompoundTag(byteBuf, CODEC_1_16);
      writeCompoundTag(byteBuf, DEFAULT_DIMENSION_1_16.getTag());
      writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      writeVarInt(byteBuf, 0); // max players
      writeVarInt(byteBuf, viewDistance);
      byteBuf.writeBoolean(reducedDebugInfo);
      byteBuf.writeBoolean(showRespawnScreen);
      byteBuf.writeBoolean(false); // debug type
      byteBuf.writeBoolean(false); // flat
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_18, MINECRAFT_1_18_2)) {
      byteBuf.writeBoolean(isHardcore);
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(-1); // previous gamemode
      writeStringArray(byteBuf, levelNames);
      if (protocolVersion.compareTo(MINECRAFT_1_18_2) >= 0) {
        writeCompoundTag(byteBuf, CODEC_1_18_2);
        writeCompoundTag(byteBuf, DEFAULT_DIMENSION_1_18_2.getTag());
      } else {
        writeCompoundTag(byteBuf, CODEC_1_16);
        writeCompoundTag(byteBuf, DEFAULT_DIMENSION_1_16.getTag());
      }
      writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      writeVarInt(byteBuf, 0); // max players
      writeVarInt(byteBuf, viewDistance);
      writeVarInt(byteBuf, viewDistance); // simulation distance
      byteBuf.writeBoolean(reducedDebugInfo);
      byteBuf.writeBoolean(showRespawnScreen);
      byteBuf.writeBoolean(false); // debug type
      byteBuf.writeBoolean(false); // flat
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_19, MINECRAFT_1_19_4)) {
      byteBuf.writeBoolean(isHardcore);
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(-1); // previous gamemode
      writeStringArray(byteBuf, levelNames);
      if (protocolVersion.compareTo(MINECRAFT_1_19_1) >= 0) {
        if (protocolVersion.compareTo(MINECRAFT_1_19_4) >= 0) {
          writeCompoundTag(byteBuf, CODEC_1_19_4);
        } else {
          writeCompoundTag(byteBuf, CODEC_1_19_1);
        }
      } else {
        writeCompoundTag(byteBuf, CODEC_1_19);
      }
      writeString(byteBuf, levelName); // world type
      writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      writeVarInt(byteBuf, 0); // max players
      writeVarInt(byteBuf, viewDistance);
      writeVarInt(byteBuf, viewDistance); // simulation distance
      byteBuf.writeBoolean(reducedDebugInfo);
      byteBuf.writeBoolean(showRespawnScreen);
      byteBuf.writeBoolean(false); // debug type
      byteBuf.writeBoolean(false); // flat
      byteBuf.writeBoolean(false); // no last death location
      return;
    }

    if (protocolVersion.equals(MINECRAFT_1_20)) {
      byteBuf.writeBoolean(isHardcore);
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(-1); // previous gamemode
      writeStringArray(byteBuf, levelNames);
      writeCompoundTag(byteBuf, CODEC_1_20);
      writeString(byteBuf, levelName); // world type
      writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      writeVarInt(byteBuf, 0); // max players
      writeVarInt(byteBuf, viewDistance);
      writeVarInt(byteBuf, viewDistance); // simulation distance
      byteBuf.writeBoolean(reducedDebugInfo);
      byteBuf.writeBoolean(showRespawnScreen);
      byteBuf.writeBoolean(false); // debug type
      byteBuf.writeBoolean(false); // flat
      byteBuf.writeBoolean(false); // no last death location
      writeVarInt(byteBuf, 0);
      return;
    }

    if (protocolVersion.inBetween(MINECRAFT_1_20_2, MINECRAFT_1_20_3)) {
      byteBuf.writeBoolean(isHardcore);
      writeStringArray(byteBuf, levelNames);
      writeVarInt(byteBuf, 0); // max players
      writeVarInt(byteBuf, viewDistance);
      writeVarInt(byteBuf, viewDistance); // simulation distance
      byteBuf.writeBoolean(reducedDebugInfo);
      byteBuf.writeBoolean(showRespawnScreen);
      byteBuf.writeBoolean(false); // limited crafting
      writeString(byteBuf, levelName);
      writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(-1); // previous gamemode
      byteBuf.writeBoolean(false); // debug type
      byteBuf.writeBoolean(false); // flat
      byteBuf.writeBoolean(false); // no last death location
      writeVarInt(byteBuf, 0);
      return;
    }

    if (protocolVersion.compareTo(MINECRAFT_1_20_5) >= 0) {
      byteBuf.writeBoolean(isHardcore);
      writeStringArray(byteBuf, levelNames);
      writeVarInt(byteBuf, 0); // max players
      writeVarInt(byteBuf, viewDistance);
      writeVarInt(byteBuf, viewDistance); // simulation distance
      byteBuf.writeBoolean(reducedDebugInfo);
      byteBuf.writeBoolean(showRespawnScreen);
      byteBuf.writeBoolean(false); // limited crafting
      // It depends on the dimension data we send in RegistryData. If we only send one dimension. It should be 0.
      writeVarInt(byteBuf, 0);
      writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(-1); // previous gamemode
      byteBuf.writeBoolean(false); // debug type
      byteBuf.writeBoolean(false); // flat
      byteBuf.writeBoolean(false); // no last death location
      writeVarInt(byteBuf, 0); // pearl cooldown
      byteBuf.writeBoolean(false); // secure profile
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
