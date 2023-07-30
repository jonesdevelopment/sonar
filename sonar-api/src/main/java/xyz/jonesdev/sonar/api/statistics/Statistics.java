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

package xyz.jonesdev.sonar.api.statistics;

public enum Statistics {
  TOTAL_TRAFFIC,
  REAL_TRAFFIC,
  FAILED_VERIFICATIONS;

  private int val = -1;

  public void increment() {
    increment(0);
  }

  public void increment(final int def) {
    set(get(def) + 1);
  }

  public int get(final int def) {
    return val == -1 ? def : val;
  }

  public void set(final int value) {
    val = value;
  }

  public void reset() {
    val = 0;
  }
}
