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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class FallbackQueue {
  @Getter
  private final Map<InetAddress, Runnable> queuedPlayers = new HashMap<>();

  public void queue(final InetAddress inetAddress, final Runnable runnable) {
    queuedPlayers.put(inetAddress, runnable);
  }

  public void poll() {
    synchronized (queuedPlayers) {
      final AtomicInteger index = new AtomicInteger();
      final List<Runnable> execute = new ArrayList<>();

      final int max = Sonar.get().getConfig().MAXIMUM_QUEUE_POLLS;

      queuedPlayers.keySet().removeIf(inetAddress -> {
        if (index.incrementAndGet() > max) {
          return false;
        }

        execute.add(queuedPlayers.get(inetAddress));
        return true;
      });

      execute.forEach(Runnable::run);
    }
  }
}
