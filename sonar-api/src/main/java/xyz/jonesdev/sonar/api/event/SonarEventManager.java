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

package xyz.jonesdev.sonar.api.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SonarEventManager {
  public static final SonarEventManager INSTANCE = new SonarEventManager();
  private static final Collection<SonarEventListener> EVENT_LISTENERS = new Vector<>(0);

  /**
   * Publishes the given event to all listeners
   */
  @ApiStatus.Internal
  public void publish(final @NotNull SonarEvent event) {
    for (final SonarEventListener eventListener : EVENT_LISTENERS) {
      eventListener.handle(event);
    }
  }

  /**
   * Registers one (or more) event listeners
   *
   * @param listeners One (or more) listeners to register
   * @see #unregisterListener(SonarEventListener...) Unregister an event listener
   */
  public synchronized void registerListener(final @NotNull SonarEventListener... listeners) {
    EVENT_LISTENERS.addAll(Arrays.asList(listeners));
  }

  /**
   * Unregisters one (or more) event listeners
   *
   * @param listeners One (or more) listeners to unregister
   * @apiNote This does not have any effect if the given listeners are not registered
   * @see #registerListener(SonarEventListener...) Register an event listener
   */
  public synchronized void unregisterListener(final @NotNull SonarEventListener... listeners) {
    EVENT_LISTENERS.removeAll(Arrays.asList(listeners));
  }
}
