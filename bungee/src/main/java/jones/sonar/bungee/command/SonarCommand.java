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
import jones.sonar.common.command.subcommand.argument.Argument;
import lombok.var;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public final class SonarCommand extends Command implements TabExecutor {
  private static final Cache<CommandSender, Long> delay = CacheBuilder.newBuilder()
    .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
    .build();
  private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");

  public SonarCommand() {
    super("sonar", "sonar.command");
  }

  @Override
  @SuppressWarnings("deprecation")
  public void execute(final CommandSender sender, final String[] args) {
    // Checking if it contains will only break more since it can throw
    // a NullPointerException if the cache is being accessed from parallel threads
    final long timestamp = delay.asMap().getOrDefault(sender, -1L);
    final long currentTimestamp = System.currentTimeMillis();

    // There were some exploits with spamming commands in the past,
    // Spamming should be prevented, especially if some heavy operations are done,
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

    final var invocationSender = new InvocationSender() {

      @Override
      public void sendMessage(final String message) {
        sender.sendMessage(new TextComponent(message));
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
      subCommand.ifPresent(it -> {
        if (!sender.hasPermission(it.getPermission())) {
          invocationSender.sendMessage(Sonar.get().getConfig().SUB_COMMAND_NO_PERM
            .replace("%permission%", it.getPermission()));
        }
      });
    }

    // No subcommand was found
    if (!subCommand.isPresent()) {
      invocationSender.sendMessage();
      invocationSender.sendMessage(
        " §eRunning §lSonar §e"
          + Sonar.get().getVersion()
          + " on "
          + Sonar.get().getPlatform().getDisplayName()
      );

      final TextComponent discordComponent = new TextComponent(
        " §7Need help?§b discord.jonesdev.xyz"
      );
      discordComponent.setHoverEvent(new HoverEvent(
        HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click to open Discord").create()
      ));
      discordComponent.setClickEvent(new ClickEvent(
        ClickEvent.Action.OPEN_URL, "https://discord.jonesdev.xyz/"
      ));
      sender.sendMessage(discordComponent);

      invocationSender.sendMessage();

      SubCommandRegistry.getSubCommands().forEach(sub -> {
        final TextComponent component = new TextComponent(" §a▪ §7/sonar "
          + sub.getInfo().name()
          + " §f"
          + sub.getInfo().description());

        if (sender instanceof ProxiedPlayer) {
          component.setClickEvent(
            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sonar " + sub.getInfo().name() + " ")
          );

          component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
            "§7Only players: §f" + (sub.getInfo().onlyPlayers() ? "§a✔" : "§c✗")
              + "\n§7Only console: §f" + (sub.getInfo().onlyConsole() ? "§a✔" : "§c✗")
              + "\n§7Permission: §f" + sub.getPermission()
              + "\n§7(Click to run)"
          ).create()));
        }

        sender.sendMessage(component);
      });

      invocationSender.sendMessage();
      return;
    }

    // ifPresentOrElse() doesn't exist yet... (version compatibility)
    subCommand.ifPresent(sub -> {
      if (sub.getInfo().onlyPlayers() && !(sender instanceof ProxiedPlayer)) {
        invocationSender.sendMessage(Sonar.get().getConfig().PLAYERS_ONLY);
        return;
      }

      if (sub.getInfo().onlyConsole() && sender instanceof ProxiedPlayer) {
        invocationSender.sendMessage(Sonar.get().getConfig().CONSOLE_ONLY);
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
        invocationSender.sendMessage(
          Sonar.get().getConfig().INCORRECT_COMMAND_USAGE
            .replace("%usage%", sub.getInfo().name() + " (" + sub.getArguments() + ")")
        );
        return;
      }

      // Execute the sub command with the custom invocation properties
      sub.execute(commandInvocation);
    });
  }

  private static final Collection<String> TAB_SUGGESTIONS = new ArrayList<>();
  private static final Map<String, List<String>> ARG_TAB_SUGGESTIONS = new HashMap<>();

  // Tab completion handling
  @Override
  public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
    if (args.length <= 1) {
      if (TAB_SUGGESTIONS.isEmpty()) {
        for (final SubCommand subCommand : SubCommandRegistry.getSubCommands()) {
          TAB_SUGGESTIONS.add(subCommand.getInfo().name());

          if (subCommand.getInfo().aliases().length > 0) {
            TAB_SUGGESTIONS.addAll(Arrays.asList(subCommand.getInfo().aliases()));
          }
        }
      }
      return TAB_SUGGESTIONS;
    } else if (args.length == 2) {
      if (ARG_TAB_SUGGESTIONS.isEmpty()) {
        for (final SubCommand subCommand : SubCommandRegistry.getSubCommands()) {
          ARG_TAB_SUGGESTIONS.put(subCommand.getInfo().name(),
            Arrays.stream(subCommand.getInfo().arguments())
              .map(Argument::name)
              .collect(Collectors.toList())
          );
        }
      }

      final String subCommandName = args[0].toLowerCase();
      return ARG_TAB_SUGGESTIONS.getOrDefault(subCommandName, emptyList());
    } else return emptyList();
  }
}
