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

package xyz.jonesdev.sonar.api.command.subcommand;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.subcommand.argument.Argument;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public abstract class Subcommand {
  private final @NotNull SubcommandInfo info;
  private final String permission, aliases, arguments;

  protected static final Sonar SONAR = Sonar.get();

  public Subcommand() {
    info = getClass().getAnnotation(SubcommandInfo.class);
    permission = "sonar." + info.name();
    aliases = info.aliases().length == 0 ? "No aliases."
      : String.join(", ", info.aliases());

    arguments = info.arguments().length == 0 ? ""
      : Arrays.stream(info.arguments())
      .map(Argument::value)
      .collect(Collectors.joining(", "));
  }

  // https://stackoverflow.com/questions/5284147/validating-ipv4-addresses-with-regexp
  private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

  protected static @Nullable InetAddress getInetAddressIfValid(final InvocationSource source, final String rawIP) {
    // We perform a regex check beforehand,
    // so we don't have to create a new InetAddress,
    // therefore, potentially saving some performance.
    if (!IP_PATTERN.matcher(Objects.requireNonNull(rawIP)).matches()) {
      source.sendMessage(SONAR.getConfig().getCommands().getIncorrectIpAddress());
      return null;
    }

    final InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByName(rawIP);

      if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
        source.sendMessage(SONAR.getConfig().getCommands().getIllegalIpAddress());
        return null;
      }
    } catch (UnknownHostException exception) {
      source.sendMessage(SONAR.getConfig().getCommands().getUnknownIpAddress());
      return null;
    }
    return inetAddress;
  }

  protected final void incorrectUsage(final @NotNull InvocationSource sender) {
    sender.sendMessage(SONAR.getConfig().getCommands().getIncorrectCommandUsage()
      .replace("%usage%", info.name() + " (" + arguments + ")"));
  }

  public final void invoke(final @NotNull InvocationSource invocationSource, final String @NotNull [] arguments) {
    // Check if the subcommand can only be executed by players
    if (getInfo().onlyPlayers() && !invocationSource.isPlayer()) {
      invocationSource.sendMessage(Sonar.get().getConfig().getCommands().getPlayersOnly());
      return;
    }

    // Check if the subcommand can only be executed though console
    if (getInfo().onlyConsole() && invocationSource.isPlayer()) {
      invocationSource.sendMessage(Sonar.get().getConfig().getCommands().getConsoleOnly());
      return;
    }

    final CommandInvocation commandInvocation = new CommandInvocation(invocationSource, this, arguments);

    // The subcommands has arguments which are not present in the executed command
    if (getInfo().arguments().length > 0 && arguments.length <= 1) {
      invocationSource.sendMessage(Sonar.get().getConfig().getCommands().getIncorrectCommandUsage()
        .replace("%usage%", getInfo().name() + " (" + getArguments() + ")"));
      return;
    }

    // Execute the sub command
    execute(commandInvocation);
  }

  protected abstract void execute(final @NotNull CommandInvocation invocation);
}
