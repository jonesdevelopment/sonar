/*
 * Copyright (C) 2023, jones
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

package jones.sonar.api.fallback

import jones.sonar.api.Sonar
import lombok.AccessLevel
import lombok.RequiredArgsConstructor
import java.net.InetAddress
import java.util.*

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class FallbackQueue {
  private val queuedPlayers = Vector<Pair<InetAddress, Runnable>>(8) // pre-allocate 8 entries

  fun getQueuedPlayers(): Collection<Pair<InetAddress, Runnable>> {
    return queuedPlayers // TODO: sync here?
  }

  /**
   * Creates a pair of InetAddress and Runnable (action to be run after the queue entry
   * has been polled) since we cannot store the Runnable for the InetAddress here because
   * of concurrency and accessibility issues.
   *
   * @param inetAddress IP address of the player
   * @param runnable queued action on the netty thread
   * @see remove
   */
  fun queue(inetAddress: InetAddress, runnable: Runnable) {
    queuedPlayers.add(Pair(inetAddress, runnable))
  }

  fun remove(inetAddress: InetAddress) {
    // Since every entry is a Pair<>, we cannot just remove an InetAddress from the map.
    // Therefore, we check for each pair and see if the InetAddress matches the one that
    // has to be removed. Furthermore, we remove the entry (Pair<>)
    queuedPlayers.removeIf { pair: Pair<InetAddress, Runnable> -> pair.first === inetAddress }
  }

  fun poll() {
    val toRemove = Vector<Pair<InetAddress, Runnable>>()

    // We need to be very careful here since we don't want any concurrency issues.
    synchronized(queuedPlayers) {
      queuedPlayers.parallelStream()
        .limit(Sonar.get().config.MAXIMUM_QUEUE_POLLS.toLong())
        .forEach {
          it.second.run()
          toRemove.add(it)
        }

      queuedPlayers.removeAll(toRemove.toSet())
    }
  }
}
