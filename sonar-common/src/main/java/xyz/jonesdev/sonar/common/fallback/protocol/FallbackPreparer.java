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

package xyz.jonesdev.sonar.common.fallback.protocol;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.exception.ReflectionException;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockPosition;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockType;
import xyz.jonesdev.sonar.common.fallback.protocol.block.ChangedBlock;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionInfo;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.*;

import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;

@UtilityClass
public class FallbackPreparer {
  private final Random RANDOM = new Random();
  private final DimensionInfo DIMENSION = new DimensionInfo(
    "minecraft:overworld",
    "sonar", false, false
  );

  // Mappings
  private final CompoundBinaryTag CHAT_TYPE_119;
  private final CompoundBinaryTag CHAT_TYPE_1191;
  private final CompoundBinaryTag DAMAGE_TYPE_1194;
  private final CompoundBinaryTag DAMAGE_TYPE_120;

  static {
    CHAT_TYPE_119 = getMapping("chat_1_19.nbt");
    CHAT_TYPE_1191 = getMapping("chat_1_19_1.nbt");
    DAMAGE_TYPE_1194 = getMapping("damage_1_19_4.nbt");
    DAMAGE_TYPE_120 = getMapping("damage_type_1_20.nbt");
  }

  // JoinGame
  public JoinGame LEGACY_JOIN_GAME;
  private JoinGame JOIN_GAME_1_16;
  private JoinGame JOIN_GAME_1_16_2;
  private JoinGame JOIN_GAME_1_18_2;
  private JoinGame JOIN_GAME_1_19_1;
  private JoinGame JOIN_GAME_1_19_4;
  private JoinGame JOIN_GAME_1_20;

  // Abilities
  public final Abilities DEFAULT_ABILITIES = new Abilities((byte) 0, 0f, 0f);

  // Chunks
  public final EmptyChunkData EMPTY_CHUNK_DATA = new EmptyChunkData(0, 0);
  public PositionLook SPAWN_TELEPORT;

  // Collisions
  public final int BLOCKS_PER_ROW = 8; // 8 * 8 = 64 (max)
  private final int SPAWN_BUFFER = 10; // player spawns at 255 + 10 (10 blocks above the platform)
  public final int DEFAULT_Y_COLLIDE_POSITION = 255; // 255 is max

  public UpdateSectionBlocks UPDATE_SECTION_BLOCKS;
  private final ChangedBlock[] CHANGED_BLOCKS = new ChangedBlock[BLOCKS_PER_ROW * BLOCKS_PER_ROW];

  public int MAX_MOVEMENT_TICK;
  public double[] PREPARED_MOVEMENT_PACKETS;
  public int DYNAMIC_SPAWN_BUFFER = SPAWN_BUFFER;
  public int DYNAMIC_SPAWN_Y_POSITION;

  public void prepare() {
    LEGACY_JOIN_GAME = createLegacyJoinGamePacket();
    JOIN_GAME_1_16 = createJoinGamePacket(MINECRAFT_1_16);
    JOIN_GAME_1_16_2 = createJoinGamePacket(MINECRAFT_1_16_2);
    JOIN_GAME_1_18_2 = createJoinGamePacket(MINECRAFT_1_18_2);
    JOIN_GAME_1_19_1 = createJoinGamePacket(MINECRAFT_1_19_1);
    JOIN_GAME_1_19_4 = createJoinGamePacket(MINECRAFT_1_19_4);
    JOIN_GAME_1_20 = createJoinGamePacket(MINECRAFT_1_20);

    MAX_MOVEMENT_TICK = Sonar.get().getConfig().MAXIMUM_MOVEMENT_TICKS;
    PREPARED_MOVEMENT_PACKETS = new double[MAX_MOVEMENT_TICK + 1];

    for (int i = 0; i < MAX_MOVEMENT_TICK + 1; i++) {
      PREPARED_MOVEMENT_PACKETS[i] = -((Math.pow(0.98, i) - 1) * 3.92);
    }

    // Adjust block and collide Y position based on max movement ticks
    double maxFallDistance = 0;
    for (final double motion : PREPARED_MOVEMENT_PACKETS) {
      maxFallDistance += motion;
    }

    // Set the dynamic spawn buffer
    DYNAMIC_SPAWN_BUFFER = (int) (SPAWN_BUFFER + maxFallDistance);
    // Set the dynamic block and collide Y position based on the maximum fall distance
    DYNAMIC_SPAWN_Y_POSITION = DEFAULT_Y_COLLIDE_POSITION + DYNAMIC_SPAWN_BUFFER + (int) maxFallDistance;

    // Prepare spawn PositionLook with the dynamic Y position
    SPAWN_TELEPORT = new PositionLook(
      8, DYNAMIC_SPAWN_Y_POSITION, 8,
      0f, 0f,
      RANDOM.nextInt(Short.MAX_VALUE),
      true
    );

    // Prepare collision platform positions
    int index = 0;
    for (int x = 0; x < BLOCKS_PER_ROW; x++) {
      for (int z = 0; z < BLOCKS_PER_ROW; z++) {
        final BlockPosition position = new BlockPosition(
          x + (BLOCKS_PER_ROW / 2),
          DEFAULT_Y_COLLIDE_POSITION,
          z + (BLOCKS_PER_ROW / 2),
          0, 0
        );
        CHANGED_BLOCKS[index++] = new ChangedBlock(position, BlockType.STONE);
      }
    }

    // Prepare UpdateSectionBlocks packet
    UPDATE_SECTION_BLOCKS = new UpdateSectionBlocks(0, 0, CHANGED_BLOCKS);
  }

  private @Nullable CompoundBinaryTag getMapping(final @NotNull String fileName) {
    try (final InputStream inputStream = Sonar.class.getResourceAsStream("/mappings/" + fileName)) {
      return BinaryTagIO.unlimitedReader().read(Objects.requireNonNull(inputStream),
        BinaryTagIO.Compression.GZIP
      );
    } catch (Throwable throwable) {
      Sonar.get().getLogger().error("Could not load mapping {}", fileName);
      throwable.printStackTrace();
      return null;
    }
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
    joinGame.setGamemode(Sonar.get().getConfig().GAMEMODE_ID);
    joinGame.setDimension(0);
    joinGame.setReducedDebugInfo(true);
    return joinGame;
  }

  // Partially taken from
  // https://github.com/Elytrium/LimboAPI/blob/master/plugin/src/main/java/net/elytrium/limboapi/server/LimboImpl.java#L607
  private @NotNull JoinGame createJoinGamePacket(final ProtocolVersion protocolVersion) {
    final JoinGame joinGame = new JoinGame();

    joinGame.setLevelType("flat");
    joinGame.setGamemode(Sonar.get().getConfig().GAMEMODE_ID);
    joinGame.setDimension(0);
    joinGame.setReducedDebugInfo(true);
    joinGame.setDimensionInfo(DIMENSION);

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
        .putInt("sky_color", 7907327)
        .putInt("fog_color", 12638463)
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

      joinGame.setCurrentDimensionData(currentDimensionData);
      joinGame.setLevelNames(new String[]{"minecraft:overworld"});
      joinGame.setRegistry(registryContainer.build());
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
    }
    return joinGame;
  }

  public @NotNull CompoundBinaryTag createDimensionData(final @NotNull ProtocolVersion version) {
    final CompoundBinaryTag details = CompoundBinaryTag.builder()
      .putBoolean("natural", false)
      .putFloat("ambient_light", 0f)
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
      .putString("effects", "minecraft:overworld")
      .putInt("min_y", 0)
      .putInt("height", 256)
      .putInt("monster_spawn_block_light_limit", 0)
      .putInt("monster_spawn_light_level", 0)
      .build();

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      return CompoundBinaryTag.builder()
        .putString("name", "minecraft:overworld")
        .putInt("id", 0)
        .put("element", details)
        .build();
    }

    return details.putString("name", "minecraft:overworld");
  }
}
