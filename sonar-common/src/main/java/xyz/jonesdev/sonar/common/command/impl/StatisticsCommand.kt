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

package xyz.jonesdev.sonar.common.command.impl

import xyz.jonesdev.sonar.api.Sonar
import xyz.jonesdev.sonar.api.command.CommandInvocation
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo

@SubcommandInfo(
  name = "statistics",
  aliases = ["stats"],
  description = "Show session statistics of this server"
)
class StatisticsCommand : Subcommand() {
  override fun execute(invocation: CommandInvocation) {
    val total = sonar.statistics.get("total", 0)
    val queued = sonar.fallback.queue.getQueuedPlayers().size
    val verifying = sonar.fallback.connected.size
    val verified = sonar.fallback.verified.size
    val blacklisted = sonar.fallback.blacklisted.estimatedSize()

    invocation.invocationSender.sendMessage()
    invocation.invocationSender.sendMessage(" §eStatistics (this session)")
    invocation.invocationSender.sendMessage()
    invocation.invocationSender.sendMessage(" §a▪ §7Verified IP addresses: §f${Sonar.DECIMAL_FORMAT.format(verified)}")
    invocation.invocationSender.sendMessage(" §a▪ §7Verifying IP addresses: §f${Sonar.DECIMAL_FORMAT.format(verifying)}")
    invocation.invocationSender.sendMessage(
      " §a▪ §7Blacklisted IP addresses: §f${
        Sonar.DECIMAL_FORMAT.format(
          blacklisted
        )
      }"
    )
    invocation.invocationSender.sendMessage(" §a▪ §7Queued connections: §f${Sonar.DECIMAL_FORMAT.format(queued)}")
    invocation.invocationSender.sendMessage(" §a▪ §7Total connections: §f${Sonar.DECIMAL_FORMAT.format(total)}")
    invocation.invocationSender.sendMessage()
  }
}
