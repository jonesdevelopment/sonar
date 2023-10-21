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

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.SonarCommand;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;

import java.util.Arrays;
import java.util.Optional;

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

    if (invocationSource.isPlayer()) {
      // Check if the player actually has the permission to run the command
      if (!sender.hasPermission("sonar.command")) {
        invocationSource.sendMessage(Sonar.get().getConfig().getNoPermission());
        return;
      }
      // Checking if it contains will only break more since it can throw
      // a NullPointerException if the cache is being accessed from parallel threads
      DELAY.cleanUp(); // Clean up the cache
      final long mapTimestamp = DELAY.asMap().getOrDefault(sender, -1L);

      // There were some exploits with spamming commands in the past.
      // Spamming should be prevented, especially if some heavy operations are done,
      // which is not the case here but let's still stay safe!
      if (mapTimestamp > 0L) {
        invocationSource.sendMessage(Sonar.get().getConfig().getCommands().getCommandCoolDown());

        // Format delay
        final long timestamp = System.currentTimeMillis();
        final double left = 0.5D - (timestamp - mapTimestamp) / 1000D;

        invocationSource.sendMessage(Sonar.get().getConfig().getCommands().getCommandCoolDownLeft()
          .replace("%time-left%", Sonar.DECIMAL_FORMAT.format(left)));
        return;
      }

      DELAY.put(sender);
    }

    Optional<Subcommand> subcommand = Optional.empty();

    if (args.length > 0) {
      // Search subcommand if command arguments are present
      subcommand = Sonar.get().getSubcommandRegistry().getSubcommands().parallelStream()
        .filter(sub -> sub.getInfo().name().equalsIgnoreCase(args[0])
          || Arrays.stream(sub.getInfo().aliases())
          .anyMatch(alias -> alias.equalsIgnoreCase(args[0])))
        .findFirst();

      // Check permissions for subcommands
      if (subcommand.isPresent()) {
        if (!subcommand.get().getInfo().onlyConsole()
          && !sender.hasPermission(subcommand.get().getPermission())
        ) {
          invocationSource.sendMessage(Sonar.get().getConfig().getCommands().getSubCommandNoPerm()
            .replace("%permission%", subcommand.get().getPermission()));
          return;
        }
      }
    }

    if (!subcommand.isPresent()) {

      // Re-use the old, cached help message since we don't want to scan
      // for each subcommand and it's arguments/attributes every time
      // someone runs /sonar since the subcommand don't change
      for (final Component component : CACHED_HELP_MESSAGE) {
        invocationSource.sendMessage(component);
      }
    } else {
      subcommand.get().invoke(invocationSource, args);
    }
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
