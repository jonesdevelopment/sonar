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

package xyz.jonesdev.sonar.api.fallback;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.jonesdev.sonar.api.Sonar;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FallbackQueue {
  public static final FallbackQueue INSTANCE = new FallbackQueue();
  private final Map<InetAddress, Runnable> queuedPlayers = new ConcurrentHashMap<>(16, 0.5f);

  /**
   * @param inetAddress IP address of the player
   * @param runnable    Queued action on the netty thread
   * @see #remove
   */
  public void queue(final InetAddress inetAddress, final Runnable runnable) {
    queuedPlayers.put(inetAddress, runnable);
  }

  /**
   * @param inetAddress IP address of the player
   */
  public void remove(final InetAddress inetAddress) {
    queuedPlayers.remove(inetAddress);
  }

  public void poll() {
    final int max = Sonar.get().getConfig().getQueue().getMaxQueuePolls();
    int index = 0;

    // We need to be very careful here since we don't want any concurrency issues.
    final Iterator<Map.Entry<InetAddress, Runnable>> iterator = queuedPlayers.entrySet().iterator();
    while (iterator.hasNext()) {
      // Break if we reached our maximum entries
      if (++index > max) {
        break;
      }
      // Run the cached runnable
      final Map.Entry<InetAddress, Runnable> entry = iterator.next();
      entry.getValue().run();
      // Remove runnable from iterator
      iterator.remove();
    }
  }
}
