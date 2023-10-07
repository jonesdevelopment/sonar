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
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.InvocationSource;
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
    InvocationSource verboseSubscriber = invocation.getSender();

    // Support for '/sonar verbose [username]'
    if (invocation.getRawArguments().length >= 2) {
      final Optional<InvocationSource> optional = SONAR.getServer().getOnlinePlayer(invocation.getRawArguments()[1]);
      if (optional.isPresent()) verboseSubscriber = optional.get();
    }

    if (SONAR.getVerboseHandler().isSubscribed(verboseSubscriber.getName())) {
      SONAR.getVerboseHandler().unsubscribe(verboseSubscriber.getName());
      if (verboseSubscriber != invocation.getSender()) {
        verboseSubscriber.sendMessage(SONAR.getConfig().getCommands().getVerboseUnsubscribedOther()
          .replace("%player%", verboseSubscriber.getName()));
      }
      verboseSubscriber.sendMessage(SONAR.getConfig().getCommands().getVerboseUnsubscribed());
      return;
    }

    if (verboseSubscriber != invocation.getSender()) {
      verboseSubscriber.sendMessage(SONAR.getConfig().getCommands().getVerboseSubscribedOther()
        .replace("%player%", verboseSubscriber.getName()));
    }
    verboseSubscriber.sendMessage(SONAR.getConfig().getCommands().getVerboseSubscribed());
    SONAR.getVerboseHandler().subscribe(verboseSubscriber.getName());
  }
}
