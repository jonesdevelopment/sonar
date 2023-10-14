/*
 * Copyright (C) 2023 Sonar Contributors
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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionInfo;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.*;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarInt;

// Mostly taken from
// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/packet/JoinGame.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class JoinGame implements FallbackPacket {
  private int entityId;
  private short gamemode;
  private int dimension;
  private long partialHashedSeed; // 1.15+
  private short difficulty;
  private boolean isHardcore;
  private @Nullable String levelType;
  private int viewDistance; // 1.14+
  private boolean reducedDebugInfo;
  private boolean showRespawnScreen;
  private boolean doLimitedCrafting; // 1.20.2+
  private String[] levelNames; // 1.16+
  private CompoundBinaryTag registry; // 1.16+
  private DimensionInfo dimensionInfo; // 1.16+
  private CompoundBinaryTag currentDimensionData; // 1.16.2+
  private short previousGamemode; // 1.16+
  private int simulationDistance; // 1.18+
  private int portalCooldown; // 1.20+

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
      // haha funny, they made 1.20.2 more complicated
      encode1202Up(byteBuf, protocolVersion);
    } else if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      // Minecraft 1.16 and above have significantly more complicated logic for reading this packet,
      // so separate it out.
      encode116Up(byteBuf, protocolVersion);
    } else {
      encodeLegacy(byteBuf, protocolVersion);
    }
  }

  private void encodeLegacy(final @NotNull ByteBuf byteBuf,
                            final @NotNull ProtocolVersion protocolVersion) {
    byteBuf.writeInt(entityId);

    if (protocolVersion.compareTo(MINECRAFT_1_16_2) >= 0) {
      byteBuf.writeBoolean(isHardcore);
      byteBuf.writeByte(gamemode);
    } else {
      byteBuf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_9_1) >= 0) {
      byteBuf.writeInt(dimension);
    } else {
      byteBuf.writeByte(dimension);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      byteBuf.writeByte(difficulty);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_15) >= 0) {
      byteBuf.writeLong(partialHashedSeed);
    }

    byteBuf.writeByte(1); // max players

    writeString(byteBuf, levelType);

    if (protocolVersion.compareTo(MINECRAFT_1_14) >= 0) {
      writeVarInt(byteBuf, viewDistance);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_8) >= 0) {
      byteBuf.writeBoolean(reducedDebugInfo);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_15) >= 0) {
      byteBuf.writeBoolean(showRespawnScreen);
    }
  }

  private void encode116Up(final @NotNull ByteBuf byteBuf,
                           final @NotNull ProtocolVersion protocolVersion) {
    byteBuf.writeInt(entityId);

    if (protocolVersion.compareTo(MINECRAFT_1_16_2) >= 0) {
      byteBuf.writeBoolean(isHardcore);
      byteBuf.writeByte(gamemode);
    } else {
      byteBuf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }

    byteBuf.writeByte(previousGamemode);

    writeStringArray(byteBuf, levelNames);
    writeCompoundTag(byteBuf, registry);

    if (protocolVersion.compareTo(MINECRAFT_1_16_2) >= 0
      && protocolVersion.compareTo(MINECRAFT_1_19) < 0) {
      writeCompoundTag(byteBuf, currentDimensionData);
      writeString(byteBuf, dimensionInfo.getIdentifier());
    } else {
      writeString(byteBuf, dimensionInfo.getIdentifier());
      writeString(byteBuf, dimensionInfo.getLevelName());
    }

    byteBuf.writeLong(partialHashedSeed);

    if (protocolVersion.compareTo(MINECRAFT_1_16_2) >= 0) {
      writeVarInt(byteBuf, 1); // max players
    } else {
      byteBuf.writeByte(1); // max players
    }

    writeVarInt(byteBuf, viewDistance);
    if (protocolVersion.compareTo(MINECRAFT_1_18) >= 0) {
      writeVarInt(byteBuf, simulationDistance);
    }

    byteBuf.writeBoolean(reducedDebugInfo);
    byteBuf.writeBoolean(showRespawnScreen);

    byteBuf.writeBoolean(dimensionInfo.isDebug());
    byteBuf.writeBoolean(dimensionInfo.isFlat());

    if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      // no last death location
      byteBuf.writeBoolean(false);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_20) >= 0) {
      writeVarInt(byteBuf, portalCooldown);
    }
  }

  private void encode1202Up(final @NotNull ByteBuf byteBuf,
                            final @SuppressWarnings("unused") ProtocolVersion protocolVersion) {
    byteBuf.writeInt(entityId);
    byteBuf.writeBoolean(isHardcore);

    writeStringArray(byteBuf, levelNames);

    writeVarInt(byteBuf, 1); // max players

    writeVarInt(byteBuf, viewDistance);
    writeVarInt(byteBuf, simulationDistance);

    byteBuf.writeBoolean(reducedDebugInfo);
    byteBuf.writeBoolean(showRespawnScreen);
    byteBuf.writeBoolean(doLimitedCrafting);

    writeString(byteBuf, dimensionInfo.getIdentifier());
    writeString(byteBuf, dimensionInfo.getLevelName());
    byteBuf.writeLong(partialHashedSeed);

    byteBuf.writeByte(gamemode);
    byteBuf.writeByte(previousGamemode);

    byteBuf.writeBoolean(dimensionInfo.isDebug());
    byteBuf.writeBoolean(dimensionInfo.isFlat());

    // no last death location
    byteBuf.writeBoolean(false);

    writeVarInt(byteBuf, portalCooldown);
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
