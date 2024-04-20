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

package xyz.jonesdev.sonar.common.statistics;

import lombok.Getter;
import lombok.Setter;
import xyz.jonesdev.sonar.api.jvm.JVMProcessInformation;

/**
 * This counts all incoming and outgoing traffic.
 */
@Getter
public enum CachedBandwidthStatistics {
  INCOMING,
  OUTGOING;

  @Setter
  private long ttl, curr;
  private String cachedSecond = "-", cachedTtl = "-";

  /**
   * Increments the current (per-second) value.
   *
   * @param b Bytes sent
   */
  public void increment(final long b) {
    curr += b;
  }

  /**
   * Increments the total value by the current
   * (per-second) value and resets the current value.
   * This method is called every second.
   */
  public static void reset() {
    INCOMING.cacheAndReset();
    OUTGOING.cacheAndReset();
  }

  private void cacheAndReset() {
    ttl += curr; // increment total
    cachedSecond = JVMProcessInformation.formatMemory(curr);
    cachedTtl = JVMProcessInformation.formatMemory(ttl);
    curr = 0L; // reset current
  }
}
