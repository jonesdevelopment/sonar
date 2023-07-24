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

package xyz.jonesdev.sonar.common.command.subcommand.impl

import xyz.jonesdev.sonar.api.Sonar
import xyz.jonesdev.sonar.common.command.CommandInvocation
import xyz.jonesdev.sonar.common.command.subcommand.SubCommand
import xyz.jonesdev.sonar.common.command.subcommand.SubCommandInfo

@SubCommandInfo(
  name = "verbose",
  description = "Enable and disable Sonar verbose",
  onlyPlayers = true
)
class VerboseCommand : SubCommand() {
  override fun execute(invocation: CommandInvocation) {
    var verboseSubscriber = invocation.invocationSender

    // Support for '/sonar verbose [username]'
    if (invocation.arguments.size >= 2) {
      val optional = Sonar.get().server.getOnlinePlayer(invocation.arguments[1])
      if (optional.isPresent) verboseSubscriber = optional.get()
    }

    if (sonar.actionBarVerbose.isSubscribed(verboseSubscriber.name)) {
      sonar.actionBarVerbose.unsubscribe(verboseSubscriber.name)
      if (verboseSubscriber != invocation.invocationSender) {
        verboseSubscriber.sendMessage(
          sonar.config.VERBOSE_UNSUBSCRIBED_OTHER
            .replace("%player%", verboseSubscriber.name)
        )
      }
      verboseSubscriber.sendMessage(sonar.config.VERBOSE_UNSUBSCRIBED)
      return
    }

    if (verboseSubscriber != invocation.invocationSender) {
      verboseSubscriber.sendMessage(
        sonar.config.VERBOSE_SUBSCRIBED_OTHER
          .replace("%player%", verboseSubscriber.name)
      )
    }
    verboseSubscriber.sendMessage(sonar.config.VERBOSE_SUBSCRIBED)
    sonar.actionBarVerbose.subscribe(verboseSubscriber.name)
  }
}
