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

package xyz.jonesdev.sonar.common.fallback.protocol.captcha;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.MapDataPacket;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;

@Getter
public final class MapCaptchaInfo {
  private final String answer;

  private final FallbackPacket[] legacy;
  private final FallbackPacket modern;

  public MapCaptchaInfo(final @NotNull String answer, final int @NotNull [] buffer) {
    this.answer = answer;

    // Prepare 1.7 map data using a grid
    final int[][] grid = new int[128][128];
    for (int i = 0; i < buffer.length; i++) {
      final int buf = buffer[i];
      grid[i & Byte.MAX_VALUE][i >> 7] = buf;
    }
    this.legacy = new FallbackPacket[grid.length];
    for (int i = 0; i < grid.length; i++) {
      this.legacy[i] = new MapDataPacket(grid[i], i, 0);
    }

    // Prepare 1.8+ map data
    this.modern = new MapDataPacket(buffer, 0, 0);
  }

  public void delayedWrite(final @NotNull FallbackUser user) {
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
      // 1.7.2-1.7.10 needs separate packets for each axis
      for (final FallbackPacket legacyPacket : legacy) {
        user.delayedWrite(legacyPacket);
      }
      return;
    }
    // Send modern packet
    user.delayedWrite(modern);
  }
}
