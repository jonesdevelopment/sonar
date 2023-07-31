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

package xyz.jonesdev.sonar.common.command.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.argument.Argument;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@SubcommandInfo(
  name = "verified",
  description = "Manage verified IP addresses",
  arguments = {
    @Argument("remove"),
    @Argument("clear"),
    @Argument("size")
  }
)
public final class VerifiedCommand extends Subcommand {
  private static final Queue<String> LOCK = new LinkedBlockingQueue<>(10);

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    switch (invocation.getArguments()[1].toLowerCase()) {
      case "remove": {
        if (invocation.getArguments().length <= 2) {
          invocation.getSender().sendMessage(
            SONAR.getConfig().INCORRECT_COMMAND_USAGE
              .replace("%usage%", "verified remove <IP address>")
          );
          return;
        }

        final String inetAddress = invocation.getArguments()[2];

        if (!IP_PATTERN.matcher(inetAddress).matches()) {
          invocation.getSender().sendMessage(SONAR.getConfig().INCORRECT_IP_ADDRESS);
          return;
        }

        // We don't need to parse this, so we can just use the string.
        final String realInetAddress = "/" + inetAddress;

        if (LOCK.contains(realInetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().COMMAND_COOL_DOWN);
          return;
        }

        if (!SONAR.getVerifiedPlayerController().has(realInetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().VERIFIED_NOT_FOUND);
          return;
        }

        // Make sure we don't accidentally run 2 operations at the same time
        LOCK.add(realInetAddress);
        SONAR.getVerifiedPlayerController().remove(realInetAddress);
        LOCK.remove(realInetAddress);

        invocation.getSender().sendMessage(
          SONAR.getConfig().VERIFIED_REMOVE
            .replace("%ip%", inetAddress)
        );
        break;
      }

      case "clear": {
        final int verified = SONAR.getVerifiedPlayerController().estimatedSize();

        if (verified == 0) {
          invocation.getSender().sendMessage(SONAR.getConfig().VERIFIED_EMPTY);
          return;
        }

        SONAR.getVerifiedPlayerController().clearAll();

        invocation.getSender().sendMessage(
          SONAR.getConfig().VERIFIED_CLEARED
            .replace("%removed%", Sonar.DECIMAL_FORMAT.format(verified))
        );
        break;
      }

      case "size": {
        invocation.getSender().sendMessage(
          SONAR.getConfig().VERIFIED_SIZE
            .replace("%amount%", Sonar.DECIMAL_FORMAT.format(SONAR.getVerifiedPlayerController().estimatedSize()))
        );
        break;
      }

      default: {
        incorrectUsage(invocation.getSender());
        break;
      }
    }
  }
}
