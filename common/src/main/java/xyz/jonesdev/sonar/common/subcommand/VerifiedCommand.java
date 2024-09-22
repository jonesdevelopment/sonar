/*
 * Copyright (C) 2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.subcommand;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.database.model.VerifiedPlayer;
import xyz.jonesdev.sonar.api.fingerprint.FingerprintingUtil;

@SubcommandInfo(
  name = "verified",
  arguments = {"add", "remove", "clear", "size"}
)
public final class VerifiedCommand extends Subcommand {

  @Override
  protected void execute(final @NotNull CommandInvocation invocation) {
    switch (invocation.getRawArguments()[1].toLowerCase()) {
      case "remove": {
        if (invocation.getRawArguments().length <= 3) {
          incorrectUsage(invocation.getSource(), "verified remove <IP address> <username>");
          return;
        }

        final String hostAddress = validateIP(invocation.getSource(), invocation.getRawArguments()[2]);
        // Make sure the given IP address is valid
        if (hostAddress == null) return;

        final String username = invocation.getRawArguments()[3];
        final String fingerprint = FingerprintingUtil.getFingerprint(username, hostAddress);

        if (!Sonar.get().getVerifiedPlayerController().getCache().contains(fingerprint)) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.ip-not-found"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        Sonar.get().getVerifiedPlayerController().remove(fingerprint);
        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.remove"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("ip", hostAddress),
          Placeholder.unparsed("username", username)));
        break;
      }

      case "add": {
        if (invocation.getRawArguments().length <= 3) {
          incorrectUsage(invocation.getSource(), "verified add <IP address> <username>");
          return;
        }

        final String hostAddress = validateIP(invocation.getSource(), invocation.getRawArguments()[2]);
        // Make sure the given IP address is valid
        if (hostAddress == null) return;

        final String username = invocation.getRawArguments()[3];
        final String fingerprint = FingerprintingUtil.getFingerprint(username, hostAddress);

        if (Sonar.get().getVerifiedPlayerController().getCache().contains(fingerprint)) {
          invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.already"),
            Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));
          return;
        }

        Sonar.get().getVerifiedPlayerController().add(new VerifiedPlayer(fingerprint, System.currentTimeMillis()));
        invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("commands.verified.add"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
          Placeholder.unparsed("ip", hostAddress),
          Placeholder.unparsed("username", username)));
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
