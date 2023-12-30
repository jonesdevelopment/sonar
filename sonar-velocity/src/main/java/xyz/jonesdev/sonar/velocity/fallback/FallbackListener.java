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

package xyz.jonesdev.sonar.velocity.fallback;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.velocity.SonarVelocity;

import java.net.InetAddress;
import java.util.Objects;

@RequiredArgsConstructor
public final class FallbackListener {

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull PreLoginEvent event) {
    final InetAddress inetAddress = event.getConnection().getRemoteAddress().getAddress();

    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();

    if (maxOnlinePerIp > 0) {
      final long onlinePerIp = SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
        .filter(player -> Objects.equals(player.getRemoteAddress().getAddress(), inetAddress))
        .count();

      // We use '>=' because the player connecting to the server hasn't joined yet
      if (onlinePerIp >= maxOnlinePerIp) {
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Sonar.get().getConfig().getTooManyOnlinePerIp()));
      }
    }
  }
}
