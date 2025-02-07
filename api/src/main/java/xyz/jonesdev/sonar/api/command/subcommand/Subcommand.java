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

package xyz.jonesdev.sonar.api.command.subcommand;

import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.InvocationSource;

import java.util.Objects;
import java.util.regex.Pattern;

@Getter
public abstract class Subcommand {
  private final @NotNull SubcommandInfo info;
  private final String permission, aliases, arguments;

  public Subcommand() {
    this.info = Objects.requireNonNull(getClass().getAnnotation(SubcommandInfo.class));
    this.permission = "sonar." + info.name();
    this.aliases = info.aliases().length == 0 ? "No aliases."
      : String.join(", ", info.aliases());
    this.arguments = info.arguments().length == 0 ? ""
      : String.join(", ", info.arguments());
  }

  private static final Pattern IPv4_REGEX = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
  private static final Pattern IPv6_REGEX = Pattern.compile("(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))");

  protected static @Nullable String validateIP(final InvocationSource source, final String raw) {
    if (!IPv4_REGEX.matcher(raw).matches() && !IPv6_REGEX.matcher(raw).matches()) {
      source.sendMessage(MiniMessage.miniMessage().deserialize(
        Sonar.get0().getConfig().getMessagesConfig().getString("commands.invalid-ip-address"),
        Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())));
      return null;
    }
    return raw;
  }

  protected final void incorrectUsage(final @NotNull InvocationSource invocationSource) {
    incorrectUsage(invocationSource, info.name() + " (" + arguments + ")");
  }

  protected final void incorrectUsage(final @NotNull InvocationSource invocationSource, final String usage) {
    invocationSource.sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get0().getConfig().getMessagesConfig().getString("commands.incorrect-usage"),
      Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
      Placeholder.unparsed("subcommand-usage", usage)));
  }

  public final void invoke(final @NotNull InvocationSource invocationSource, final String @NotNull [] arguments) {
    // Check if the subcommand can only be executed by players
    if (info.onlyPlayers() && !invocationSource.isPlayer()) {
      invocationSource.sendMessage(MiniMessage.miniMessage().deserialize(
        Sonar.get0().getConfig().getMessagesConfig().getString("commands.player-only"),
        Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())));
      return;
    }

    // Check if the subcommand can only be executed though console
    if (info.onlyConsole() && invocationSource.isPlayer()) {
      invocationSource.sendMessage(MiniMessage.miniMessage().deserialize(
        Sonar.get0().getConfig().getMessagesConfig().getString("commands.console-only"),
        Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())));
      return;
    }

    // The subcommands has arguments which are not present in the executed command
    if (info.argumentsRequired() && info.arguments().length > 0 && arguments.length <= 1) {
      incorrectUsage(invocationSource);
      return;
    }

    // Execute the sub command from the invocation source with the given arguments
    execute(invocationSource, arguments);
  }

  public final @NotNull String getDescription() {
    final String path = String.format("commands.%s.description", info.name());
    return Objects.requireNonNull(Sonar.get0().getConfig().getMessagesConfig().getString(path));
  }

  protected abstract void execute(final @NotNull InvocationSource source, final String @NotNull [] args);
}
