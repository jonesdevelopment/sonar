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

package xyz.jonesdev.sonar.common.fallback.protocol.block;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class ChangedBlock {
  private final @NotNull BlockPosition position;
  private final BlockType type;
  private final int legacyChunkPosCrammed;

  public ChangedBlock(final @NotNull BlockPosition position, final BlockType type) {
    this.position = position;
    this.type = type;
    this.legacyChunkPosCrammed = position.getX()
      - (position.getChunkX() << 4) << 12 | position.getZ()
      - (position.getChunkZ() << 4) << 8 | position.getY();
  }
}
