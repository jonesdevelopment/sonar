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
import java.text.DecimalFormat

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

    invocation.invocationSender.sendMessage("§a● §fSonar session statistics")
    invocation.invocationSender.sendMessage()
    invocation.invocationSender.sendMessage(" §7Verified IP addresses §f${decimalFormat.format(verified)}")
    invocation.invocationSender.sendMessage(" §7Verifying IP addresses §f${decimalFormat.format(verifying)}")
    invocation.invocationSender.sendMessage(" §7Blacklisted IP addresses §f${decimalFormat.format(blacklisted)}")
    invocation.invocationSender.sendMessage(" §7Queued connections §f${decimalFormat.format(queued)}")
    invocation.invocationSender.sendMessage(" §7Total traffic §f${decimalFormat.format(total)}")
    invocation.invocationSender.sendMessage()
  }

  companion object {
    private val decimalFormat = DecimalFormat("#,###")
  }
}
