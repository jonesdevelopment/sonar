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

package jones.sonar.bungee.verbose;

import jones.sonar.api.Sonar;
import jones.sonar.api.verbose.Verbose;
import jones.sonar.common.verbose.VerboseAnimation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static jones.sonar.api.util.Formatting.formatMemory;

@RequiredArgsConstructor
public final class ActionBarVerbose implements Verbose {
  private final ProxyServer server;
  @Getter
  private final Collection<String> subscribers = new ArrayList<>();

  public void update() {
    final TextComponent component = new TextComponent(Sonar.get().getConfig().ACTION_BAR_LAYOUT
      .replace("%queued%",
        Sonar.get().getFormatter().format(Sonar.get().getFallback().getQueue().getQueuedPlayers().size()))
      .replace("%verifying%", Sonar.get().getFormatter().format(Sonar.get().getFallback().getConnected().size()))
      .replace("%blacklisted%", Sonar.get().getFormatter().format(Sonar.get().getFallback().getBlacklisted().size()))
      .replace("%total%", Sonar.get().getFormatter().format(Sonar.get().getStatistics().get("total", 0)))
      .replace("%used-memory%", formatMemory(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()))
      .replace("%free-memory%", formatMemory(Runtime.getRuntime().freeMemory()))
      .replace("%total-memory%", formatMemory(Runtime.getRuntime().totalMemory()))
      .replace("%max-memory%", formatMemory(Runtime.getRuntime().maxMemory()))
      .replace("%animation%", VerboseAnimation.Companion.nextState())
    );

    synchronized (subscribers) {
      for (final String subscriber : subscribers) {
        Optional.ofNullable(server.getPlayer(subscriber)).ifPresent(player -> {
          player.sendMessage(ChatMessageType.ACTION_BAR, component);
        });
      }
    }
  }
}
