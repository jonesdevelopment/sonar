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
          invocation.getSender().sendMessage(SONAR.getConfig().getIncorrectCommandUsage()
            .replace("%usage%", "blacklist add <IP address>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];
        final InetAddress inetAddress = checkIP(invocation.getSender(), rawInetAddress);
        // Make sure the given IP address is valid
        if (inetAddress == null) return;

        // Make sure the IP is not blacklisted already
        if (SONAR.getFallback().getBlacklisted().has(inetAddress.toString())) {
          invocation.getSender().sendMessage(SONAR.getConfig().getBlacklistDuplicate());
          return;
        }

        // Display a warning if the IP is verified but being added to the blacklist
        if (SONAR.getVerifiedPlayerController().has(inetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().getBlacklistAddWarning()
            .replace("%ip%", rawInetAddress));
        }

        // Blacklist the given IP address
        SONAR.getFallback().getBlacklisted().put(inetAddress.toString());
        invocation.getSender().sendMessage(SONAR.getConfig().getBlacklistAdd()
          .replace("%ip%", rawInetAddress));
        break;
      }

      case "remove": {
        if (invocation.getRawArguments().length <= 2) {
          invocation.getSender().sendMessage(SONAR.getConfig().getIncorrectCommandUsage()
            .replace("%usage%", "blacklist remove <IP address>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];
        final InetAddress inetAddress = checkIP(invocation.getSender(), rawInetAddress);
        // Make sure the given IP address is valid
        if (inetAddress == null) return;

        // Make sure the IP is blacklisted
        if (!SONAR.getFallback().getBlacklisted().has(inetAddress.toString())) {
          invocation.getSender().sendMessage(SONAR.getConfig().getBlacklistNotFound());
          return;
        }

        // Invalidate the cache entry of the blacklisted IP address
        SONAR.getFallback().getBlacklisted().invalidate(inetAddress.toString());
        invocation.getSender().sendMessage(SONAR.getConfig().getBlacklistRemove()
          .replace("%ip%", rawInetAddress));
        break;
      }

      case "clear": {
        final int blacklisted = SONAR.getFallback().getBlacklisted().estimatedSize();

        if (blacklisted == 0) {
          invocation.getSender().sendMessage(SONAR.getConfig().getBlacklistEmpty());
          return;
        }

        // Invalidate all cache entries
        SONAR.getFallback().getBlacklisted().invalidateAll();
        invocation.getSender().sendMessage(SONAR.getConfig().getBlacklistCleared()
          .replace("%removed%", Sonar.DECIMAL_FORMAT.format(blacklisted)));
        break;
      }

      case "size": {
        invocation.getSender().sendMessage(SONAR.getConfig().getBlacklistSize()
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
