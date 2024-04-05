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

package xyz.jonesdev.sonar.bukkit.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.SonarCommand;
import xyz.jonesdev.sonar.bukkit.SonarBukkit;

import java.util.List;

import static java.util.Collections.emptyList;

public final class BukkitSonarCommand implements CommandExecutor, TabExecutor, SonarCommand {

  @Override
  public boolean onCommand(final CommandSender sender,
                           final Command command,
                           final String label,
                           final String[] args) {
    // Create our own invocation source wrapper to handle messages properly
    final InvocationSource invocationSource = new InvocationSource(
      sender instanceof Player ? ((Player) sender).getUniqueId() : null,
      SonarBukkit.INSTANCE.getBukkitAudiences().sender(sender),
      sender::hasPermission);
    // Pass the invocation source and command arguments to our command handler
    handle(invocationSource, args);
    return true; // Valid
  }

  @Override
  public List<String> onTabComplete(final @NotNull CommandSender sender,
                                    final Command command,
                                    final String commandAlias,
                                    final String @NotNull [] args) {
    // Do not allow tab completion if the player does not have the required permission
    if (!sender.hasPermission("sonar.command")) {
      return emptyList();
    }
    return getCachedTabSuggestions(args);
  }
}
