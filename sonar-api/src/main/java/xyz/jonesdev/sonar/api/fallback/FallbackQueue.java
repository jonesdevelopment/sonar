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
import xyz.jonesdev.sonar.api.list.Pair;

import java.net.InetAddress;
import java.util.List;
import java.util.Vector;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class FallbackQueue {
  @Getter
  private final List<Pair<InetAddress, Runnable>> queuedPlayers = new Vector<>(8);

  /**
   * Creates a pair of InetAddress and Runnable (action to be run after the queue entry
   * has been polled) since we cannot store the Runnable for the InetAddress here because
   * of concurrency and accessibility issues.
   *
   * @param inetAddress IP address of the player
   * @param runnable queued action on the netty thread
   * @see #remove
   */
  public void queue(final InetAddress inetAddress, final Runnable runnable) {
    queuedPlayers.add(new Pair<>(inetAddress, runnable));
  }

  /**
   * Since every entry is a Pair<>, we cannot just remove an InetAddress from the map.
   * Therefore, we check for each pair and see if the InetAddress matches the one that
   * has to be removed.
   * Furthermore, we remove the entry (Pair<>)
   *
   * @param inetAddress IP address of the player
   */
  public void remove(final InetAddress inetAddress) {
    queuedPlayers.removeIf(pair -> pair.getFirst() == inetAddress);
  }

  public void poll() {
    final Vector<Pair<InetAddress, Runnable>> toRemove = new Vector<>();

    // We need to be very careful here since we don't want any concurrency issues.
    synchronized (queuedPlayers) {
      queuedPlayers.parallelStream()
        .limit(Sonar.get().getConfig().MAXIMUM_QUEUE_POLLS)
        .forEach(pair -> {
          pair.getSecond().run();
          toRemove.add(pair);
        });

      queuedPlayers.removeAll(toRemove);
    }
  }
}
