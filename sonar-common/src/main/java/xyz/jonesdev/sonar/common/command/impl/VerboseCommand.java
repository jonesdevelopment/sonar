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
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.InvocationSender;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

import java.util.Optional;

@SubcommandInfo(
  name = "verbose",
  description = "Enable and disable Sonar verbose",
  onlyPlayers = true
)
public final class VerboseCommand extends Subcommand {

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    InvocationSender verboseSubscriber = invocation.getSender();

    // Support for '/sonar verbose [username]'
    if (invocation.getArguments().length >= 2) {
      final Optional<InvocationSender> optional = SONAR.getServer().getOnlinePlayer(invocation.getArguments()[1]);
      if (optional.isPresent()) verboseSubscriber = optional.get();
    }

    if (SONAR.getActionBarVerbose().isSubscribed(verboseSubscriber.getName())) {
      SONAR.getActionBarVerbose().unsubscribe(verboseSubscriber.getName());
      if (verboseSubscriber != invocation.getSender()) {
        verboseSubscriber.sendMessage(
          SONAR.getConfig().VERBOSE_UNSUBSCRIBED_OTHER
            .replace("%player%", verboseSubscriber.getName())
        );
      }
      verboseSubscriber.sendMessage(SONAR.getConfig().VERBOSE_UNSUBSCRIBED);
      return;
    }

    if (verboseSubscriber != invocation.getSender()) {
      verboseSubscriber.sendMessage(
        SONAR.getConfig().VERBOSE_SUBSCRIBED_OTHER
          .replace("%player%", verboseSubscriber.getName())
      );
    }
    verboseSubscriber.sendMessage(SONAR.getConfig().VERBOSE_SUBSCRIBED);
    SONAR.getActionBarVerbose().subscribe(verboseSubscriber.getName());
  }
}
