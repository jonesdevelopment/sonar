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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.jonesdev.cappuccino.Cappuccino;
import xyz.jonesdev.cappuccino.ExpiringCache;
import xyz.jonesdev.sonar.api.Sonar;

import java.util.*;

public interface SonarCommand {
  List<String> TAB_SUGGESTIONS = new ArrayList<>();

  Map<String, List<String>> ARG_TAB_SUGGESTIONS = new HashMap<>();

  ExpiringCache<Object> DELAY = Cappuccino.buildExpiring(500L);

  int COPYRIGHT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

  List<Component> CACHED_HELP_MESSAGE = new Vector<>();

  default void cacheHelpMessage() {
    CACHED_HELP_MESSAGE.addAll(Arrays.asList(
      Component.text("Running Sonar " + Sonar.get().getVersion()
        + " on " + Sonar.get().getPlatform().getDisplayName()
        + ".", NamedTextColor.YELLOW),
      Component.text("(C) " + COPYRIGHT_YEAR + " Jones Development and Sonar Contributors", NamedTextColor.YELLOW),
      Component.text("https://github.com/jonesdevelopment/sonar", NamedTextColor.GREEN)
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/jonesdevelopment/sonar")),
      Component.empty(),
      Component.text("Need help or have any questions?", NamedTextColor.YELLOW),
      Component.textOfChildren(
        Component.text("Open a ticket on the Discord ", NamedTextColor.YELLOW)
          .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("(Click to open Discord)")))
          .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://jonesdev.xyz/discord/")),
        Component.text("or open a new issue on GitHub.", NamedTextColor.YELLOW)
          .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("(Click to open GitHub)")))
          .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/jonesdevelopment/sonar" +
            "/issues"))
      ),
      Component.empty()
    ));

    Sonar.get().getSubcommandRegistry().getSubcommands().forEach(sub -> {
      Component component = Component.textOfChildren(
        Component.text(" ▪ ", NamedTextColor.GRAY),
        Component.text("/sonar " + sub.getInfo().name(), NamedTextColor.GREEN),
        Component.text(" - ", NamedTextColor.GRAY),
        Component.text(sub.getInfo().description(), NamedTextColor.WHITE)
      );

      Component hoverComponent = Component.textOfChildren(
        Component.text("Only players: ", NamedTextColor.GRAY),
        Component.text(sub.getInfo().onlyPlayers() ? "✔" : "✗",
          sub.getInfo().onlyPlayers() ? NamedTextColor.GREEN : NamedTextColor.RED),
        Component.newline(),
        Component.text("Require console: ", NamedTextColor.GRAY),
        Component.text(sub.getInfo().onlyConsole() ? "✔" : "✗",
          sub.getInfo().onlyConsole() ? NamedTextColor.GREEN : NamedTextColor.RED),
        Component.newline(),
        Component.text("Permission: ", NamedTextColor.GRAY),
        Component.text(sub.getPermission(), NamedTextColor.WHITE)
      );
      if (sub.getInfo().aliases().length > 0) {
        hoverComponent = hoverComponent
          .append(Component.newline())
          .append(Component.text("Aliases: ", NamedTextColor.GRAY))
          .append(Component.text(sub.getAliases(), NamedTextColor.WHITE));
      }
      component = component
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
          "/sonar " + sub.getInfo().name() + " "))
        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent));
      CACHED_HELP_MESSAGE.add(component);
    });
  }
}
