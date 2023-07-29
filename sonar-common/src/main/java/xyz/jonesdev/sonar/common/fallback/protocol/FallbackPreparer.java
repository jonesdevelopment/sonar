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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockPosition;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockType;
import xyz.jonesdev.sonar.common.fallback.protocol.block.ChangedBlock;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.Abilities;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.EmptyChunkData;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.PositionLook;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.UpdateSectionBlocks;

import java.io.InputStream;
import java.util.Objects;
import java.util.Random;

@UtilityClass
public class FallbackPreparer {
  private final Random random = new Random();

  // Mappings
  public final CompoundBinaryTag CHAT_TYPE_119;
  public final CompoundBinaryTag CHAT_TYPE_1191;
  public final CompoundBinaryTag DAMAGE_TYPE_1194;
  public final CompoundBinaryTag DAMAGE_TYPE_120;

  static {
    CHAT_TYPE_119 = getMapping("chat_1_19.nbt");
    CHAT_TYPE_1191 = getMapping("chat_1_19_1.nbt");
    DAMAGE_TYPE_1194 = getMapping("damage_1_19_4.nbt");
    DAMAGE_TYPE_120 = getMapping("damage_type_1_20.nbt");
  }

  // Abilities
  public final Abilities DEFAULT_ABILITIES = new Abilities((byte) 0, 0f, 0f);

  // Chunks
  public final EmptyChunkData EMPTY_CHUNK_DATA = new EmptyChunkData(0, 0);
  public PositionLook SPAWN_TELEPORT;

  // Collisions
  private final int BLOCKS_PER_ROW = 8; // 8 * 8 = 64 (max)
  private final int SPAWN_BUFFER = 10; // player spawns at 255 + 10 (10 blocks above the platform)
  public final int DEFAULT_Y_COLLIDE_POSITION = 255; // 255 is max

  public UpdateSectionBlocks UPDATE_SECTION_BLOCKS;
  private final ChangedBlock[] CHANGED_BLOCKS = new ChangedBlock[BLOCKS_PER_ROW * BLOCKS_PER_ROW];

  public int MAX_MOVEMENT_TICK;
  public double[] PREPARED_MOVEMENT_PACKETS;
  public int DYNAMIC_BLOCK_Y_POSITION;

  public void prepare() {
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

    // Set the dynamic block and collide Y position based on the maximum fall distance
    DYNAMIC_BLOCK_Y_POSITION = DEFAULT_Y_COLLIDE_POSITION + SPAWN_BUFFER + (int) maxFallDistance;

    // Prepare spawn PositionLook with the dynamic Y position
    SPAWN_TELEPORT = new PositionLook(
      8, DYNAMIC_BLOCK_Y_POSITION, 8,
      0f, 0f,
      random.nextInt(Short.MAX_VALUE),
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
      .putString("effects", Sonar.get().getConfig().DIMENSION_KEY)
      .putInt("min_y", 0)
      .putInt("height", 256)
      .putInt("monster_spawn_block_light_limit", 0)
      .putInt("monster_spawn_light_level", 0)
      .build();

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      return CompoundBinaryTag.builder()
        .putString("name", Sonar.get().getConfig().DIMENSION_KEY)
        .putInt("id", Sonar.get().getConfig().DIMENSION_MODERN_ID)
        .put("element", details)
        .build();
    }

    return details.putString("name", Sonar.get().getConfig().DIMENSION_KEY);
  }
}
