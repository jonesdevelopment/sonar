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

package xyz.jonesdev.sonar.api.command.subcommand;

import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.subcommand.argument.Argument;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public abstract class Subcommand {
  private final @NotNull SubcommandInfo info;
  private final String permission, aliases, arguments;

  public Subcommand() {
    this.info = getClass().getAnnotation(SubcommandInfo.class);
    this.permission = "sonar." + info.name();
    this.aliases = info.aliases().length == 0 ? "No aliases."
      : String.join(", ", info.aliases());
    this.arguments = info.arguments().length == 0 ? ""
      : Arrays.stream(info.arguments())
      .map(Argument::value)
      .collect(Collectors.joining(", "));
  }

  protected static @Nullable InetAddress getInetAddressIfValid(final InvocationSource source, final String raw) {
    final InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByName(raw);
    } catch (UnknownHostException exception) {
      source.sendMessage(MiniMessage.miniMessage().deserialize(
        Sonar.get().getConfig().getMessagesConfig().getString("commands.invalid-ip-address"),
        Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
      return null;
    }
    return inetAddress;
  }

  protected final void incorrectUsage(final @NotNull InvocationSource invocationSource) {
    incorrectUsage(invocationSource, info.name() + " (" + arguments + ")");
  }

  protected final void incorrectUsage(final @NotNull InvocationSource invocationSource, final String usage) {
    invocationSource.sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get().getConfig().getMessagesConfig().getString("commands.incorrect-usage"),
      Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
      Placeholder.unparsed("subcommand-usage", usage)));
  }

  public final void invoke(final @NotNull InvocationSource invocationSource, final String @NotNull [] arguments) {
    // Check if the subcommand can only be executed by players
    if (info.onlyPlayers() && !invocationSource.isPlayer()) {
      invocationSource.sendMessage(MiniMessage.miniMessage().deserialize(
        Sonar.get().getConfig().getMessagesConfig().getString("commands.player-only"),
        Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
      return;
    }

    // Check if the subcommand can only be executed though console
    if (info.onlyConsole() && invocationSource.isPlayer()) {
      invocationSource.sendMessage(MiniMessage.miniMessage().deserialize(
        Sonar.get().getConfig().getMessagesConfig().getString("commands.console-only"),
        Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
      return;
    }

    // The subcommands has arguments which are not present in the executed command
    if (info.argumentsRequired() && info.arguments().length > 0 && arguments.length <= 1) {
      incorrectUsage(invocationSource);
      return;
    }

    // Execute the sub command from the invocation source with the given arguments
    execute(new CommandInvocation(invocationSource, arguments));
  }

  protected abstract void execute(final @NotNull CommandInvocation invocation);
}
