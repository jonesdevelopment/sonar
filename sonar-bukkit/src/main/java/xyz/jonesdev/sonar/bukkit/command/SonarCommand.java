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

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.cappuccino.Cappuccino;
import xyz.jonesdev.cappuccino.ExpiringCache;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.InvocationSender;
import xyz.jonesdev.sonar.api.command.argument.Argument;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public final class SonarCommand implements CommandExecutor, TabExecutor {
  private static final List<String> TAB_SUGGESTIONS = new ArrayList<>();
  private static final Map<String, List<String>> ARG_TAB_SUGGESTIONS = new HashMap<>();
  private static final ExpiringCache<CommandSender> DELAY = Cappuccino.buildExpiring(500L);

  private static final TextComponent GITHUB_LINK_COMPONENT = new TextComponent(ChatColor.GREEN + "https://github" +
    ".com/jonesdevelopment/sonar");
  private static final TextComponent DISCORD_SUPPORT = new TextComponent(ChatColor.YELLOW + "Open a ticket on the " +
    "Discord ");
  private static final TextComponent GITHUB_ISSUES = new TextComponent(ChatColor.YELLOW + "or open a new issue on " +
    "GitHub.");

  static {
    GITHUB_LINK_COMPONENT.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github" +
      ".com/jonesdevelopment/sonar"));
    DISCORD_SUPPORT.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://jonesdev.xyz/discord/"));
    DISCORD_SUPPORT.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("(Click to open " +
      "Discord)").create()));
    GITHUB_ISSUES.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/jonesdevelopment/sonar" +
      "/issues"));
    GITHUB_ISSUES.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("(Click to open " +
      "GitHub)").create()));
  }

  private static final List<TextComponent> HELP = new Vector<>();

  static {
    HELP.addAll(Arrays.asList(
      new TextComponent(ChatColor.YELLOW + "Running Sonar " + Sonar.get().getVersion()
        + " on " + Sonar.get().getServer().getPlatform().getDisplayName()
        + "."),
      new TextComponent(ChatColor.YELLOW + "(C) 2023 Jones Development and Sonar Contributors"),
      GITHUB_LINK_COMPONENT,
      new TextComponent(""),
      new TextComponent(ChatColor.YELLOW + "Need help or have any questions?"),
      new TextComponent(
        DISCORD_SUPPORT,
        GITHUB_ISSUES
      ),
      new TextComponent("")
    ));

    Sonar.get().getSubcommandRegistry().getSubcommands().forEach(sub -> {
      final TextComponent component = new TextComponent(
        new TextComponent(ChatColor.GRAY + " ▪ "),
        new TextComponent(ChatColor.GREEN + "/sonar " + sub.getInfo().name()),
        new TextComponent(ChatColor.GRAY + " - "),
        new TextComponent(ChatColor.WHITE + sub.getInfo().description())
      );

      component.setClickEvent(
        new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sonar " + sub.getInfo().name() + " ")
      );
      component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
          "§7Only players: §f" + (sub.getInfo().onlyPlayers() ? "§a✔" : "§c✗")
            + Sonar.LINE_SEPARATOR + "§7Require console: §f" + (sub.getInfo().onlyConsole() ? "§a✔" : "§c✗")
            + Sonar.LINE_SEPARATOR + "§7Permission: §f" + sub.getPermission()
            + Sonar.LINE_SEPARATOR + "§7Aliases: §f" + sub.getAliases()
        ).create())
      );
      HELP.add(component);
    });
  }

  @Override
  public boolean onCommand(final CommandSender sender,
                           final Command command,
                           final String label,
                           final String[] args) {
    if (!(sender instanceof ConsoleCommandSender)) {
      // Checking if it contains will only break more since it can throw
      // a NullPointerException if the cache is being accessed from parallel threads
      DELAY.cleanUp(); // Clean up the cache
      final long mapTimestamp = DELAY.asMap().getOrDefault(sender, -1L);

      // There were some exploits with spamming commands in the past.
      // Spamming should be prevented, especially if some heavy operations are done,
      // which is not the case here but let's still stay safe!
      if (mapTimestamp > 0L) {
        sender.sendMessage(Sonar.get().getConfig().COMMAND_COOL_DOWN);

        // Format delay
        final long timestamp = System.currentTimeMillis();
        final double left = 0.5D - (timestamp - mapTimestamp) / 1000D;

        sender.sendMessage(
          Sonar.get().getConfig().COMMAND_COOL_DOWN_LEFT
            .replace("%time-left%", Sonar.DECIMAL_FORMAT.format(left))
        );
        return false;
      }

      DELAY.put(sender);
    }

    Optional<Subcommand> subcommand = Optional.empty();

    final InvocationSender invocationSender = new InvocationSender() {

      @Override
      public String getName() {
        return sender.getName();
      }

      @Override
      public void sendMessage(final String message) {
        sender.sendMessage(message);
      }
    };

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
          invocationSender.sendMessage(
            Sonar.get().getConfig().SUB_COMMAND_NO_PERM
              .replace("%permission%", subcommand.get().getPermission())
          );
          return false;
        }
      }
    }

    subcommand.ifPresent(sub -> {
      if (sub.getInfo().onlyPlayers() && !(sender instanceof Player)) {
        invocationSender.sendMessage(Sonar.get().getConfig().PLAYERS_ONLY);
        return;
      }

      if (sub.getInfo().onlyConsole() && !(sender instanceof ConsoleCommandSender)) {
        invocationSender.sendMessage(Sonar.get().getConfig().CONSOLE_ONLY);
        return;
      }

      final CommandInvocation commandInvocation = new CommandInvocation(invocationSender, sub, args);

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

    if (!subcommand.isPresent()) {

      // Re-use the old, cached help message since we don't want to scan
      // for each subcommand and it's arguments/attributes every time
      // someone runs /sonar since the subcommand don't change
      for (final TextComponent component : HELP) {
        if (sender instanceof Player) {
          ((Player) sender).spigot().sendMessage(component);
        } else {
          sender.sendMessage(component.getText());
        }
      }
    }
    return false;
  }

  @Override
  public List<String> onTabComplete(final CommandSender sender,
                                    final Command command,
                                    final String commandAlias,
                                    final String @NotNull [] args) {
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
