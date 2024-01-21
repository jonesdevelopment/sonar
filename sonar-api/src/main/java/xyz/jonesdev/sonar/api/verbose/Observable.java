/*
 * Copyright (C) 2024 Sonar Contributors
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

public interface Observable {
  Collection<String> getSubscribers();

  /**
   * @param name Name of the audience
   * @return Whether the audience is subscribed or not
   */
  default boolean isSubscribed(final @NotNull String name) {
    return getSubscribers().contains(name);
  }

  /**
   * @param name Name of the audience to subscribe
   */
  default void subscribe(final String name) {
    getSubscribers().add(name);
  }

  /**
   * @param name Name of the audience to unsubscribe
   */
  default void unsubscribe(final String name) {
    getSubscribers().remove(name);
  }
}
