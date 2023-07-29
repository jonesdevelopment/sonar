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

package xyz.jonesdev.sonar.common.fallback.protocol.packets;

import io.netty.buffer.ByteBuf;
import lombok.*;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionInfo;

import java.util.Objects;

import static xyz.jonesdev.sonar.common.protocol.ProtocolUtil.*;
import static xyz.jonesdev.sonar.common.protocol.VarIntUtil.writeVarInt;

// Mostly taken from
// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/packet/JoinGame.java
@Getter
@Setter
@ToString
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
  private String[] levelNames; // 1.16+
  private CompoundBinaryTag registry; // 1.16+
  private DimensionInfo dimensionInfo; // 1.16+
  private CompoundBinaryTag currentDimensionData; // 1.16.2+
  private short previousGamemode; // 1.16+
  private int simulationDistance; // 1.18+
  private int portalCooldown; // 1.20+

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      encode116Up(byteBuf, protocolVersion);
    } else {
      encodeLegacy(byteBuf, protocolVersion);
    }
  }

  private void encodeLegacy(ByteBuf buf, ProtocolVersion version) {
    buf.writeInt(entityId);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      buf.writeBoolean(isHardcore);
      buf.writeByte(gamemode);
    } else {
      buf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9_1) >= 0) {
      buf.writeInt(dimension);
    } else {
      buf.writeByte(dimension);
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) <= 0) {
      buf.writeByte(difficulty);
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      buf.writeLong(partialHashedSeed);
    }

    buf.writeByte(1); // max players

    writeString(buf, Objects.requireNonNull(levelType));

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      writeVarInt(buf, viewDistance);
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      buf.writeBoolean(reducedDebugInfo);
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      buf.writeBoolean(showRespawnScreen);
    }
  }

  private void encode116Up(ByteBuf buf, ProtocolVersion version) {
    buf.writeInt(entityId);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      buf.writeBoolean(isHardcore);
      buf.writeByte(gamemode);
    } else {
      buf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }

    buf.writeByte(previousGamemode);

    writeStringArray(buf, levelNames);
    writeCompoundTag(buf, registry);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0
      && version.compareTo(ProtocolVersion.MINECRAFT_1_19) < 0) {
      writeCompoundTag(buf, currentDimensionData);
      writeString(buf, dimensionInfo.getRegistryIdentifier());
    } else {
      writeString(buf, dimensionInfo.getRegistryIdentifier());
      writeString(buf, dimensionInfo.getLevelName());
    }

    buf.writeLong(partialHashedSeed);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      writeVarInt(buf, 1); // max players
    } else {
      buf.writeByte(1); // max players
    }

    writeVarInt(buf, viewDistance);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_18) >= 0) {
      writeVarInt(buf, simulationDistance);
    }

    buf.writeBoolean(reducedDebugInfo);
    buf.writeBoolean(showRespawnScreen);

    buf.writeBoolean(dimensionInfo.isDebugType());
    buf.writeBoolean(dimensionInfo.isFlat());

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      // no last death location
      buf.writeBoolean(false);
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_20) >= 0) {
      writeVarInt(buf, portalCooldown);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
