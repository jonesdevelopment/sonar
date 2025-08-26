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

package xyz.jonesdev.sonar.bukkit.fallback;

import lombok.Getter;

@Getter
public enum BukkitServerVersion {
  MINECRAFT_1_7_10,
  MINECRAFT_1_8,
  MINECRAFT_1_8_3,
  MINECRAFT_1_8_8,
  MINECRAFT_1_9,
  MINECRAFT_1_9_2,
  MINECRAFT_1_9_4,
  MINECRAFT_1_10,
  MINECRAFT_1_10_1,
  MINECRAFT_1_10_2,
  MINECRAFT_1_11,
  MINECRAFT_1_11_2,
  MINECRAFT_1_12,
  MINECRAFT_1_12_1,
  MINECRAFT_1_12_2,
  MINECRAFT_1_13,
  MINECRAFT_1_13_1,
  MINECRAFT_1_13_2,
  MINECRAFT_1_14,
  MINECRAFT_1_14_1,
  MINECRAFT_1_14_2,
  MINECRAFT_1_14_3,
  MINECRAFT_1_14_4,
  MINECRAFT_1_15,
  MINECRAFT_1_15_1,
  MINECRAFT_1_15_2,
  MINECRAFT_1_16,
  MINECRAFT_1_16_1,
  MINECRAFT_1_16_2,
  MINECRAFT_1_16_3,
  MINECRAFT_1_16_4,
  MINECRAFT_1_16_5,
  MINECRAFT_1_17,
  MINECRAFT_1_17_1,
  MINECRAFT_1_18,
  MINECRAFT_1_18_1,
  MINECRAFT_1_18_2,
  MINECRAFT_1_19,
  MINECRAFT_1_19_1,
  MINECRAFT_1_19_2,
  MINECRAFT_1_19_3,
  MINECRAFT_1_19_4,
  MINECRAFT_1_20,
  MINECRAFT_1_20_1,
  MINECRAFT_1_20_2,
  MINECRAFT_1_20_3,
  MINECRAFT_1_20_4,
  MINECRAFT_1_20_5,
  MINECRAFT_1_20_6,
  MINECRAFT_1_21,
  MINECRAFT_1_21_1,
  MINECRAFT_1_21_2,
  MINECRAFT_1_21_3,
  MINECRAFT_1_21_4,
  MINECRAFT_1_21_5,
  MINECRAFT_1_21_6,
  MINECRAFT_1_21_7,
  MINECRAFT_1_21_8;

  static final BukkitServerVersion[] REVERSED_VALUES;

  static {
    REVERSED_VALUES = values().clone();

    final int size = REVERSED_VALUES.length;
    final int half = size / 2;

    for (int i = 0; i < half; i++) {
      final BukkitServerVersion temp = REVERSED_VALUES[i];
      REVERSED_VALUES[i] = REVERSED_VALUES[size - 1 - i];
      REVERSED_VALUES[size - 1 - i] = temp;
    }
  }

  private final String release;

  BukkitServerVersion() {
    this.release = name().substring(10).replace("_", ".");
  }
}
