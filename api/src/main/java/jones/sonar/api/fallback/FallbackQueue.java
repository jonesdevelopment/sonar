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

package jones.sonar.api.fallback;

import jones.sonar.api.Sonar;
import jones.sonar.api.util.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class FallbackQueue {
  @Getter
  private final List<Pair<InetAddress, Runnable>> queuedPlayers = new Vector<>();

  public void queue(final InetAddress inetAddress, final Runnable runnable) {
    queuedPlayers.add(new Pair<>(inetAddress, runnable));
  }

  public void remove(final InetAddress inetAddress) {
    queuedPlayers.removeIf(pair -> pair.getFirst() == inetAddress);
  }

  public void poll() {
    synchronized (queuedPlayers) {
      final Collection<Pair<InetAddress, Runnable>> toRemove = new Vector<>();

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
