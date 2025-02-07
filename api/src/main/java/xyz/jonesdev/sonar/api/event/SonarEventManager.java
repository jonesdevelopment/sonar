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

package xyz.jonesdev.sonar.api.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SonarEventManager {
  public static final SonarEventManager INSTANCE = new SonarEventManager();
  private static final Collection<SonarEventListener> EVENT_LISTENERS = new Vector<>(0);
  private static final ExecutorService EVENT_SERVICE = Executors.newSingleThreadExecutor();

  @ApiStatus.Internal
  public void publish(final @NotNull SonarEvent event) {
    // Don't post an event if there are no listeners
    if (EVENT_LISTENERS.isEmpty()) {
      return;
    }

    EVENT_SERVICE.execute(() -> {
      for (final SonarEventListener eventListener : EVENT_LISTENERS) {
        try {
          eventListener.handle(event);
        } catch (Throwable throwable) {
          Sonar.get0().getLogger().error("Could not pass {} to listener: {}",
            event.getClass().getSimpleName(), throwable);
        }
      }
    });
  }

  /**
   * Registers one (or more) event listeners
   *
   * @param listeners One (or more) listeners to register
   * @see #unregisterListener(SonarEventListener[]) Unregister an event listener
   */
  @SuppressWarnings("unused") // External API usage
  public void registerListener(final @NotNull SonarEventListener... listeners) {
    synchronized (EVENT_LISTENERS) {
      EVENT_LISTENERS.addAll(Arrays.asList(listeners));
    }
  }

  /**
   * Unregisters one (or more) event listeners
   *
   * @param listeners One (or more) listeners to unregister
   * @apiNote This does not have any effect if the given listeners are not registered
   * @see #registerListener(SonarEventListener[]) Register an event listener
   */
  @SuppressWarnings("unused") // External API usage
  public void unregisterListener(final @NotNull SonarEventListener... listeners) {
    synchronized (EVENT_LISTENERS) {
      EVENT_LISTENERS.removeAll(Arrays.asList(listeners));
    }
  }
}
