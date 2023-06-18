/*
 * Copyright (C) 2023, jones
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

package jones.sonar.common.command.subcommand.impl

import jones.sonar.api.Sonar
import jones.sonar.common.command.CommandInvocation
import jones.sonar.common.command.subcommand.SubCommand
import jones.sonar.common.command.subcommand.SubCommandInfo

@SubCommandInfo(
  name = "statistics",
  aliases = ["stats"],
  description = "Show session statistics of this server"
)
class StatisticsCommand : SubCommand() {
  override fun execute(invocation: CommandInvocation) {
    val total = Sonar.get().statistics.get("total", 0)
    val queued = Sonar.get().fallback.queue.queuedPlayers.size
    val verifying = Sonar.get().fallback.connected.size
    val verified = Sonar.get().fallback.verified.size
    val blacklisted = Sonar.get().fallback.blacklisted.size

    invocation.invocationSender.sendMessage()
    invocation.invocationSender.sendMessage(" §a▪ §7Verified IP addresses: §f${Sonar.get().formatter.format(verified)}")
    invocation.invocationSender.sendMessage(" §a▪ §7Verifying IP addresses: §f${Sonar.get().formatter.format(verifying)}")
    invocation.invocationSender.sendMessage(
      " §a▪ §7Blacklisted IP addresses: §f${
        Sonar.get().formatter.format(
          blacklisted
        )
      }"
    )
    invocation.invocationSender.sendMessage(" §a▪ §7Queued connections: §f${Sonar.get().formatter.format(queued)}")
    invocation.invocationSender.sendMessage(" §a▪ §7Total connections: §f${Sonar.get().formatter.format(total)}")
    invocation.invocationSender.sendMessage()
  }
}
