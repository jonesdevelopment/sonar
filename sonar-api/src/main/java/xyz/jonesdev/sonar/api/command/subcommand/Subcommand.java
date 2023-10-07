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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public abstract class Subcommand {
  private final @NotNull SubcommandInfo info;
  private final String permission, aliases, arguments;
  protected static final Sonar SONAR = Sonar.get();
  protected static final Pattern IP_PATTERN = Pattern.compile(
    "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])([.])){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
  );

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

  protected static @Nullable InetAddress checkIP(final InvocationSource source, final String rawIP) {
    if (!IP_PATTERN.matcher(rawIP).matches()) {
      source.sendMessage(SONAR.getConfig().getIncorrectIpAddress());
      return null;
    }

    final InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByName(rawIP);

      if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
        source.sendMessage(SONAR.getConfig().getIllegalIpAddress());
        return null;
      }
    } catch (UnknownHostException exception) {
      source.sendMessage(SONAR.getConfig().getUnknownIpAddress());
      return null;
    }
    return inetAddress;
  }

  protected final void incorrectUsage(final @NotNull InvocationSource sender) {
    sender.sendMessage(SONAR.getConfig().getIncorrectCommandUsage()
      .replace("%usage%", info.name() + " (" + arguments + ")"));
  }

  public abstract void execute(final @NotNull CommandInvocation invocation);
}
