/*
 * Copyright (C) 2023 jones
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

package jones.sonar.api.statistics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface Statistics {
  @NotNull Map<String, Integer> getRawMap();

  default void increment(final String key) {
    increment(key, 0);
  }

  default void increment(final String key, final int fallback) {
    set(key, get(key, fallback) + 1);
  }

  default int get(final String key, final int fallback) {
    return getRawMap().getOrDefault(key, fallback);
  }

  default void set(final String key, final int value) {
    if (!has(key)) getRawMap().put(key, value);
    else getRawMap().replace(key, value);
  }

  default void reset(final String key) {
    set(key, 0);
  }

  default void remove(final String key) {
    getRawMap().remove(key);
  }

  default boolean has(final String key) {
    return getRawMap().containsKey(key);
  }
}
