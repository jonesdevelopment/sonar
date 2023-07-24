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

package jones.sonar.api.cache;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ExpiringConcurrentHashMap<K> extends ConcurrentHashMap<K, Long> implements ExpiringConcurrentMap<K> {
  @Getter
  private final long duration;

  public ExpiringConcurrentHashMap(final long duration,
                                   final @NotNull TimeUnit timeUnit) {
    this.duration = TimeUnit.MILLISECONDS.convert(duration, timeUnit);
  }

  @Override
  public void put(final K key) {
    put(key, System.currentTimeMillis());
  }

  @Override
  public void invalidate(final K key) {
    remove(key);
  }

  @Override
  public long estimatedSize() {
    cleanUp();
    return size();
  }

  @Override
  public boolean has(final K key) {
    cleanUp();
    return containsKey(key);
  }

  @Override
  public void invalidateAll() {
    clear();
  }

  @Override
  public void cleanUp() {
    final long currentTimestamp = System.currentTimeMillis();

    final Iterator<Entry<K, Long>> iterator = entrySet().iterator();
    while (iterator.hasNext()) {
      final Entry<K, Long> entry = iterator.next();
      final long entryTimestamp = entry.getValue();
      final long delay = currentTimestamp - entryTimestamp;

      // Invalidate entry if duration is over
      if (delay > duration) {
        iterator.remove();
      }
    }
  }
}
