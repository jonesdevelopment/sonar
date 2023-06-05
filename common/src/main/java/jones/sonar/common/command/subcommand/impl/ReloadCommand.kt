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
  name = "reload",
  description = "Reload the configuration"
)
class ReloadCommand : SubCommand() {
  override fun execute(invocation: CommandInvocation) {
    val startTime = System.currentTimeMillis()

    invocation.invocationSender.sendMessage(Sonar.get().config.RELOADING)
    Sonar.get().reload()

    val timeTaken = System.currentTimeMillis() - startTime;
    invocation.invocationSender.sendMessage(
      Sonar.get().config.RELOADED
        .replace("%taken%", timeTaken.toString())
    )
  }
}
