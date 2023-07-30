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

package xyz.jonesdev.sonar.api.format;

import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.Sonar;

@UtilityClass
public class MemoryFormatter {
  private final int MIN = 1024;

  public String formatMemory(long memory) {
    if (memory < MIN) {
      return memory + "B";
    }

    memory /= MIN; // KB
    String suffix = "KB";

    if (memory >= MIN) {
      suffix = "MB";
      memory /= MIN;
    }

    if (memory >= MIN) {
      suffix = "GB";
      memory /= MIN;
    }
    return Sonar.DECIMAL_FORMAT.format(memory) + suffix;
  }
}
