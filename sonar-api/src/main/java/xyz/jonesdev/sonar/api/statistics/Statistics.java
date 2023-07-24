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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public interface Statistics {
  Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

  Statistics INSTANCE = new Statistics() {
  };

  default void increment(final String key) {
    increment(key, 0);
  }

  default void increment(final String key, final int fallback) {
    set(key, get(key, fallback) + 1);
  }

  default int get(final String key, final int fallback) {
    return map.getOrDefault(key, fallback);
  }

  default void set(final String key, final int value) {
    if (!has(key)) map.put(key, value);
    else map.replace(key, value);
  }

  default void reset(final String key) {
    set(key, 0);
  }

  default void remove(final String key) {
    map.remove(key);
  }

  default boolean has(final String key) {
    return map.containsKey(key);
  }
}
