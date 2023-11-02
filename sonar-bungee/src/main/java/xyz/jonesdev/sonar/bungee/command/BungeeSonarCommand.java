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

package xyz.jonesdev.sonar.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.SonarCommand;

import static java.util.Collections.emptyList;

public final class BungeeSonarCommand extends Command implements TabExecutor, SonarCommand {
  public BungeeSonarCommand() {
    super("sonar");
  }

  @Override
  @SuppressWarnings({"redundantSuppression"})
  public void execute(final @NotNull CommandSender sender, final String[] args) {
    // Create our own invocation source wrapper to handle messages properly
    final InvocationSource invocationSource = new BungeeInvocationSource(sender);
    // Pass the invocation source and command arguments to our command handler
    handle(invocationSource, args);
  }

  @Override
  public Iterable<String> onTabComplete(final @NotNull CommandSender sender, final String @NotNull [] args) {
    // Do not allow tab completion if the player does not have the required permission
    if (!sender.hasPermission("sonar.command")) {
      return emptyList();
    }
    return getCachedTabSuggestions(args);
  }
}
