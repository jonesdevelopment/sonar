/*
 * Copyright (C) 2023, jones
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

package jones.sonar.velocity.fallback;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import jones.sonar.api.Sonar;
import jones.sonar.velocity.fallback.dimension.Biome;
import jones.sonar.velocity.fallback.dimension.PacketDimension;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class FallbackPackets {
  private final PacketDimension USED_DIMENSION = PacketDimension.OVERWORLD;
  public final JoinGame LEGACY_JOIN_GAME = new JoinGame();

  static {
    LEGACY_JOIN_GAME.setGamemode((short) 3);
    LEGACY_JOIN_GAME.setLevelType("flat");
    LEGACY_JOIN_GAME.setDimension(USED_DIMENSION.getLegacyID());
  }

  private static final ImmutableSet<String> LEVELS = ImmutableSet.of(
    PacketDimension.OVERWORLD.getKey(),
    PacketDimension.NETHER.getKey(),
    PacketDimension.THE_END.getKey()
  );

  private static final MethodHandle PARTIAL_HASHED_SEED;
  private static final MethodHandle CURRENT_DIMENSION_DATA;
  private static final MethodHandle REGISTRY;
  private static final MethodHandle LEVEL_NAMES;

  private static final CompoundBinaryTag CHAT_TYPE_119;
  private static final CompoundBinaryTag CHAT_TYPE_1191;
  private static final CompoundBinaryTag DAMAGE_TYPE_1194;

  // https://github.com/Elytrium/LimboAPI/blob/91bedd5dad5e659092fbb0a7411bd00d67044d01/plugin/src/main/java/net/elytrium/limboapi/server/LimboImpl.java#L813
  static {
    try {
      PARTIAL_HASHED_SEED = MethodHandles.privateLookupIn(JoinGame.class, MethodHandles.lookup())
        .findSetter(JoinGame.class, "partialHashedSeed", long.class);
      CURRENT_DIMENSION_DATA = MethodHandles.privateLookupIn(JoinGame.class, MethodHandles.lookup())
        .findSetter(JoinGame.class, "currentDimensionData", CompoundBinaryTag.class);
      REGISTRY = MethodHandles.privateLookupIn(JoinGame.class, MethodHandles.lookup())
        .findSetter(JoinGame.class, "registry", CompoundBinaryTag.class);
      LEVEL_NAMES = MethodHandles.privateLookupIn(JoinGame.class, MethodHandles.lookup())
        .findSetter(JoinGame.class, "levelNames", ImmutableSet.class);

      try (final InputStream stream = Sonar.class.getResourceAsStream("/mappings/chat_1_19.nbt")) {
        CHAT_TYPE_119 = BinaryTagIO.unlimitedReader().read(Objects.requireNonNull(stream),
          BinaryTagIO.Compression.GZIP);
      }
      try (final InputStream stream = Sonar.class.getResourceAsStream("/mappings/chat_1_19_1.nbt")) {
        CHAT_TYPE_1191 = BinaryTagIO.unlimitedReader().read(Objects.requireNonNull(stream),
          BinaryTagIO.Compression.GZIP);
      }
      try (final InputStream stream = Sonar.class.getResourceAsStream("/mappings/damage_1_19_4.nbt")) {
        DAMAGE_TYPE_1194 = BinaryTagIO.unlimitedReader().read(Objects.requireNonNull(stream),
          BinaryTagIO.Compression.GZIP);
      }
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  public final JoinGame JOIN_GAME_1_16_2 = createJoinGamePacket(ProtocolVersion.MINECRAFT_1_16_2);
  public final JoinGame JOIN_GAME_1_18_2 = createJoinGamePacket(ProtocolVersion.MINECRAFT_1_18_2);
  public final JoinGame JOIN_GAME_1_19_1 = createJoinGamePacket(ProtocolVersion.MINECRAFT_1_19_1);
  public final JoinGame JOIN_GAME_1_19_4 = createJoinGamePacket(ProtocolVersion.MINECRAFT_1_19_4);

  private JoinGame createJoinGamePacket(final ProtocolVersion version) {
    final JoinGame joinGame = new JoinGame();

    joinGame.setIsHardcore(true);
    joinGame.setLevelType("flat");
    joinGame.setGamemode((short) 3);
    joinGame.setPreviousGamemode((short) -1);
    joinGame.setReducedDebugInfo(true);
    joinGame.setDimension(1);
    joinGame.setDifficulty((short) 0);
    joinGame.setMaxPlayers(1);

    try {
      PARTIAL_HASHED_SEED.invokeExact(joinGame, ThreadLocalRandom.current().nextLong());
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }

    joinGame.setDimensionInfo(new DimensionInfo(USED_DIMENSION.getKey(), USED_DIMENSION.getKey(), false, false));

    CompoundBinaryTag.Builder registryContainer = CompoundBinaryTag.builder();
    ListBinaryTag encodedDimensionRegistry = ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
      .add(createDimensionData(PacketDimension.OVERWORLD, version))
      .add(createDimensionData(PacketDimension.NETHER, version))
      .add(createDimensionData(PacketDimension.THE_END, version))
      .build();

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      final CompoundBinaryTag.Builder dimensionRegistryEntry = CompoundBinaryTag.builder();

      dimensionRegistryEntry.putString("type", "minecraft:dimension_type");
      dimensionRegistryEntry.put("value", encodedDimensionRegistry);

      registryContainer.put("minecraft:dimension_type", dimensionRegistryEntry.build());
      registryContainer.put("minecraft:worldgen/biome", Biome.getRegistry(version));

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) == 0) {
        registryContainer.put("minecraft:chat_type", CHAT_TYPE_119);
      } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
        registryContainer.put("minecraft:chat_type", CHAT_TYPE_1191);
      }

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_19_4) >= 0) {
        registryContainer.put("minecraft:damage_type", DAMAGE_TYPE_1194);
      }
    } else {
      registryContainer.put("dimension", encodedDimensionRegistry);
    }

    try {
      CompoundBinaryTag currentDimensionData = encodedDimensionRegistry.getCompound(USED_DIMENSION.getModernID());

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
        currentDimensionData = currentDimensionData.getCompound("element");
      }

      CURRENT_DIMENSION_DATA.invokeExact(joinGame, currentDimensionData);
      LEVEL_NAMES.invokeExact(joinGame, LEVELS);
      REGISTRY.invokeExact(joinGame, registryContainer.build());
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }

    return joinGame;
  }

  // https://github.com/Elytrium/LimboAPI/blob/91bedd5dad5e659092fbb0a7411bd00d67044d01/plugin/src/main/java/net/elytrium/limboapi/server/LimboImpl.java#L552
  private CompoundBinaryTag createDimensionData(final PacketDimension dimension, final ProtocolVersion version) {
    CompoundBinaryTag details = CompoundBinaryTag.builder()
      .putBoolean("natural", false)
      .putFloat("ambient_light", 0.0F)
      .putBoolean("shrunk", false)
      .putBoolean("ultrawarm", false)
      .putBoolean("has_ceiling", false)
      .putBoolean("has_skylight", true)
      .putBoolean("piglin_safe", false)
      .putBoolean("bed_works", false)
      .putBoolean("respawn_anchor_works", false)
      .putBoolean("has_raids", false)
      .putInt("logical_height", 256)
      .putString("infiniburn", version.compareTo(ProtocolVersion.MINECRAFT_1_18_2) >= 0 ? "#minecraft" +
        ":infiniburn_nether" : "minecraft:infiniburn_nether")
      .putDouble("coordinate_scale", 1.0)
      .putString("effects", dimension.getKey())
      .putInt("min_y", 0)
      .putInt("height", 256)
      .putInt("monster_spawn_block_light_limit", 0)
      .putInt("monster_spawn_light_level", 0)
      .build();

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      return CompoundBinaryTag.builder()
        .putString("name", dimension.getKey())
        .putInt("id", dimension.getModernID())
        .put("element", details)
        .build();
    } else {
      return details.putString("name", dimension.getKey());
    }
  }
}
