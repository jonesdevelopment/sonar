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

package xyz.jonesdev.sonar.bungee.audience;

import net.kyori.adventure.audience.Audience;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.bungee.SonarBungee;

public final class AudienceListener implements Listener {

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handle(final @NotNull PostLoginEvent event) {
    final Audience audience = SonarBungee.INSTANCE.getBungeeAudiences().player(event.getPlayer());
    Sonar.get().getVerboseHandler().getAudiences().put(event.getPlayer().getName(), audience);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handle(final @NotNull PlayerDisconnectEvent event) {
    Sonar.get().getVerboseHandler().getAudiences().remove(event.getPlayer().getName());
  }
}
