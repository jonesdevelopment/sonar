/*
 * Copyright (C) 2025 Sonar Contributors
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
import xyz.jonesdev.sonar.api.command.InvocationSource;
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
  protected void execute(final @NotNull InvocationSource source, final String @NotNull [] args) {
    switch (args[1].toLowerCase()) {
      case "remove": {
        if (args.length <= 3) {
          incorrectUsage(source, "verified remove <IP address> <username>");
          return;
        }

        final String hostAddress = validateIP(source, args[2]);
        // Make sure the given IP address is valid
        if (hostAddress == null) return;

        final String username = args[3];
        final String fingerprint = FingerprintingUtil.getFingerprint(username, hostAddress);

        if (!Sonar.get0().getVerifiedPlayerController().getCache().contains(fingerprint)) {
          source.sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get0().getConfig().getMessagesConfig().getString("commands.verified.ip-not-found"),
            Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())));
          return;
        }

        Sonar.get0().getVerifiedPlayerController().remove(fingerprint);
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.verified.remove"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("ip", hostAddress),
          Placeholder.unparsed("username", username)));
        break;
      }

      case "add": {
        if (args.length <= 3) {
          incorrectUsage(source, "verified add <IP address> <username>");
          return;
        }

        final String hostAddress = validateIP(source, args[2]);
        // Make sure the given IP address is valid
        if (hostAddress == null) return;

        final String username = args[3];
        final String fingerprint = FingerprintingUtil.getFingerprint(username, hostAddress);

        if (Sonar.get0().getVerifiedPlayerController().getCache().contains(fingerprint)) {
          source.sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get0().getConfig().getMessagesConfig().getString("commands.verified.already"),
            Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())));
          return;
        }

        Sonar.get0().getVerifiedPlayerController().add(new VerifiedPlayer(fingerprint, System.currentTimeMillis()));
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.verified.add"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("ip", hostAddress),
          Placeholder.unparsed("username", username)));
        break;
      }

      case "clear": {
        final int verifiedSize = Sonar.get0().getVerifiedPlayerController().getCache().size();

        if (verifiedSize == 0) {
          source.sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get0().getConfig().getMessagesConfig().getString("commands.verified.empty"),
            Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())));
          return;
        }

        Sonar.get0().getVerifiedPlayerController().clearAll();
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.verified.clear"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(verifiedSize))));
        break;
      }

      case "size": {
        final int verifiedSize = Sonar.get0().getVerifiedPlayerController().getCache().size();

        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.verified.size"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(verifiedSize))));
        break;
      }

      default: {
        incorrectUsage(source);
        break;
      }
    }
  }
}
