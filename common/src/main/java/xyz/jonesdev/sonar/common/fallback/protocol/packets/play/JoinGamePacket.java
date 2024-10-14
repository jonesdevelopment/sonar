/*
 * Copyright (C) 2024 Sonar Contributors
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
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionType;

import java.util.Objects;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class JoinGamePacket implements FallbackPacket {
  private int entityId;
  private int gamemode;
  private int previousGamemode;
  private int viewDistance;
  private int simulationDistance;
  private int difficulty;
  private int maxPlayers;
  private int portalCooldown;
  private int seaLevel;
  private String[] levelNames;
  private String levelName;
  private String levelType;
  private DimensionType dimension;
  private long partialHashedSeed;
  private boolean hardcore;
  private boolean reducedDebugInfo;
  private boolean showRespawnScreen;
  private boolean debug;
  private boolean flat;
  private boolean limitedCrafting;
  private boolean secureProfile;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeInt(entityId);

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_16_2)) {
      byteBuf.writeBoolean(hardcore);
    }

    if (protocolVersion.lessThan(MINECRAFT_1_20_2)) {
      byteBuf.writeByte(gamemode);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_16)) {
      if (protocolVersion.lessThan(MINECRAFT_1_20_2)) {
        byteBuf.writeByte(previousGamemode);
      }

      writeStringArray(byteBuf, levelNames);

      final CompoundBinaryTag codec = getCodec(protocolVersion);

      if (protocolVersion.lessThan(MINECRAFT_1_20_2)) {
        writeBinaryTag(byteBuf, protocolVersion, codec);
      }

      if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_16_2) && protocolVersion.lessThan(MINECRAFT_1_19)) {
        final ListBinaryTag dimensions = codec.getCompound("minecraft:dimension_type").getList("value");
        final BinaryTag elementTag = ((CompoundBinaryTag) dimensions.get(0)).get("element");
        writeBinaryTag(byteBuf, protocolVersion, Objects.requireNonNull(elementTag));
      } else if (protocolVersion.lessThan(MINECRAFT_1_20_2)) {
        writeString(byteBuf, dimension.getKey());
      }
      if (protocolVersion.lessThan(MINECRAFT_1_20_2)) {
        writeString(byteBuf, levelName);
      }
    } else if (protocolVersion.greaterThan(MINECRAFT_1_9)) {
      byteBuf.writeInt(dimension.getLegacyId());
    } else {
      byteBuf.writeByte(dimension.getLegacyId());
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_15) && protocolVersion.lessThan(MINECRAFT_1_20_2)) {
      byteBuf.writeLong(partialHashedSeed);
    }

    if (protocolVersion.lessThan(MINECRAFT_1_14)) {
      byteBuf.writeByte(difficulty);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_16_2)) {
      writeVarInt(byteBuf, maxPlayers);
    } else {
      byteBuf.writeByte(maxPlayers);
    }

    if (protocolVersion.lessThan(MINECRAFT_1_16)) {
      writeString(byteBuf, levelType);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_14)) {
      writeVarInt(byteBuf, viewDistance);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_18)) {
      writeVarInt(byteBuf, simulationDistance);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_8)) {
      byteBuf.writeBoolean(reducedDebugInfo);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_15)) {
      byteBuf.writeBoolean(showRespawnScreen);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_20_2)) {
      byteBuf.writeBoolean(limitedCrafting);

      if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_20_5)) {
        writeVarInt(byteBuf, dimension.getId());
      } else {
        writeString(byteBuf, dimension.getKey());
      }

      writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      byteBuf.writeByte(gamemode);
      byteBuf.writeByte(previousGamemode);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_16)) {
      byteBuf.writeBoolean(debug);
      byteBuf.writeBoolean(flat);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_19)) {
      byteBuf.writeBoolean(false); // last death location
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_20)) {
      writeVarInt(byteBuf, portalCooldown);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_21_2_PRE3)) {
      writeVarInt(byteBuf, seaLevel);
    }

    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_20_5)) {
      byteBuf.writeBoolean(secureProfile);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  private static CompoundBinaryTag getCodec(final @NotNull ProtocolVersion protocolVersion) {
    if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_21)) {
      return DimensionRegistry.CODEC_1_21;
    } else if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_20)) {
      return DimensionRegistry.CODEC_1_20;
    } else if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_19_4)) {
      return DimensionRegistry.CODEC_1_19_4;
    } else if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_19_1)) {
      return DimensionRegistry.CODEC_1_19_1;
    } else if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_19)) {
      return DimensionRegistry.CODEC_1_19;
    } else if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_18_2)) {
      return DimensionRegistry.CODEC_1_18_2;
    } else if (protocolVersion.greaterThanOrEquals(MINECRAFT_1_16_2)) {
      return DimensionRegistry.CODEC_1_16_2;
    }
    return DimensionRegistry.CODEC_1_16;
  }
}
