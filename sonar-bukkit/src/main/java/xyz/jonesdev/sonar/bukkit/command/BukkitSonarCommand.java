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

package xyz.jonesdev.sonar.bukkit.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.SonarCommand;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.argument.Argument;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public final class BukkitSonarCommand implements CommandExecutor, TabExecutor, SonarCommand {
  {
    cacheHelpMessage();
  }

  @Override
  public boolean onCommand(final CommandSender sender,
                           final Command command,
                           final String label,
                           final String[] args) {
    if (!(sender instanceof ConsoleCommandSender)) {
      // Check if the player actually has the permission to run the command
      if (!sender.hasPermission("sonar.command")) {
        sender.sendMessage(Sonar.get().getConfig().getNoPermission());
        return false;
      }
      // Checking if it contains will only break more since it can throw
      // a NullPointerException if the cache is being accessed from parallel threads
      DELAY.cleanUp(); // Clean up the cache
      final long mapTimestamp = DELAY.asMap().getOrDefault(sender, -1L);

      // There were some exploits with spamming commands in the past.
      // Spamming should be prevented, especially if some heavy operations are done,
      // which is not the case here but let's still stay safe!
      if (mapTimestamp > 0L) {
        sender.sendMessage(Sonar.get().getConfig().commandCoolDown);

        // Format delay
        final long timestamp = System.currentTimeMillis();
        final double left = 0.5D - (timestamp - mapTimestamp) / 1000D;

        sender.sendMessage(
          Sonar.get().getConfig().commandCoolDownLeft
            .replace("%time-left%", Sonar.DECIMAL_FORMAT.format(left))
        );
        return false;
      }

      DELAY.put(sender);
    }

    Optional<Subcommand> subcommand = Optional.empty();

    final InvocationSource invocationSource = new BukkitInvocationSource(sender);

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
          invocationSource.sendMessage(
            Sonar.get().getConfig().subCommandNoPerm
              .replace("%permission%", subcommand.get().getPermission())
          );
          return false;
        }
      }
    }

    subcommand.ifPresent(sub -> {
      if (sub.getInfo().onlyPlayers() && !(sender instanceof Player)) {
        invocationSource.sendMessage(Sonar.get().getConfig().playersOnly);
        return;
      }

      if (sub.getInfo().onlyConsole() && !(sender instanceof ConsoleCommandSender)) {
        invocationSource.sendMessage(Sonar.get().getConfig().consoleOnly);
        return;
      }

      final CommandInvocation commandInvocation = new CommandInvocation(invocationSource, sub, args);

      // The subcommands has arguments which are not present in the executed command
      if (sub.getInfo().arguments().length > 0
        && commandInvocation.getRawArguments().length <= 1) {
        invocationSource.sendMessage(
          Sonar.get().getConfig().incorrectCommandUsage
            .replace("%usage%", sub.getInfo().name() + " (" + sub.getArguments() + ")")
        );
        return;
      }

      // Execute the sub command with the custom invocation properties
      sub.execute(commandInvocation);
    });

    if (!subcommand.isPresent()) {

      // Re-use the old, cached help message since we don't want to scan
      // for each subcommand and it's arguments/attributes every time
      // someone runs /sonar since the subcommand don't change
      for (final Component component : CACHED_HELP_MESSAGE) {
        invocationSource.sendMessage(component);
      }
    }
    return false;
  }

  @Override
  public List<String> onTabComplete(final CommandSender sender,
                                    final Command command,
                                    final String commandAlias,
                                    final String @NotNull [] args) {
    // Do not allow tab completion if the player does not have the required permission
    if (!sender.hasPermission("sonar.command")) {
      return emptyList();
    }
    if (args.length <= 1) {
      if (TAB_SUGGESTIONS.isEmpty()) {
        for (final Subcommand subcommand : Sonar.get().getSubcommandRegistry().getSubcommands()) {
          TAB_SUGGESTIONS.add(subcommand.getInfo().name());

          if (subcommand.getInfo().aliases().length > 0) {
            TAB_SUGGESTIONS.addAll(Arrays.asList(subcommand.getInfo().aliases()));
          }
        }
      }
      return TAB_SUGGESTIONS;
    } else if (args.length == 2) {
      if (ARG_TAB_SUGGESTIONS.isEmpty()) {
        for (final Subcommand subcommand : Sonar.get().getSubcommandRegistry().getSubcommands()) {
          final List<String> parsedArguments = Arrays.stream(subcommand.getInfo().arguments())
            .map(Argument::value)
            .collect(Collectors.toList());
          ARG_TAB_SUGGESTIONS.put(subcommand.getInfo().name(), parsedArguments);
          for (final String alias : subcommand.getInfo().aliases()) {
            ARG_TAB_SUGGESTIONS.put(alias, parsedArguments);
          }
        }
      }

      final String subCommandName = args[0].toLowerCase();
      return ARG_TAB_SUGGESTIONS.getOrDefault(subCommandName, emptyList());
    }
    return emptyList();
  }
}
