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

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
          incorrectUsage(invocation.getSource(), "verified history <IP address>");
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];
        final InetAddress inetAddress = getInetAddressIfValid(invocation.getSource(), rawInetAddress);
        // Make sure the given IP address is valid
        if (inetAddress == null) return;

        // Make sure the IP is verified already
        if (!Sonar.get().getVerifiedPlayerController().has(inetAddress.toString())) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.ip-not-found"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.history"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("ip", rawInetAddress)));

        for (final UUID uuid : Sonar.get().getVerifiedPlayerController().getUUIDs(inetAddress.toString())) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.history-entry"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
            Placeholder.unparsed("uuid", uuid.toString())));
        }
        break;
      }

      case "remove": {
        if (invocation.getRawArguments().length <= 2) {
          incorrectUsage(invocation.getSource(), "verified remove <IP address>");
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];

        // Make sure we aren't currently locking the IP address to avoid a double I/O operation
        if (LOCK.contains(rawInetAddress)) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.blocked"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        final InetAddress inetAddress = getInetAddressIfValid(invocation.getSource(), rawInetAddress);
        // Make sure the given IP address is valid
        if (inetAddress == null) return;

        // Make sure the player is verified already
        if (!Sonar.get().getVerifiedPlayerController().has(inetAddress.toString())) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.ip-not-found"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        // Lock the IP address
        // Make sure we don't accidentally run 2 operations at the same time
        LOCK.add(rawInetAddress);
        Sonar.get().getVerifiedPlayerController().remove(inetAddress.toString());
        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.remove"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("ip", rawInetAddress)));

        // Unlock the IP address
        LOCK.remove(rawInetAddress);
        break;
      }

      case "add": {
        if (invocation.getRawArguments().length <= 3) {
          incorrectUsage(invocation.getSource(), "verified add <IP address> <UUID/username>");
          return;
        }

        final String rawInetAddress = invocation.getRawArguments()[2];

        // Make sure we aren't currently locking the IP address to avoid a double I/O operation
        if (LOCK.contains(rawInetAddress)) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.blocked"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        final InetAddress inetAddress = getInetAddressIfValid(invocation.getSource(), rawInetAddress);
        // Make sure the given IP address is valid
        if (inetAddress == null) return;

        // Try to parse the UUID (from the username, if needed)
        final String rawUUID = invocation.getRawArguments()[3];
        final UUID uuid = rawUUID.length() == 36 ? UUID.fromString(rawUUID)
          : UUID.nameUUIDFromBytes(("OfflinePlayer:" + rawUUID).getBytes(StandardCharsets.UTF_8));

        // Make sure the player is verified already
        if (Sonar.get().getVerifiedPlayerController().has(inetAddress.toString(), uuid)) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.already"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        // Lock the IP address
        // Make sure we don't accidentally run 2 operations at the same time
        LOCK.add(rawInetAddress);
        // Add verified player to the database
        final long timestamp = System.currentTimeMillis();
        Sonar.get().getVerifiedPlayerController().add(new VerifiedPlayer(inetAddress, uuid, timestamp));

        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.add"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("ip", rawInetAddress)));
        // Unlock the IP address
        LOCK.remove(rawInetAddress);
        break;
      }

      case "clear": {
        final int verifiedSize = Sonar.get().getVerifiedPlayerController().estimatedSize();

        if (verifiedSize == 0) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.empty"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        // Invalidate all cache entries
        Sonar.get().getVerifiedPlayerController().clearAll();
        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.clear"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(verifiedSize))));
        break;
      }

      case "size": {
        final int verifiedSize = Sonar.get().getVerifiedPlayerController().estimatedSize();

        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.size"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(verifiedSize))));
        break;
      }

      default: {
        incorrectUsage(invocation.getSource());
        break;
      }
    }
  }
}
