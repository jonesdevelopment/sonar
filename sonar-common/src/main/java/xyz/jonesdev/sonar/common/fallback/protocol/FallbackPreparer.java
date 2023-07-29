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
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockPosition;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockType;
import xyz.jonesdev.sonar.common.fallback.protocol.block.ChangedBlock;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.Abilities;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.EmptyChunkData;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.PositionLook;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.UpdateSectionBlocks;

import java.util.Random;

@UtilityClass
public class FallbackPreparer {
  private final Random random = new Random();

  private final int BLOCKS_PER_ROW = 8; // 8 * 8 = 64 (max)

  public final Abilities DEFAULT_ABILITIES = new Abilities((byte) 0, 0f, 0f);
  public final EmptyChunkData EMPTY_CHUNK_DATA = new EmptyChunkData(0, 0);
  public UpdateSectionBlocks UPDATE_SECTION_BLOCKS;
  private final ChangedBlock[] CHANGED_BLOCKS = new ChangedBlock[BLOCKS_PER_ROW * BLOCKS_PER_ROW];

  public int MAX_MOVEMENT_TICK = 8;
  public double[] PREPARED_MOVEMENT_PACKETS;

  private final int SPAWN_BUFFER = 10; // player spawns at 255 + 10 (10 blocks above the platform)
  public final int DEFAULT_Y_COLLIDE_POSITION = 255; // 255 is max
  public int DYNAMIC_BLOCK_Y_POSITION = DEFAULT_Y_COLLIDE_POSITION + SPAWN_BUFFER;
  public PositionLook SPAWN_TELEPORT;

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
}
