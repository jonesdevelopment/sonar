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

  // https://stackoverflow.com/questions/2015463/how-to-view-the-current-heap-size-that-an-application-is-using
  public String formatMemory(final long m) {
    if (m < 1024) return m + " B";
    final int z = (63 - Long.numberOfLeadingZeros(m)) / 10;
    final String formatted = Sonar.DECIMAL_FORMAT.format((double) m / (1L << (z * 10)));
    return String.format("%s %sB", formatted, " KMGTPE".charAt(z));
  }
}
