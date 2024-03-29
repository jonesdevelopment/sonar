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

package xyz.jonesdev.sonar.velocity.fallback;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.velocity.SonarVelocity;

import java.net.InetAddress;
import java.util.Objects;

public final class FallbackLoginListener {

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull PreLoginEvent event) {
    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();
    // Don't do anything if the setting is disabled
    if (maxOnlinePerIp <= 0) return;

    final InetAddress inetAddress = event.getConnection().getRemoteAddress().getAddress();

    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final long onlinePerIp = SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
      .filter(player -> Objects.equals(player.getRemoteAddress().getAddress(), inetAddress))
      .count();

    // We use '>=' because the player connecting to the server hasn't joined yet
    if (onlinePerIp >= maxOnlinePerIp) {
      event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Sonar.get().getConfig().getTooManyOnlinePerIp()));
    }
  }
}
