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

import java.net.InetAddress;
import java.net.UnknownHostException;

@SubcommandInfo(
  name = "whitelist",
  description = "Manage whitelisted IP addresses",
  arguments = {
    @Argument("add"),
    @Argument("remove"),
    @Argument("size")
  }
)
public final class WhitelistCommand extends Subcommand {

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    switch (invocation.getArguments()[1].toLowerCase()) {
      case "add": {
        if (invocation.getArguments().length <= 2) {
          invocation.getSender().sendMessage(
            SONAR.getConfig().INCORRECT_COMMAND_USAGE
              .replace("%usage%", "whitelist add <IP address>")
          );
          return;
        }

        final String rawInetAddress = invocation.getArguments()[2];

        if (!IP_PATTERN.matcher(rawInetAddress).matches()) {
          invocation.getSender().sendMessage(SONAR.getConfig().INCORRECT_IP_ADDRESS);
          return;
        }

        final InetAddress inetAddress;
        try {
          inetAddress = InetAddress.getByName(rawInetAddress);

          if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
            invocation.getSender().sendMessage(SONAR.getConfig().ILLEGAL_IP_ADDRESS);
            return;
          }
        } catch (UnknownHostException exception) {
          invocation.getSender().sendMessage(SONAR.getConfig().ILLEGAL_IP_ADDRESS);
          return;
        }

        if (SONAR.getFallback().getVerified().contains(inetAddress.toString())) {
          invocation.getSender().sendMessage(SONAR.getConfig().WHITELIST_DUPLICATE);
          return;
        }

        SONAR.getFallback().getVerified().add(inetAddress.toString());
        invocation.getSender().sendMessage(
          SONAR.getConfig().WHITELIST_ADD
            .replace("%ip%", rawInetAddress)
        );
        break;
      }

      case "remove": {
        if (invocation.getArguments().length <= 2) {
          invocation.getSender().sendMessage(
            SONAR.getConfig().INCORRECT_COMMAND_USAGE
              .replace("%usage%", "whitelist remove <IP address>")
          );
          return;
        }

        final String rawInetAddress = invocation.getArguments()[2];

        if (!IP_PATTERN.matcher(rawInetAddress).matches()) {
          invocation.getSender().sendMessage(SONAR.getConfig().INCORRECT_IP_ADDRESS);
          return;
        }

        final InetAddress inetAddress;
        try {
          inetAddress = InetAddress.getByName(rawInetAddress);

          if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
            invocation.getSender().sendMessage(SONAR.getConfig().ILLEGAL_IP_ADDRESS);
            return;
          }
        } catch (UnknownHostException exception) {
          invocation.getSender().sendMessage(SONAR.getConfig().ILLEGAL_IP_ADDRESS);
          return;
        }

        if (!SONAR.getFallback().getVerified().contains(inetAddress.toString())) {
          invocation.getSender().sendMessage(SONAR.getConfig().WHITELIST_NOT_FOUND);
          return;
        }

        SONAR.getFallback().getVerified().remove(inetAddress.toString());
        invocation.getSender().sendMessage(
          SONAR.getConfig().WHITELIST_REMOVE
            .replace("%ip%", rawInetAddress)
        );
        break;
      }

      case "size": {
        invocation.getSender().sendMessage(
          SONAR.getConfig().WHITELIST_SIZE
            .replace("%amount%", Sonar.DECIMAL_FORMAT.format(SONAR.getFallback().getVerified().size()))
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
