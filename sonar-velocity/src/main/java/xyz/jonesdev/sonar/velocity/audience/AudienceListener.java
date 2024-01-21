/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.velocity.audience;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;

public final class AudienceListener {

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull PostLoginEvent event) {
    Sonar.AUDIENCES.put(event.getPlayer().getUsername(), event.getPlayer());
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull DisconnectEvent event) {
    Sonar.AUDIENCES.remove(event.getPlayer().getUsername());
  }
}
