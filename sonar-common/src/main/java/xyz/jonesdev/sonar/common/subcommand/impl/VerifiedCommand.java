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

package xyz.jonesdev.sonar.common.subcommand.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.command.subcommand.argument.Argument;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

@SubcommandInfo(
  name = "verified",
  description = "Manage verified IP addresses",
  arguments = {
    @Argument("history"),
    @Argument("remove"),
    @Argument("clear"),
    @Argument("size")
  }
)
public final class VerifiedCommand extends Subcommand {
  private static final Queue<String> LOCK = new LinkedBlockingQueue<>(10);

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    switch (invocation.getRawArguments()[1].toLowerCase()) {
      case "history": {
        if (invocation.getRawArguments().length <= 2) {
          invocation.getSender().sendMessage(SONAR.getConfig().getIncorrectCommandUsage()
            .replace("%usage%", "verified history <IP address>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];
        if (!IP_PATTERN.matcher(rawInetAddress).matches()) {
          invocation.getSender().sendMessage(SONAR.getConfig().getIncorrectIpAddress());
          return;
        }

        // We don't need to parse this, so we can just use the string.
        final String realInetAddress = "/" + rawInetAddress;

        if (!SONAR.getVerifiedPlayerController().has(realInetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().getVerifiedNotFound());
          return;
        }

        invocation.getSender().sendMessage("§ePrevious UUIDs for " + rawInetAddress + ":");

        for (final UUID uuid : SONAR.getVerifiedPlayerController().getUUIDs(realInetAddress)) {
          invocation.getSender().sendMessage(" §7▪ §f" + uuid.toString());
        }
        break;
      }

      case "remove": {
        if (invocation.getRawArguments().length <= 2) {
          invocation.getSender().sendMessage(SONAR.getConfig().getIncorrectCommandUsage()
            .replace("%usage%", "verified remove <IP address>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];
        if (!IP_PATTERN.matcher(rawInetAddress).matches()) {
          invocation.getSender().sendMessage(SONAR.getConfig().getIncorrectIpAddress());
          return;
        }

        // We don't need to parse this, so we can just use the string.
        final String realInetAddress = "/" + rawInetAddress;

        if (LOCK.contains(realInetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().getVerifiedBlocked());
          return;
        }

        if (!SONAR.getVerifiedPlayerController().has(realInetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().getVerifiedNotFound());
          return;
        }

        // Make sure we don't accidentally run 2 operations at the same time
        LOCK.add(realInetAddress);
        SONAR.getVerifiedPlayerController().remove(realInetAddress);
        LOCK.remove(realInetAddress);

        invocation.getSender().sendMessage(SONAR.getConfig().getVerifiedRemove()
          .replace("%ip%", rawInetAddress));
        break;
      }

      case "clear": {
        final int verified = SONAR.getVerifiedPlayerController().estimatedSize();

        if (verified == 0) {
          invocation.getSender().sendMessage(SONAR.getConfig().getVerifiedEmpty());
          return;
        }

        SONAR.getVerifiedPlayerController().clearAll();
        invocation.getSender().sendMessage(SONAR.getConfig().getVerifiedCleared()
          .replace("%removed%", Sonar.DECIMAL_FORMAT.format(verified)));
        break;
      }

      case "size": {
        invocation.getSender().sendMessage(SONAR.getConfig().getVerifiedSize()
          .replace("%amount%", Sonar.DECIMAL_FORMAT.format(SONAR.getVerifiedPlayerController().estimatedSize())));
        break;
      }

      default: {
        incorrectUsage(invocation.getSender());
        break;
      }
    }
  }
}
