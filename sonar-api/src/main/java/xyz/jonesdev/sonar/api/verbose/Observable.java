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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.Vector;

@Getter
public abstract class Observable {
  protected final @NotNull Collection<UUID> subscribers = new Vector<>(0);

  public abstract void observe();

  /**
   * @param uuid UUID of the player
   * @return Whether the audience is subscribed or not
   */
  public final boolean isSubscribed(final @NotNull UUID uuid) {
    return getSubscribers().contains(uuid);
  }

  /**
   * @param uuid UUID of the player
   */
  public final void subscribe(final UUID uuid) {
    getSubscribers().add(uuid);
  }

  /**
   * @param uuid UUID of the player
   */
  public final void unsubscribe(final UUID uuid) {
    getSubscribers().remove(uuid);
  }
}
