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

package jones.sonar.common.fallback.dimension;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// https://github.com/Elytrium/LimboAPI/blob/91bedd5dad5e659092fbb0a7411bd00d67044d01/api/src/main/java/net/elytrium/limboapi/api/chunk/Dimension.java
@Getter
@RequiredArgsConstructor
public enum PacketDimension {
  OVERWORLD("minecraft:overworld", 0, 0, 28, true), // (384 + 64) / 16
  NETHER("minecraft:the_nether", -1, 1, 16, false), // 256 / 16
  THE_END("minecraft:the_end", 1, 2, 16, false); // 256 / 16

  private final String key;
  private final int legacyID;
  private final int modernID;
  private final int maxSections;
  private final boolean hasLegacySkyLight;
}
