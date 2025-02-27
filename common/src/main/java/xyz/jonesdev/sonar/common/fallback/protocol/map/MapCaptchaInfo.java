/*
 * Copyright (C) 2025 Sonar Contributors
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
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.MapDataPacket;

@Getter
public final class MapCaptchaInfo {
  private final String answer;

  private final FallbackPacket[] legacy;
  private final FallbackPacket modern;

  public MapCaptchaInfo(final @NotNull String answer, final byte @NotNull [] buffer) {
    this.answer = answer;

    // Prepare 1.7 map data using a grid
    final byte[][] grid = new byte[128][128];
    for (int i = 0; i < buffer.length; i++) {
      grid[i & Byte.MAX_VALUE][i >> 7] = buffer[i];
    }
    this.legacy = new FallbackPacket[grid.length];
    for (int i = 0; i < grid.length; i++) {
      this.legacy[i] = new MapDataPacket(grid[i], i, 0, 0, true);
    }

    // Prepare 1.8+ map data
    this.modern = new MapDataPacket(buffer, 0, 0, 0, true);
  }

  public void delayedWrite(final @NotNull FallbackUser user) {
    if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      // 1.7.2-1.7.10 needs separate packets for each axis
      for (final FallbackPacket packet : legacy) {
        user.delayedWrite(packet);
      }
      return;
    }
    user.delayedWrite(modern);
  }
}
