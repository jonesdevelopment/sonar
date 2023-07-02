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
    Argument("update"),
  ]
)
class DatabaseCommand : SubCommand() {
  // use this as a "lock" to prevent players from spamming purge
  private var purging = false

  override fun execute(invocation: CommandInvocation) {
    if (sonar.config.DATABASE == DatabaseType.NONE) {
      invocation.invocationSender.sendMessage(sonar.config.DATABASE_NOT_SELECTED)
      return
    }

    when (invocation.arguments[1]!!.lowercase()) {
      "info" -> {
        invocation.invocationSender.sendMessage()
        invocation.invocationSender.sendMessage(" §eData storage information")
        invocation.invocationSender.sendMessage()
        invocation.invocationSender.sendMessage(" §a▪ §7Current data storage type: §f${sonar.config.DATABASE}")

        when (sonar.config.DATABASE) {
          DatabaseType.MYSQL -> {
            invocation.invocationSender.sendMessage(" §a▪ §7Database URL: §f${sonar.config.DATABASE_URL}")
            invocation.invocationSender.sendMessage(" §a▪ §7Database port: §f${sonar.config.DATABASE_PORT}")
            invocation.invocationSender.sendMessage(" §a▪ §7Database name: §f${sonar.config.DATABASE_NAME}")
            invocation.invocationSender.sendMessage(" §a▪ §7Query limit: §f${sonar.formatter.format(sonar.config.DATABASE_QUERY_LIMIT)}")
          }

          DatabaseType.YAML -> {
            invocation.invocationSender.sendMessage(" §a▪ §7File name: §f${sonar.config.DATABASE_FILE_NAME}.yml")
          }

          else -> throw IllegalStateException("Invalid argument")
        }
        invocation.invocationSender.sendMessage()
      }

      "purge" -> {
        if (invocation.arguments.size == 2) {
          invocation.invocationSender.sendMessage(sonar.config.DATABASE_PURGE_CONFIRM)
          return
        }

        if (purging) {
          invocation.invocationSender.sendMessage(sonar.config.DATABASE_PURGE_ALREADY)
          return
        }

        purging = true
        try {
          sonar.database.purge()
          sonar.reloadDatabases()
        } catch (e: Throwable) {
          e.printStackTrace()
        }
        purging = false

        invocation.invocationSender.sendMessage(sonar.config.DATABASE_PURGE)
      }

      "update" -> {
        val startTime = System.currentTimeMillis()

        invocation.invocationSender.sendMessage(sonar.config.DATABASE_RELOADING)
        sonar.reloadDatabases()

        val timeTaken = System.currentTimeMillis() - startTime;
        invocation.invocationSender.sendMessage(
          sonar.config.DATABASE_RELOADED
            .replace("%taken%", timeTaken.toString())
        )
      }

      else -> incorrectUsage(invocation.invocationSender)
    }
  }
}
