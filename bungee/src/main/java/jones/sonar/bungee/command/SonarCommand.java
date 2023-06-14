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

package jones.sonar.bungee.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jones.sonar.api.Sonar;
import jones.sonar.common.command.CommandInvocation;
import jones.sonar.common.command.InvocationSender;
import jones.sonar.common.command.subcommand.SubCommand;
import jones.sonar.common.command.subcommand.SubCommandRegistry;
import lombok.var;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static jones.sonar.common.command.CommandInvocation.printHelp;
import static jones.sonar.common.command.CommandInvocation.printSubNotFound;

public final class SonarCommand extends Command implements TabExecutor {
  private static final Cache<CommandSender, Long> delay = CacheBuilder.newBuilder()
    .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
    .build();
  private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

  public SonarCommand() {
    super("sonar", "sonar.command");
  }

  @Override
  public void execute(final CommandSender sender, final String[] args) {
    // Checking if it contains will only break more since it can throw
    // a NullPointerException if the cache is being accessed from parallel threads
    final long timestamp = delay.asMap().getOrDefault(sender, -1L);
    final long currentTimestamp = System.currentTimeMillis();

    // There were some exploits with spamming commands in the past
    // Spamming should be prevented especially if some heavy operations are done
    // which is not the case here but let's still stay safe!
    if (timestamp > 0L) {
      sender.sendMessage(new TextComponent(Sonar.get().getConfig().COMMAND_COOL_DOWN));

      // Format delay
      final double left = 0.5D - ((currentTimestamp - (double) timestamp) / 1000D);

      sender.sendMessage(new TextComponent(Sonar.get().getConfig().COMMAND_COOL_DOWN_LEFT
        .replace("%time-left%", decimalFormat.format(left))));
      return;
    }

    delay.put(sender, currentTimestamp);

    Optional<SubCommand> subCommand = Optional.empty();

    final var invocationSender = new InvocationSender<CommandSender>() {

      @Override
      public void sendMessage(final String message) {
        sender.sendMessage(new TextComponent(message));
      }

      @Override
      public CommandSender getPlayer() {
        return sender;
      }
    };

    if (args.length > 0) {
      // Search subcommand if command arguments are present
      subCommand = SubCommandRegistry.getSubCommands().stream()
        .filter(sub -> sub.getInfo().name().equalsIgnoreCase(args[0])
          || (sub.getInfo().aliases().length > 0
          && Arrays.stream(sub.getInfo().aliases())
          .anyMatch(alias -> alias.equalsIgnoreCase(args[0]))))
        .findFirst();

      // Check permissions for subcommands
      if (subCommand.isPresent()) {
        final String permission = "sonar." + subCommand.get().getInfo().name();

        if (!sender.hasPermission(permission)) {
          sender.sendMessage(new TextComponent(
            "§cYou do not have permission to execute this subcommand. §7(" + permission + ")"
          ));
          return;
        }
      }
    }

    // No subcommand was found
    if (!subCommand.isPresent()) {
      printHelp(invocationSender);
      return;
    }

    // ifPresentOrElse() doesn't exist yet... (version compatibility)
    subCommand.ifPresent(sub -> {
      if (sub.getInfo().onlyPlayers() && !(sender instanceof ProxiedPlayer)) {
        sender.sendMessage(new TextComponent(Sonar.get().getConfig().PLAYERS_ONLY));
        return;
      }

      final CommandInvocation commandInvocation = new CommandInvocation(
        sender.getName(),
        invocationSender,
        sub,
        args
      );

      // The subcommands has arguments which are not present in the executed command
      if (sub.getInfo().arguments().length > 0
        && commandInvocation.getArguments().length <= 1) {
        printSubNotFound(invocationSender, sub);
        return;
      }

      // Execute the sub command with the custom invocation properties
      sub.execute(commandInvocation);
    });
  }

  private static final Collection<String> TAB_SUGGESTIONS = new ArrayList<>();

  // Tab completion handling
  @Override
  public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
    return args.length <= 1
      ? getSuggestions()
      : Collections.emptyList();
  }

  private static Collection<String> getSuggestions() {
    if (TAB_SUGGESTIONS.isEmpty()) {
      for (final SubCommand subCommand : SubCommandRegistry.getSubCommands()) {
        TAB_SUGGESTIONS.add(subCommand.getInfo().name());

        if (subCommand.getInfo().aliases().length > 0) {
          TAB_SUGGESTIONS.addAll(Arrays.asList(subCommand.getInfo().aliases()));
        }
      }
    }
    return TAB_SUGGESTIONS;
  }
}
