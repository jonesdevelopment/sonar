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

import xyz.jonesdev.sonar.api.database.DatabaseType
import xyz.jonesdev.sonar.api.format.MemoryFormatter
import xyz.jonesdev.sonar.common.command.CommandInvocation
import xyz.jonesdev.sonar.common.command.subcommand.SubCommand
import xyz.jonesdev.sonar.common.command.subcommand.SubCommandInfo
import xyz.jonesdev.sonar.common.command.subcommand.argument.Argument
import java.io.File
import java.nio.file.Files

@SubCommandInfo(
  name = "database",
  description = "Data storage management",
  arguments = [
    Argument("info"),
    Argument("purge"),
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
            val file = File(sonar.pluginDataFolder, sonar.config.DATABASE_FILE_NAME + ".yml")

            if (!file.exists()) {
              invocation.invocationSender.sendMessage(" §c▪ §7File does not exist?!")
              return
            }

            invocation.invocationSender.sendMessage(" §a▪ §7File name: §f${sonar.config.DATABASE_FILE_NAME}.yml")

            val fileSize = Files.size(file.toPath())
            invocation.invocationSender.sendMessage(" §a▪ §7File size: §f${MemoryFormatter.formatMemory(fileSize)}")
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

        // This is a security feature
        if (!sonar.config.ALLOW_PURGING) {
          invocation.invocationSender.sendMessage(sonar.config.DATABASE_PURGE_DISALLOWED)
          return
        }

        if (purging) {
          invocation.invocationSender.sendMessage(sonar.config.DATABASE_PURGE_ALREADY)
          return
        }

        purging = true
        try {
          sonar.database.purge()

          sonar.reload()
          invocation.invocationSender.sendMessage(sonar.config.DATABASE_PURGE)
        } catch (e: Throwable) {
          e.printStackTrace()
        }
        purging = false
      }

      else -> incorrectUsage(invocation.invocationSender)
    }
  }
}
