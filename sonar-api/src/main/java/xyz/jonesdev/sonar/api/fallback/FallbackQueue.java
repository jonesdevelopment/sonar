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

package xyz.jonesdev.sonar.api.fallback;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.jonesdev.sonar.api.Sonar;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class FallbackQueue {
  // Fixed thread pool executor for all new verifications
  private static final int THREAD_POOL_SIZE = Math.min(0x7fff, Runtime.getRuntime().availableProcessors() * 2);
  private static final ExecutorService QUEUE_EXECUTOR = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

  private final ConcurrentMap<InetAddress, Runnable> players = new ConcurrentHashMap<>(
    128, 0.75f, Runtime.getRuntime().availableProcessors());

  public void poll() {
    final int maxQueuePolls = Sonar.get().getConfig().getQueue().getMaxQueuePolls();
    int index = 0;

    // Iterate through the map and process up to maxQueuePolls entries
    // We need to be cautious here since we don't want any concurrency issues
    final Iterator<Map.Entry<InetAddress, Runnable>> iterator = players.entrySet().iterator();
    while (iterator.hasNext() && index++ < maxQueuePolls) {
      // Run the cached runnable
      final Map.Entry<InetAddress, Runnable> entry = iterator.next();
      QUEUE_EXECUTOR.execute(entry.getValue());
      // Remove runnable from the map
      iterator.remove();
    }
  }
}
