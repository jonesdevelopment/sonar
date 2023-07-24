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

package xyz.jonesdev.sonar.api.format

import xyz.jonesdev.sonar.api.Sonar

class MemoryFormatter {
  companion object {
    private const val MIN = 1024

    fun formatMemory(memory: Long): String {
      var bytes = memory

      if (bytes < MIN) {
        return "$bytes B"
      }

      bytes /= MIN // KB
      var suffix = "KB"

      if (bytes >= MIN) {
        suffix = "MB"
        bytes /= MIN
      }

      if (bytes >= MIN) {
        suffix = "GB"
        bytes /= MIN
      }
      return Sonar.get().formatter.format(bytes) + suffix
    }
  }
}
