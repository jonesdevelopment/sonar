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

package xyz.jonesdev.sonar.api.timer;

import lombok.Getter;

import java.text.SimpleDateFormat;

@Getter
public final class SystemTimer {
  public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("mm:ss");

  private long start = System.currentTimeMillis();

  public void reset() {
    start = System.currentTimeMillis();
  }

  /**
   * @return Time between the current timestamp and the start timestamp
   */
  public long delay() {
    return System.currentTimeMillis() - start;
  }

  /**
   * @apiNote Read more: {@link SystemTimer#delay}
   */
  public boolean elapsed(final long amount) {
    return delay() >= amount;
  }

  @Override
  public String toString() {
    return String.format("%.3f", delay() / 1000D);
  }
}
