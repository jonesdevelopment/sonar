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

  /**
   * Increments the current value by 1 with the default value of 0
   * @see #get(int)
   * @see #set(int)
   */
  public void increment() {
    increment(0);
  }

  /**
   * Increments the current value by 1 with a custom default value
   * @see #get(int)
   * @see #set(int)
   */
  public void increment(final int def) {
    set(get(def) + 1);
  }

  /**
   * @return The current value with the default value of 0
   * @see #get(int)
   */
  public int get() {
    return get(0);
  }

  /**
   * @return The current value with a custom default value
   */
  public int get(final int def) {
    return val == -1 ? def : val;
  }

  /**
   * Set current value to a new value
   */
  public void set(final int value) {
    val = value;
  }

  /**
   * Set current value to 0
   */
  public void reset() {
    val = 0;
  }
}
