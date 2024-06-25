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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;

import java.util.*;

import static java.util.Collections.emptyList;

public interface SonarCommand {
  List<String> TAB_SUGGESTIONS = new ArrayList<>();

  Map<String, List<String>> ARG_TAB_SUGGESTIONS = new HashMap<>();

  default void handle(final @NotNull InvocationSource source, final String @NotNull [] args) {
    // Check if the player actually has the permission to run the command
    if (source.isPlayer() && !source.getPermissionFunction().test("sonar.command")) {
      source.sendMessage(Sonar.get().getConfig().getNoPermission());
      return;
    }

    Optional<Subcommand> subcommand = Optional.empty();

    if (args.length > 0) {
      // Search subcommand if command arguments are present
      subcommand = Sonar.get().getSubcommandRegistry().getSubcommands().stream()
        .filter(sub -> sub.getInfo().name().equalsIgnoreCase(args[0])
          || Arrays.stream(sub.getInfo().aliases()).anyMatch(alias -> alias.equalsIgnoreCase(args[0])))
        .findFirst();
    }

    subcommand.ifPresentOrElse(command -> {
      // Check permissions for subcommands
      if (!command.getInfo().onlyConsole()
        && !source.getPermissionFunction().test(command.getPermission())) {
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.subcommand-no-permission"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("permission", command.getPermission())));
        return;
      }
      // Invoke subcommand
      command.invoke(source, args);
    }, () -> {
      // Print standard help message
      source.sendMessage(MiniMessage.miniMessage().deserialize(
        String.join("<newline>",
          Sonar.get().getConfig().getMessagesConfig().getStringList("commands.main.header")),
        Placeholder.unparsed("version", Sonar.get().getVersion().getFormatted()),
        Placeholder.unparsed("platform", Sonar.get().getPlatform().getDisplayName()),
        Placeholder.unparsed("copyright-year", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))));

      final Component yes = MiniMessage.miniMessage().deserialize(
        Sonar.get().getConfig().getMessagesConfig().getString("commands.main.tick"));
      final Component no = MiniMessage.miniMessage().deserialize(
        Sonar.get().getConfig().getMessagesConfig().getString("commands.main.cross"));

      Sonar.get().getSubcommandRegistry().getSubcommands().forEach(command -> {
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.main.subcommands"),
          TagResolver.resolver("suggest-subcommand",
            (queue, context) -> Tag.styling(ClickEvent.suggestCommand("/sonar " + command.getInfo().name()))),
          Placeholder.unparsed("subcommand", command.getInfo().name()),
          Placeholder.unparsed("description", command.getDescription()),
          Placeholder.unparsed("permission", command.getPermission()),
          Placeholder.unparsed("aliases", command.getAliases()),
          Placeholder.component("only-players", command.getInfo().onlyPlayers() ? yes : no),
          Placeholder.component("only-console", command.getInfo().onlyConsole() ? yes : no)));
      });
    });
  }

  static void prepareCachedMessages() {
    // Don't re-cache tab suggestions
    if (!TAB_SUGGESTIONS.isEmpty()) return;
    // Cache tab suggestions
    for (final Subcommand subcommand : Sonar.get().getSubcommandRegistry().getSubcommands()) {
      TAB_SUGGESTIONS.add(subcommand.getInfo().name());
      if (subcommand.getInfo().aliases().length > 0) {
        TAB_SUGGESTIONS.addAll(Arrays.asList(subcommand.getInfo().aliases()));
      }
      final List<String> arguments = Arrays.asList(subcommand.getInfo().arguments());
      ARG_TAB_SUGGESTIONS.put(subcommand.getInfo().name(), arguments);
      for (final String alias : subcommand.getInfo().aliases()) {
        ARG_TAB_SUGGESTIONS.put(alias, arguments);
      }
    }
  }

  default List<String> getCachedTabSuggestions(final String @NotNull [] arguments) {
    if (arguments.length <= 1) {
      return TAB_SUGGESTIONS;
    } else if (arguments.length == 2) {
      final String subcommandName = arguments[0].toLowerCase();
      return ARG_TAB_SUGGESTIONS.getOrDefault(subcommandName, emptyList());
    }
    return emptyList();
  }
}
