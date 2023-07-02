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

import jones.sonar.api.database.DatabaseType
import jones.sonar.common.command.CommandInvocation
import jones.sonar.common.command.subcommand.SubCommand
import jones.sonar.common.command.subcommand.SubCommandInfo
import jones.sonar.common.command.subcommand.argument.Argument

@SubCommandInfo(
  name = "database",
  description = "Data storage management",
  arguments = [
    Argument("info"),
    Argument("purge"),
  ]
)
class DatabaseCommand : SubCommand() {
  override fun execute(invocation: CommandInvocation) {
    when (invocation.arguments[1]!!.lowercase()) {
      "info" -> {
        invocation.invocationSender.sendMessage()
        invocation.invocationSender.sendMessage(" §eData storage information")
        invocation.invocationSender.sendMessage()
        invocation.invocationSender.sendMessage(" §a▪ §7Current database type: §f" + sonar.config.DATABASE)

        when (sonar.config.DATABASE) {
          DatabaseType.MYSQL -> {
            invocation.invocationSender.sendMessage(" §a▪ §7Database URL: " + sonar.config.DATABASE_URL)
            invocation.invocationSender.sendMessage(" §a▪ §7Database name: " + sonar.config.DATABASE_NAME)
            invocation.invocationSender.sendMessage(" §a▪ §7Query limit: " + sonar.formatter.format(sonar.config.DATABASE_QUERY_LIMIT))
          }

          DatabaseType.YAML -> {
            invocation.invocationSender.sendMessage(" §a▪ §7File name: " + sonar.config.DATABASE_FILE_NAME)
          }

          else -> {
            invocation.invocationSender.sendMessage(" §c▪ §7You haven't selected any database yet")
          }
        }
        invocation.invocationSender.sendMessage()
      }

      "purge" -> {
        println(invocation.arguments.size)
      }

      else -> incorrectUsage(invocation.invocationSender)
    }
  }
}
