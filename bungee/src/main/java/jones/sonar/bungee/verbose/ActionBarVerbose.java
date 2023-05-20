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

@RequiredArgsConstructor
public final class ActionBarVerbose implements Verbose {
  private final ProxyServer server;
  @Getter
  private final Collection<String> subscribers = new ArrayList<>();

  public void update() {
    final TextComponent component = new TextComponent("§e§lSonar §7> §f" + VerboseAnimation.nextState());

    for (final String subscriber : subscribers) {
      Optional.ofNullable(server.getPlayer(subscriber)).ifPresent(player -> {
        player.sendMessage(ChatMessageType.ACTION_BAR, component);
      });
    }
  }
}
