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

package xyz.jonesdev.sonar.common.subcommand.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.command.subcommand.argument.Argument;
import xyz.jonesdev.sonar.api.model.VerifiedPlayer;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

@SubcommandInfo(
  name = "verified",
  description = "Manage verified IP addresses",
  arguments = {
    @Argument("history"),
    @Argument("remove"),
    @Argument("add"),
    @Argument("clear"),
    @Argument("size")
  }
)
public final class VerifiedCommand extends Subcommand {
  private static final Queue<String> LOCK = new LinkedBlockingQueue<>(1);

  @Override
  protected void execute(final @NotNull CommandInvocation invocation) {
    switch (invocation.getRawArguments()[1].toLowerCase()) {
      case "history": {
        if (invocation.getRawArguments().length <= 2) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getIncorrectCommandUsage()
            .replace("%usage%", "verified history <IP address>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];
        final InetAddress inetAddress = getInetAddressIfValid(invocation.getSender(), rawInetAddress);
        // Make sure the given IP address is valid
        if (inetAddress == null) return;

        // Make sure the IP is verified already
        if (!SONAR.getVerifiedPlayerController().has(inetAddress.toString())) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedNotFound());
          return;
        }

        invocation.getSender().sendMessage("<yellow>Previous UUIDs for " + rawInetAddress + ":");
        for (final UUID uuid : SONAR.getVerifiedPlayerController().getUUIDs(inetAddress.toString())) {
          invocation.getSender().sendMessage(" <gray>▪ <white>" + uuid.toString());
        }
        break;
      }

      case "remove": {
        if (invocation.getRawArguments().length <= 2) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getIncorrectCommandUsage()
            .replace("%usage%", "verified remove <IP address>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];

        // Make sure we aren't currently locking the IP address to avoid a double I/O operation
        if (LOCK.contains(rawInetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedBlocked());
          return;
        }

        final InetAddress inetAddress = getInetAddressIfValid(invocation.getSender(), rawInetAddress);
        // Make sure the given IP address is valid
        if (inetAddress == null) return;

        // Make sure the player is verified already
        if (!SONAR.getVerifiedPlayerController().has(inetAddress.toString())) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedNotFound());
          return;
        }

        // Lock the IP address
        // Make sure we don't accidentally run 2 operations at the same time
        LOCK.add(rawInetAddress);
        SONAR.getVerifiedPlayerController().remove(inetAddress.toString());

        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedRemove()
          .replace("%ip%", rawInetAddress));
        // Unlock the IP address
        LOCK.remove(rawInetAddress);
        break;
      }

      case "add": {
        if (invocation.getRawArguments().length <= 3) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getIncorrectCommandUsage()
            .replace("%usage%", "verified add <IP address> <UUID/username>"));
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];

        // Make sure we aren't currently locking the IP address to avoid a double I/O operation
        if (LOCK.contains(rawInetAddress)) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedBlocked());
          return;
        }

        final InetAddress inetAddress = getInetAddressIfValid(invocation.getSender(), rawInetAddress);
        // Make sure the given IP address is valid
        if (inetAddress == null) return;

        // Try to parse the UUID (from the username, if needed)
        final String rawUUID = invocation.getRawArguments()[3];
        final UUID uuid = rawUUID.length() == 36 ? UUID.fromString(rawUUID)
          : UUID.nameUUIDFromBytes(("OfflinePlayer:" + rawUUID).getBytes(StandardCharsets.UTF_8));

        // Make sure the player is verified already
        if (SONAR.getVerifiedPlayerController().has(inetAddress.toString(), uuid)) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedAlready());
          return;
        }

        // Lock the IP address
        // Make sure we don't accidentally run 2 operations at the same time
        LOCK.add(rawInetAddress);
        // Add verified player to the database
        final long timestamp = System.currentTimeMillis();
        Sonar.get().getVerifiedPlayerController().add(new VerifiedPlayer(inetAddress, uuid, timestamp));

        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedAdd()
          .replace("%ip%", rawInetAddress));
        // Unlock the IP address
        LOCK.remove(rawInetAddress);
        break;
      }

      case "clear": {
        final int verified = SONAR.getVerifiedPlayerController().estimatedSize();

        if (verified == 0) {
          invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedEmpty());
          return;
        }

        // Invalidate all cache entries
        SONAR.getVerifiedPlayerController().clearAll();
        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedCleared()
          .replace("%removed%", Sonar.DECIMAL_FORMAT.format(verified)));
        break;
      }

      case "size": {
        invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getVerifiedSize()
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
