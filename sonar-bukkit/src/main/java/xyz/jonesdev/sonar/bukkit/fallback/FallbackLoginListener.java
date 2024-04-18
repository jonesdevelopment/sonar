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

package xyz.jonesdev.sonar.bukkit.fallback;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.bukkit.SonarBukkit;

import java.util.Objects;

public final class FallbackLoginListener implements Listener {

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(final @NotNull AsyncPlayerPreLoginEvent event) {
    // Don't check player if the login is already denied by another plugin
    // We have to use the deprecated API functions to ensure backwards compatibility
    if (event.getResult() != PlayerPreLoginEvent.Result.ALLOWED) return;

    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();
    // Don't do anything if the setting is disabled
    if (maxOnlinePerIp <= 0) return;

    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final long onlinePerIp = SonarBukkit.INSTANCE.getPlugin().getServer().getOnlinePlayers().stream()
      .filter(player -> Objects.equals(player.getAddress().getAddress(), event.getAddress()))
      .count();

    // We use '>=' because the player connecting to the server hasn't joined yet
    if (onlinePerIp >= maxOnlinePerIp) {
      event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
        LegacyComponentSerializer.legacySection().serialize(Sonar.get().getConfig().getTooManyOnlinePerIp()));
    }
  }
}
