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

package xyz.jonesdev.sonar.api.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.cappuccino.Cappuccino;
import xyz.jonesdev.cappuccino.ExpiringCache;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.argument.Argument;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public interface SonarCommand {
  List<String> TAB_SUGGESTIONS = new ArrayList<>();

  Map<String, List<String>> ARG_TAB_SUGGESTIONS = new HashMap<>();

  ExpiringCache<Object> DELAY = Cappuccino.buildExpiring(500L);

  List<Component> CACHED_HELP_MESSAGE = new ArrayList<>();

  static void prepareCachedMessages() {
    // Cache help message
    CACHED_HELP_MESSAGE.clear();
    for (final String message : Sonar.get().getConfig().getCommands().getHelpHeader()) {
      CACHED_HELP_MESSAGE.add(MiniMessage.miniMessage().deserialize(message
        .replace("%version%", Sonar.get().getVersion().getFormatted())
        .replace("%platform%", Sonar.get().getPlatform().getDisplayName())
        .replace("%copyright_year%", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))));
    }

    final String subcommandFormat = Sonar.get().getConfig().getCommands().getHelpSubcommands();
    Sonar.get().getSubcommandRegistry().getSubcommands().forEach(subcommand -> {
      final Component deserialized = MiniMessage.miniMessage().deserialize(subcommandFormat
        .replace("%subcommand%", subcommand.getInfo().name())
        .replace("%description%", subcommand.getInfo().description())
        .replace("%only_players%", subcommand.getInfo().onlyPlayers() ? "<green>✔</green>" : "<red>✗</red>")
        .replace("%require_console%", subcommand.getInfo().onlyConsole() ? "<green>✔</green>" : "<red>✗</red>")
        .replace("%permission%", subcommand.getPermission())
        .replace("%aliases%", subcommand.getAliases()));
      CACHED_HELP_MESSAGE.add(deserialized);
    });

    // Cache tab suggestions
    for (final Subcommand subcommand : Sonar.get().getSubcommandRegistry().getSubcommands()) {
      if (TAB_SUGGESTIONS.isEmpty()) {
        TAB_SUGGESTIONS.add(subcommand.getInfo().name());
        if (subcommand.getInfo().aliases().length > 0) {
          TAB_SUGGESTIONS.addAll(Arrays.asList(subcommand.getInfo().aliases()));
        }
      }
      if (ARG_TAB_SUGGESTIONS.isEmpty()) {
        final List<String> parsedArguments = Arrays.stream(subcommand.getInfo().arguments())
          .map(Argument::value)
          .collect(Collectors.toList());
        ARG_TAB_SUGGESTIONS.put(subcommand.getInfo().name(), parsedArguments);
        for (final String alias : subcommand.getInfo().aliases()) {
          ARG_TAB_SUGGESTIONS.put(alias, parsedArguments);
        }
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
