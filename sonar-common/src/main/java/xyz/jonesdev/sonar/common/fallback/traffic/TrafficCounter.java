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

package xyz.jonesdev.sonar.common.fallback.traffic;

import lombok.Getter;
import lombok.Setter;

import static xyz.jonesdev.sonar.api.format.MemoryFormatter.formatMemory;

/**
 * This counts all incoming and outgoing traffic new players
 * who need to be verified are sending to the server.
 */
@Getter
public enum TrafficCounter {
  INCOMING,
  OUTGOING;

  @Setter
  private long ttl, curr;
  private String cachedSecond = "-", cachedTtl = "-";

  public void increment(final long b) {
    curr += b;
  }

  public static synchronized void reset() {
    for (final TrafficCounter value : values()) {
      value.ttl += value.curr; // increment total
      value.cachedSecond = formatMemory(value.curr);
      value.cachedTtl = formatMemory(value.ttl);
      value.curr = 0L; // reset current
    }
  }
}
