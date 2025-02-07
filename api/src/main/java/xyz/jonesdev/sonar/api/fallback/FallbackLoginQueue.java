/*
 * Copyright (C) 2025 Sonar Contributors
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class FallbackLoginQueue {
  // Fixed thread pool executor for all new verifications
  private static final int THREAD_POOL_SIZE = Math.min(0x7fff, Runtime.getRuntime().availableProcessors() * 2);
  private static final ExecutorService QUEUE_EXECUTOR = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

  private final ConcurrentMap<InetAddress, Runnable> players = new ConcurrentHashMap<>(512);

  public void poll() {
    final int maxQueuePolls = Math.min(players.size(), Sonar.get0().getConfig().getQueue().getMaxQueuePolls());
    // No need to initiate an executor service task if nobody is currently queued
    if (maxQueuePolls <= 0) return;
    // We need to be cautious here since we don't want any concurrency issues
    QUEUE_EXECUTOR.execute(() -> {
      final var iterator = players.entrySet().iterator();
      for (int index = 0; index < maxQueuePolls && iterator.hasNext(); index++) {
        iterator.next().getValue().run();
        iterator.remove();
      }
    });
  }
}
