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
import xyz.jonesdev.sonar.common.fallback.protocol.packets.config.FinishConfiguration;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.config.RegistrySync;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.Abilities;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.EmptyChunkData;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.JoinGame;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.UpdateSectionBlocks;

@UtilityClass
public class FallbackPreparer {

  // Abilities
  public final FallbackPacket DEFAULT_ABILITIES = new Abilities((byte) 0, 0f, 0f);
  // Chunks
  public final FallbackPacket EMPTY_CHUNK_DATA = new EmptyChunkData(0, 0);
  // Finish Configuration
  public final FallbackPacket FINISH_CONFIGURATION = new FinishConfiguration();
  // Synchronize Registry
  public final FallbackPacket REGISTRY_SYNC = new RegistrySync();
  // JoinGame
  public FallbackPacket joinGame;
  // Update Section Blocks
  public FallbackPacket updateSectionBlocks;

  // Collisions
  public final int BLOCKS_PER_ROW = 8; // 8 * 8 = 64 (max)
  private final int SPAWN_BUFFER = 5; // player spawns at 255 + 5 (5 blocks above the platform)
  public final int SPAWN_X_POSITION = BLOCKS_PER_ROW; // middle of the chunk
  public final int SPAWN_Z_POSITION = BLOCKS_PER_ROW;
  public final int DEFAULT_Y_COLLIDE_POSITION = 255; // 255 is the maximum Y position allowed

  private final ChangedBlock[] CHANGED_BLOCKS = new ChangedBlock[BLOCKS_PER_ROW * BLOCKS_PER_ROW];

  public int maxMovementTick, dynamicSpawnYPosition;
  public double[] preparedCachedYMotions;
  public double maxFallDistance;

  public void prepare() {
    joinGame = new JoinGame(5,
      Sonar.get().getConfig().getVerification().getGamemodeId(),
      0,
      false,
      0,
      true,
      true,
      false,
      new String[]{"minecraft:overworld"},
      "minecraft:overworld");

    maxFallDistance = 0;
    maxMovementTick = Sonar.get().getConfig().getVerification().getMaxMovementTicks();
    preparedCachedYMotions = new double[maxMovementTick + 10];

    for (int i = 0; i < preparedCachedYMotions.length; i++) {
      final double gravity = -((Math.pow(0.98, i) - 1) * 3.92);
      preparedCachedYMotions[i] = gravity;
      if (i <= maxMovementTick) {
        maxFallDistance += gravity;
      }
    }

    // Set the dynamic block and collide Y position based on the maximum fall distance
    dynamicSpawnYPosition = DEFAULT_Y_COLLIDE_POSITION + 3 + (int) Math.ceil(maxFallDistance);
    System.out.println(dynamicSpawnYPosition);

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
        CHANGED_BLOCKS[index++] = new ChangedBlock(position, BlockType.BARRIER);
      }
    }

    // Prepare UpdateSectionBlocks packet
    updateSectionBlocks = new UpdateSectionBlocks(0, 0, CHANGED_BLOCKS);
  }
}
