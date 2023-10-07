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

package xyz.jonesdev.sonar.bukkit.verbose;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import xyz.jonesdev.sonar.api.verbose.Verbose;

@RequiredArgsConstructor
public final class ActionBarVerbose extends Verbose {
  private final Server server;

  @Override
  public void broadcast(final Component component) {
    synchronized (subscribers) {
      for (final String subscriber : subscribers) {
        final Player player = server.getPlayer(subscriber);
        if (player != null) {
          // TODO: use an adventure audience
        }
      }
    }
  }
}
