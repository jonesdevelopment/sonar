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

package xyz.jonesdev.sonar.api.verbose;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface Verbose {
  @NotNull Collection<String> getSubscribers();

  /**
   * @param subscriber Name of the player who subscribed
   * @return Whether the player is subscribed or not
   */
  default boolean isSubscribed(final @NotNull String subscriber) {
    return getSubscribers().contains(subscriber);
  }

  /**
   * @param subscriber Name of the player to subscribe
   */
  default void subscribe(final @NotNull String subscriber) {
    getSubscribers().add(subscriber);
  }

  /**
   * @param subscriber Name of the player to unsubscribe
   */
  default void unsubscribe(final @NotNull String subscriber) {
    getSubscribers().remove(subscriber);
  }
}
