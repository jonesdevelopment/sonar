/*
 * Copyright (C) 2025 Sonar Contributors
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
import xyz.jonesdev.sonar.api.update.UpdateChecker;

import java.util.*;

public interface SonarCommand {
  Map<Integer, List<String>> TAB_SUGGESTIONS = new LinkedHashMap<>();

  default void handle(final @NotNull InvocationSource source, final String @NotNull [] args) {
    // Check if the player actually has the permission to run the command
    if (source.isPlayer() && !source.getPermissionFunction().test("sonar.command")) {
      if (Sonar.get0().getConfig().getNoPermission() != null) {
        source.sendMessage(Sonar.get0().getConfig().getNoPermission());
      }
      return;
    }

    Optional<Subcommand> subcommand = Optional.empty();

    if (args.length > 0) {
      // Search subcommand if command arguments are present
      subcommand = Sonar.get0().getSubcommandRegistry().getSubcommands().stream()
        .filter(sub -> sub.getInfo().name().equalsIgnoreCase(args[0])
          || Arrays.stream(sub.getInfo().aliases()).anyMatch(alias -> alias.equalsIgnoreCase(args[0])))
        .findFirst();
    }

    subcommand.ifPresentOrElse(command -> {
      // Check permissions for subcommands
      if (!command.getInfo().onlyConsole()
        && !source.getPermissionFunction().test(command.getPermission())) {
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.subcommand-no-permission"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("permission", command.getPermission())));
        return;
      }
      // Invoke subcommand
      command.invoke(source, args);
    }, () -> {
      final UpdateChecker.CheckResult checkResult = UpdateChecker.getLastCheckResult();
      Component version = Component.text(Sonar.get0().getVersion().getFormatted());
      if (checkResult.getConfigKey() != null) {
        version = version.append(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.main." + checkResult.getConfigKey())));
      }

      // Print standard help message
      source.sendMessage(MiniMessage.miniMessage().deserialize(
        String.join("<newline>",
          Sonar.get0().getConfig().getMessagesConfig().getStringList("commands.main.header")),
        Placeholder.component("version", version),
        Placeholder.unparsed("platform", Sonar.get0().getPlatform().getDisplayName()),
        Placeholder.unparsed("copyright-year", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))));

      final Component yes = MiniMessage.miniMessage().deserialize(
        Sonar.get0().getConfig().getMessagesConfig().getString("commands.main.tick"));
      final Component no = MiniMessage.miniMessage().deserialize(
        Sonar.get0().getConfig().getMessagesConfig().getString("commands.main.cross"));

      Sonar.get0().getSubcommandRegistry().getSubcommands().forEach(command -> {
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.main.subcommands"),
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

  static void prepareCachedTabSuggestions() {
    if (TAB_SUGGESTIONS.isEmpty()) {
      final List<String> subcommandsAndAliases = new ArrayList<>();
      for (final Subcommand subcommand : Sonar.get0().getSubcommandRegistry().getSubcommands()) {
        subcommandsAndAliases.add(subcommand.getInfo().name());

        final List<String> arguments = Arrays.asList(subcommand.getInfo().arguments());
        TAB_SUGGESTIONS.put(subcommand.getInfo().name().hashCode(), arguments);

        for (final String alias : subcommand.getInfo().aliases()) {
          subcommandsAndAliases.add(alias);
          TAB_SUGGESTIONS.put(alias.hashCode(), arguments);
        }
      }
      TAB_SUGGESTIONS.put(-1, subcommandsAndAliases);
    }
  }

  default List<String> getCachedTabSuggestions(final String @NotNull [] arguments) {
    if (arguments.length <= 2) {
      final int subcommandHash = arguments.length <= 1 ? -1 : arguments[0].toLowerCase().hashCode();
      return TAB_SUGGESTIONS.getOrDefault(subcommandHash, Collections.emptyList());
    }
    return Collections.emptyList();
  }
}
