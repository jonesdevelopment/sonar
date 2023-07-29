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

package xyz.jonesdev.sonar.velocity.fallback;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.exception.ReflectionException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collections;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

@UtilityClass
public class FallbackPackets {
  private final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  private final MethodHandle CURRENT_DIMENSION_DATA;
  private final MethodHandle REGISTRY;
  private final MethodHandle LEVEL_NAMES;

  // https://github.com/Elytrium/LimboAPI/blob/91bedd5dad5e659092fbb0a7411bd00d67044d01/plugin/src/main/java/net/elytrium/limboapi/server/LimboImpl.java#L813
  static {
    try {
      CURRENT_DIMENSION_DATA = MethodHandles.privateLookupIn(JoinGame.class, LOOKUP)
        .findSetter(JoinGame.class,
          "currentDimensionData", CompoundBinaryTag.class
        );

      REGISTRY = MethodHandles.privateLookupIn(JoinGame.class, LOOKUP)
        .findSetter(JoinGame.class,
          "registry", CompoundBinaryTag.class
        );

      LEVEL_NAMES = MethodHandles.privateLookupIn(JoinGame.class, LOOKUP)
        .findSetter(JoinGame.class,
          "levelNames", ImmutableSet.class
        );
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
    }
  }

  public final JoinGame LEGACY_JOIN_GAME = createLegacyJoinGamePacket();
  private JoinGame JOIN_GAME_1_16;
  private JoinGame JOIN_GAME_1_16_2;
  private JoinGame JOIN_GAME_1_18_2;
  private JoinGame JOIN_GAME_1_19_1;
  private JoinGame JOIN_GAME_1_19_4;
  private JoinGame JOIN_GAME_1_20;

  public static void prepare() {
    JOIN_GAME_1_16 = createJoinGamePacket(MINECRAFT_1_16);
    JOIN_GAME_1_16_2 = createJoinGamePacket(MINECRAFT_1_16_2);
    JOIN_GAME_1_18_2 = createJoinGamePacket(MINECRAFT_1_18_2);
    JOIN_GAME_1_19_1 = createJoinGamePacket(MINECRAFT_1_19_1);
    JOIN_GAME_1_19_4 = createJoinGamePacket(MINECRAFT_1_19_4);
    JOIN_GAME_1_20 = createJoinGamePacket(MINECRAFT_1_20);
  }

  public static JoinGame getJoinPacketForVersion(final ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return LEGACY_JOIN_GAME; // 1.7-1.15.2
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_1) <= 0) {
      return JOIN_GAME_1_16; // 1.16-1.16.1
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18) <= 0) {
      return JOIN_GAME_1_16_2; // 1.16.2-1.18
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19) <= 0) {
      return JOIN_GAME_1_18_2; // 1.18.1-1.19
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return JOIN_GAME_1_19_1; // 1.19.1-1.19.3
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return JOIN_GAME_1_19_4; // 1.19.4
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20) <= 0) {
      return JOIN_GAME_1_20; // 1.20-1.20.1
    }
    throw new IllegalStateException("Unsupported protocol version");
  }

  private @NotNull JoinGame createLegacyJoinGamePacket() {
    final JoinGame joinGame = new JoinGame();

    joinGame.setLevelType("flat");
    joinGame.setGamemode((short) 3);
    joinGame.setReducedDebugInfo(true);
    return joinGame;
  }

  private @NotNull JoinGame createJoinGamePacket(final ProtocolVersion protocolVersion) {
    final JoinGame joinGame = new JoinGame();

    joinGame.setLevelType("flat");
    joinGame.setGamemode((short) 3);
    joinGame.setReducedDebugInfo(true);
    joinGame.setDifficulty((short) 0);
    joinGame.setMaxPlayers(1);
    joinGame.setDimensionInfo(new DimensionInfo(
      Sonar.get().getConfig().DIMENSION_KEY,
      "sonar", false, false
    ));

    final CompoundBinaryTag.Builder registryContainer = CompoundBinaryTag.builder();
    final ListBinaryTag encodedDimensionRegistry = ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
      .add(createDimensionData(protocolVersion))
      .build();

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      final CompoundBinaryTag.Builder dimensionRegistryEntry = CompoundBinaryTag.builder();

      dimensionRegistryEntry.putString("type", "minecraft:dimension_type");
      dimensionRegistryEntry.put("value", encodedDimensionRegistry);

      registryContainer.put("minecraft:dimension_type", dimensionRegistryEntry.build());

      final CompoundBinaryTag.Builder effectsTagBuilder = CompoundBinaryTag.builder()
        .putInt("sky_color", Sonar.get().getConfig().DIMENSION_SKY_COLOR)
        .putInt("fog_color", Sonar.get().getConfig().DIMENSION_FOG_COLOR)
        .putInt("water_color", 0)
        .putInt("water_fog_color", 0);

      final CompoundBinaryTag.Builder elementTagBuilder = CompoundBinaryTag.builder()
        .putFloat("depth", 0.125f)
        .putFloat("temperature", 0.8f)
        .putFloat("scale", 0.05f)
        .putFloat("downfall", 0.4f)
        .putString("category", "plains")
        .put("effects", effectsTagBuilder.build());

      if (protocolVersion.compareTo(MINECRAFT_1_19_4) >= 0) {
        elementTagBuilder.putBoolean("has_precipitation", false);
      } else {
        elementTagBuilder.putString("precipitation", "rain");
      }

      registryContainer.put("minecraft:worldgen/biome", CompoundBinaryTag.builder()
        .putString("type", "minecraft:worldgen/biome")
        .put("value", ListBinaryTag.from(Collections.singletonList(
          CompoundBinaryTag.builder()
            .putString("name", "minecraft:plains")
            .putInt("id", 1)
            .put("element", elementTagBuilder.build())
            .build()
        ))).build()
      );

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19) == 0) {
        registryContainer.put("minecraft:chat_type", CHAT_TYPE_119);
      } else if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
        registryContainer.put("minecraft:chat_type", CHAT_TYPE_1191);
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_4) == 0) {
        registryContainer.put("minecraft:damage_type", DAMAGE_TYPE_1194);
      } else if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20) >= 0) {
        registryContainer.put("minecraft:damage_type", DAMAGE_TYPE_120);
      }
    } else {
      registryContainer.put("dimension", encodedDimensionRegistry);
    }

    try {
      CompoundBinaryTag currentDimensionData = encodedDimensionRegistry.getCompound(0);

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
        currentDimensionData = currentDimensionData.getCompound("element");
      }

      CURRENT_DIMENSION_DATA.invokeExact(joinGame, currentDimensionData);
      LEVEL_NAMES.invokeExact(joinGame, ImmutableSet.of(Sonar.get().getConfig().DIMENSION_KEY));
      REGISTRY.invokeExact(joinGame, registryContainer.build());
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
    }
    return joinGame;
  }
}
