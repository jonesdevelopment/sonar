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

package jones.sonar.common.command.subcommand.impl

import jones.sonar.common.command.CommandInvocation
import jones.sonar.common.command.subcommand.SubCommand
import jones.sonar.common.command.subcommand.SubCommandInfo

@SubCommandInfo(
  name = "version",
  description = "Show version information",
)
class VersionCommand : SubCommand() {
  override fun execute(invocation: CommandInvocation) {
    invocation.invocationSender.sendMessage()
    invocation.invocationSender.sendMessage(" §eVersion information")
    invocation.invocationSender.sendMessage()
    invocation.invocationSender.sendMessage(" §a▪ §7Sonar version: §f${sonar.version.semanticVersion}")
    invocation.invocationSender.sendMessage(" §a▪ §7Commit SHA: §f${sonar.version.commitSHA}")
    invocation.invocationSender.sendMessage(" §a▪ §7Build number: §f${sonar.version.build}")
    invocation.invocationSender.sendMessage(" §a▪ §7Platform: §f${sonar.platform.displayName}")
    invocation.invocationSender.sendMessage()
  }
}
