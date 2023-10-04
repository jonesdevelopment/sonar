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

import java.net.InetAddress;
import java.net.UnknownHostException;

@SubcommandInfo(
  name = "blacklist",
  description = "Manage blacklisted IP addresses",
  arguments = {
    @Argument("add"),
    @Argument("remove"),
    @Argument("clear"),
    @Argument("size")
  }
)
public final class BlacklistCommand extends Subcommand {

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    switch (invocation.getRawArguments()[1].toLowerCase()) {
      case "add": {
        if (invocation.getRawArguments().length <= 2) {
          invocation.getSender().sendMessage(SONAR.getConfig().incorrectCommandUsage
            .replace("%usage%", "blacklist add <IP address>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];

        if (!IP_PATTERN.matcher(rawInetAddress).matches()) {
          invocation.getSender().sendMessage(SONAR.getConfig().incorrectIpAddress);
          return;
        }

        final InetAddress inetAddress;
        try {
          inetAddress = InetAddress.getByName(rawInetAddress);

          if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
            invocation.getSender().sendMessage(SONAR.getConfig().illegalIpAddress);
            return;
          }
        } catch (UnknownHostException exception) {
          invocation.getSender().sendMessage(SONAR.getConfig().illegalIpAddress);
          return;
        }

        if (SONAR.getFallback().getBlacklisted().has(inetAddress.toString())) {
          invocation.getSender().sendMessage(SONAR.getConfig().blacklistDuplicate);
          return;
        }

        if (SONAR.getVerifiedPlayerController().has(inetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().blacklistAddWarning
            .replace("%ip%", rawInetAddress));
        }

        SONAR.getFallback().getBlacklisted().put(inetAddress.toString());
        invocation.getSender().sendMessage(SONAR.getConfig().blacklistAdd
          .replace("%ip%", rawInetAddress));
        break;
      }

      case "remove": {
        if (invocation.getRawArguments().length <= 2) {
          invocation.getSender().sendMessage(SONAR.getConfig().incorrectCommandUsage
            .replace("%usage%", "blacklist remove <IP address>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];

        if (!IP_PATTERN.matcher(rawInetAddress).matches()) {
          invocation.getSender().sendMessage(SONAR.getConfig().incorrectIpAddress);
          return;
        }

        final InetAddress inetAddress;
        try {
          inetAddress = InetAddress.getByName(rawInetAddress);

          if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
            invocation.getSender().sendMessage(SONAR.getConfig().illegalIpAddress);
            return;
          }
        } catch (UnknownHostException exception) {
          invocation.getSender().sendMessage(SONAR.getConfig().illegalIpAddress);
          return;
        }

        if (!SONAR.getFallback().getBlacklisted().has(inetAddress.toString())) {
          invocation.getSender().sendMessage(SONAR.getConfig().blacklistNotFound);
          return;
        }

        SONAR.getFallback().getBlacklisted().invalidate(inetAddress.toString());
        invocation.getSender().sendMessage(SONAR.getConfig().blacklistRemove
          .replace("%ip%", rawInetAddress));
        break;
      }

      case "clear": {
        final int blacklisted = SONAR.getFallback().getBlacklisted().estimatedSize();

        if (blacklisted == 0) {
          invocation.getSender().sendMessage(SONAR.getConfig().blacklistEmpty);
          return;
        }

        SONAR.getFallback().getBlacklisted().invalidateAll();

        invocation.getSender().sendMessage(
          SONAR.getConfig().blacklistCleared
            .replace("%removed%", Sonar.DECIMAL_FORMAT.format(blacklisted))
        );
        break;
      }

      case "size": {
        invocation.getSender().sendMessage(SONAR.getConfig().blacklistSize
          .replace("%amount%", Sonar.DECIMAL_FORMAT.format(SONAR.getFallback().getBlacklisted().estimatedSize())));
        break;
      }

      default: {
        incorrectUsage(invocation.getSender());
        break;
      }
    }
  }
}
