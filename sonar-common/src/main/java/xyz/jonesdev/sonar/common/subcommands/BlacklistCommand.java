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

package xyz.jonesdev.sonar.common.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SubcommandInfo(
  name = "blacklist",
  arguments = {"add", "remove", "clear", "size"}
)
public final class BlacklistCommand extends Subcommand {

  @Override
  protected void execute(final @NotNull CommandInvocation invocation) {
    switch (invocation.getRawArguments()[1].toLowerCase()) {
      case "add": {
        if (invocation.getRawArguments().length <= 2) {
          incorrectUsage(invocation.getSource(), "blacklist add <IP address>");
          return;
        }

        final String rawAddress = validateIP(invocation.getSource(), invocation.getRawArguments()[2]);
        // Make sure the given IP address is valid
        if (rawAddress == null) return;

        try {
          // Try to resolve the IP address, so we can actually do something with it
          final InetAddress inetAddress = InetAddress.getAllByName(rawAddress)[0];

          // Make sure the IP is not blacklisted already
          if (Sonar.get().getFallback().getBlacklist().asMap().containsKey(inetAddress)) {
            invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
              Sonar.get().getConfig().getMessagesConfig().getString("commands.blacklist.ip-duplicate"),
              Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
              Placeholder.unparsed("ip", rawAddress)));
            return;
          }

          // Display a warning if the IP is verified but being added to the blacklist
          if (Sonar.get().getVerifiedPlayerController().has(rawAddress)) {
            invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
              Sonar.get().getConfig().getMessagesConfig().getString("commands.blacklist.add-warning"),
              Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
              Placeholder.unparsed("ip", rawAddress)));
          }

          // Blacklist the given IP address
          Sonar.get().getFallback().getBlacklist().put(inetAddress, (byte) 0);
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.blacklist.add"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
            Placeholder.unparsed("ip", rawAddress)));
        } catch (UnknownHostException exception) {
          invocation.getSource().sendMessage(Component.text(
            "Unexpected error, check console.", NamedTextColor.RED));
          exception.printStackTrace(System.err);
        }
        break;
      }

      case "remove": {
        if (invocation.getRawArguments().length <= 2) {
          incorrectUsage(invocation.getSource(), "blacklist remove <IP address>");
          return;
        }

        final String rawAddress = validateIP(invocation.getSource(), invocation.getRawArguments()[2]);
        // Make sure the given IP address is valid
        if (rawAddress == null) return;

        try {
          // Try to resolve the IP address, so we can actually do something with it
          final InetAddress inetAddress = InetAddress.getAllByName(rawAddress)[0];

          // Make sure the IP is blacklisted
          if (!Sonar.get().getFallback().getBlacklist().asMap().containsKey(inetAddress)) {
            invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
              Sonar.get().getConfig().getMessagesConfig().getString("commands.blacklist.ip-not-found"),
              Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
            return;
          }

          // Invalidate the cache entry of the blacklisted IP address
          Sonar.get().getFallback().getBlacklist().invalidate(inetAddress);
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.blacklist.remove"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
            Placeholder.unparsed("ip", rawAddress)));
        } catch (UnknownHostException exception) {
          invocation.getSource().sendMessage(Component.text(
            "Unexpected error, check console.", NamedTextColor.RED));
          exception.printStackTrace(System.err);
        }
        break;
      }

      case "clear": {
        final long blacklistSize = Sonar.get().getFallback().getBlacklist().estimatedSize();

        if (blacklistSize == 0) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.blacklist.empty"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        // Invalidate all cache entries
        Sonar.get().getFallback().getBlacklist().invalidateAll();
        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.blacklist.clear"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(blacklistSize))));
        break;
      }

      case "size": {
        final long blacklistSize = Sonar.get().getFallback().getBlacklist().estimatedSize();

        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.blacklist.size"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(blacklistSize))));
        break;
      }

      default: {
        incorrectUsage(invocation.getSource());
        break;
      }
    }
  }
}
