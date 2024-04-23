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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FallbackQueue {
  static final FallbackQueue INSTANCE = new FallbackQueue();
  private final Map<InetAddress, Runnable> queuedPlayers = new ConcurrentHashMap<>(64);
  // Async executor for all new verifications
  public static final ExecutorService QUEUE_EXECUTOR = new ForkJoinPool(
    Math.min(0x7fff, Runtime.getRuntime().availableProcessors()),
    ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

  public void poll() {
    final int maxQueuePolls = Sonar.get().getConfig().getQueue().getMaxQueuePolls();
    int index = 0;

    // We need to be cautious here since we don't want any concurrency issues.
    final Iterator<Map.Entry<InetAddress, Runnable>> iterator = queuedPlayers.entrySet().iterator();
    while (iterator.hasNext()) {
      // Break if we reached our maximum entries
      if (++index > maxQueuePolls) {
        break;
      }
      // Run the cached runnable
      final Map.Entry<InetAddress, Runnable> entry = iterator.next();
      QUEUE_EXECUTOR.execute(entry.getValue());
      // Remove runnable from iterator
      iterator.remove();
    }
  }
}
