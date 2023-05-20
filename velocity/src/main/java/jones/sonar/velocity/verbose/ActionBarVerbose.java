/*
 * Copyright (C) 2023, jones
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

package jones.sonar.velocity.verbose;

import com.velocitypowered.api.proxy.ProxyServer;
import jones.sonar.api.Sonar;
import jones.sonar.api.verbose.Verbose;
import jones.sonar.common.verbose.VerboseAnimation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor
public final class ActionBarVerbose implements Verbose {
  private final ProxyServer server;
  @Getter
  private final Collection<String> subscribers = new ArrayList<>();
  private static final DecimalFormat decimalFormat = new DecimalFormat("#,###");

  public void update() {
    final Component component = Component.text(Sonar.get().getConfig().ACTION_BAR_LAYOUT
      .replace("%queued%", decimalFormat.format(Sonar.get().getFallback().getQueue().getQueuedPlayers().size()))
      .replace("%verifying%", decimalFormat.format(Sonar.get().getFallback().getConnected().size()))
      .replace("%blacklisted%", decimalFormat.format(Sonar.get().getFallback().getBlacklisted().size()))
      .replace("%animation%", VerboseAnimation.nextState())
    );

    synchronized (subscribers) {
      for (final String subscriber : subscribers) {
        server.getPlayer(subscriber).ifPresent(player -> {
          player.sendActionBar(component);
        });
      }
    }
  }
}
