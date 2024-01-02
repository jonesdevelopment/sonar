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

package xyz.jonesdev.sonar.common.fallback.protocol.block;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class ChangedBlock {
  private final @NotNull BlockPosition position;
  private final BlockType type;
  private final int legacyBlockState, blockState;

  public ChangedBlock(final @NotNull BlockPosition position, final BlockType type) {
    this.position = position;
    this.type = type;
    final int x = position.getX() - (position.getChunkX() << 4);
    final int y = position.getY();
    final int z = position.getZ() - (position.getChunkZ() << 4);
    this.legacyBlockState = x << 12 | z << 8 | y;
    this.blockState = x << 8 | z << 4 | y - ((y >> 4) << 4);
  }
}
