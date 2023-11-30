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

package xyz.jonesdev.sonar.common.fallback.protocol.map;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.MapData;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;

@Getter
public final class PreparedMapInfo {
  public static final int DIMENSIONS = (int) Math.pow(2, 7);
  public static final int SCALE = DIMENSIONS * DIMENSIONS;

  private final MapInfo info;
  private final MapData[] legacy;
  private final MapData modern;

  public PreparedMapInfo(final String answer,
                         final int columns, final int rows,
                         final byte @NotNull [] buffer) {
    this.info = new MapInfo(answer, columns, rows, 0, 0, buffer);

    // Prepare 1.7 map data using a grid
    final byte[][] grid = new byte[DIMENSIONS][DIMENSIONS];
    for (int i = 0; i < buffer.length; i++) {
      final byte buf = buffer[i];
      grid[i & Byte.MAX_VALUE][i >> 7] = buf;
    }
    this.legacy = new MapData[grid.length];
    for (int i = 0; i < grid.length; i++) {
      this.legacy[i] = new MapData(new MapInfo(answer, DIMENSIONS, DIMENSIONS, i, 0, grid[i]));
    }

    // Prepare 1.8+ map data
    this.modern = new MapData(info);
  }

  public void write(final @NotNull FallbackUser<?, ?> user) {
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
      for (final MapData data : legacy) {
        user.delayedWrite(data);
      }
    } else {
      user.delayedWrite(modern);
    }
  }
}
