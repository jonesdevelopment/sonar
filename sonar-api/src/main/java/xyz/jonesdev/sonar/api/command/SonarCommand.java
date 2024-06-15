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

package xyz.jonesdev.sonar.api.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.argument.Argument;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public interface SonarCommand {
  List<String> TAB_SUGGESTIONS = new ArrayList<>();

  Map<String, List<String>> ARG_TAB_SUGGESTIONS = new HashMap<>();

  List<Component> CACHED_HELP_MESSAGE = new ArrayList<>();

  default void handle(final @NotNull InvocationSource source, final String @NotNull [] args) {
    if (args.length > 0) {
      // Search subcommand if command arguments are present
      final Optional<Subcommand> subcommand = Sonar.get().getSubcommandRegistry().getSubcommands().stream()
        .filter(sub -> sub.getInfo().name().equalsIgnoreCase(args[0])
          || Arrays.stream(sub.getInfo().aliases())
          .anyMatch(alias -> alias.equalsIgnoreCase(args[0])))
        .findFirst();

      // Check permissions for subcommands
      if (subcommand.isPresent()) {
        if (!subcommand.get().getInfo().onlyConsole()
          && !source.getPermissionFunction().test(subcommand.get().getPermission())) {
          source.sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getCommands().getSubCommandNoPerm(),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
            Placeholder.unparsed("permission", subcommand.get().getPermission())));
          return;
        }
        subcommand.get().invoke(source, args);
        return;
      }
    }

    // Re-use the old, cached help message since we don't want to scan
    // for each subcommand and it's arguments/attributes every time
    // someone runs /sonar since the subcommand don't change
    for (final Component component : CACHED_HELP_MESSAGE) {
      source.sendMessage(component);
    }
  }

  static void prepareCachedMessages() {
    // Cache help message
    CACHED_HELP_MESSAGE.clear();
    CACHED_HELP_MESSAGE.add(MiniMessage.miniMessage().deserialize(
      String.join("<newline>",
        Sonar.get().getConfig().getMessagesConfig().getStringList("commands.main.header")),
      Placeholder.unparsed("version", Sonar.get().getVersion().getFormatted()),
      Placeholder.unparsed("platform", Sonar.get().getPlatform().getDisplayName()),
      Placeholder.unparsed("copyright_year", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))));

    final Component yes = MiniMessage.miniMessage().deserialize("<green>✔</green>");
    final Component no = MiniMessage.miniMessage().deserialize("<red>✗</red>");

    Sonar.get().getSubcommandRegistry().getSubcommands().forEach(subcommand -> {
      CACHED_HELP_MESSAGE.add(MiniMessage.miniMessage().deserialize(
        Sonar.get().getConfig().getMessagesConfig().getString("commands.main.subcommands"),
        Placeholder.unparsed("subcommand", subcommand.getInfo().name()),
        Placeholder.unparsed("description", subcommand.getInfo().description()),
        Placeholder.component("only_players", subcommand.getInfo().onlyPlayers() ? yes : no),
        Placeholder.component("require_console", subcommand.getInfo().onlyConsole() ? yes : no),
        Placeholder.unparsed("permission", subcommand.getPermission()),
        Placeholder.unparsed("aliases", subcommand.getAliases())));
    });

    // Don't re-cache tab suggestions
    if (!TAB_SUGGESTIONS.isEmpty()) return;
    // Cache tab suggestions
    for (final Subcommand subcommand : Sonar.get().getSubcommandRegistry().getSubcommands()) {
      TAB_SUGGESTIONS.add(subcommand.getInfo().name());
      if (subcommand.getInfo().aliases().length > 0) {
        TAB_SUGGESTIONS.addAll(Arrays.asList(subcommand.getInfo().aliases()));
      }
      final List<String> parsedArguments = Arrays.stream(subcommand.getInfo().arguments())
        .map(Argument::value)
        .collect(Collectors.toList());
      ARG_TAB_SUGGESTIONS.put(subcommand.getInfo().name(), parsedArguments);
      for (final String alias : subcommand.getInfo().aliases()) {
        ARG_TAB_SUGGESTIONS.put(alias, parsedArguments);
      }
    }
  }

  default List<String> getCachedTabSuggestions(final String @NotNull [] arguments) {
    if (arguments.length <= 1) {
      return TAB_SUGGESTIONS;
    } else if (arguments.length == 2) {
      final String subCommandName = arguments[0].toLowerCase();
      return ARG_TAB_SUGGESTIONS.getOrDefault(subCommandName, emptyList());
    }
    return emptyList();
  }
}
