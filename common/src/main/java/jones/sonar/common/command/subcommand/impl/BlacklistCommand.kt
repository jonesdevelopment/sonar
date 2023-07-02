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

import jones.sonar.common.command.CommandInvocation
import jones.sonar.common.command.subcommand.SubCommand
import jones.sonar.common.command.subcommand.SubCommandInfo
import jones.sonar.common.command.subcommand.argument.Argument
import java.net.InetAddress

@SubCommandInfo(
  name = "blacklist",
  description = "Manage blacklisted IP addresses",
  arguments = [
    Argument("add"),
    Argument("remove"),
    Argument("clear"),
    Argument("size"),
  ],
)
class BlacklistCommand : SubCommand() {
  companion object {
    private val IP_REGEX =
      Regex("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])([.,])){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\$")
  }

  override fun execute(invocation: CommandInvocation) {
    when (invocation.arguments[1]!!.lowercase()) {
      "add" -> {
        if (invocation.arguments.size <= 2) {
          invocation.invocationSender.sendMessage(
            sonar.config.INCORRECT_COMMAND_USAGE
              .replace("%usage%", "blacklist add <IP address>")
          )
          return
        }

        val rawInetAddress = invocation.arguments[2]

        if (!rawInetAddress.matches(IP_REGEX)) {
          invocation.invocationSender.sendMessage(sonar.config.INCORRECT_IP_ADDRESS)
          return
        }

        val inetAddress = InetAddress.getByName(rawInetAddress)

        if (inetAddress.isAnyLocalAddress || inetAddress.isLoopbackAddress) {
          invocation.invocationSender.sendMessage(sonar.config.ILLEGAL_IP_ADDRESS)
          return
        }

        synchronized(sonar.fallback.blacklisted) {
          if (sonar.fallback.blacklisted.contains(inetAddress.toString())) {
            invocation.invocationSender.sendMessage(sonar.config.BLACKLIST_DUPLICATE)
            return
          }

          sonar.fallback.blacklisted.add(inetAddress.toString())
          invocation.invocationSender.sendMessage(
            sonar.config.BLACKLIST_ADD
              .replace("%ip%", rawInetAddress)
          )
        }
      }

      "remove" -> {
        if (invocation.arguments.size <= 2) {
          invocation.invocationSender.sendMessage(
            sonar.config.INCORRECT_COMMAND_USAGE
              .replace("%usage%", "blacklist remove <IP address>")
          )
          return
        }

        val rawInetAddress = invocation.arguments[2]

        if (!rawInetAddress.matches(IP_REGEX)) {
          invocation.invocationSender.sendMessage(sonar.config.INCORRECT_IP_ADDRESS)
          return
        }

        val inetAddress = InetAddress.getByName(rawInetAddress)

        if (inetAddress.isAnyLocalAddress || inetAddress.isLoopbackAddress) {
          invocation.invocationSender.sendMessage(sonar.config.ILLEGAL_IP_ADDRESS)
          return
        }

        synchronized(sonar.fallback.blacklisted) {
          if (!sonar.fallback.blacklisted.contains(inetAddress.toString())) {
            invocation.invocationSender.sendMessage(sonar.config.BLACKLIST_NOT_FOUND)
            return
          }

          sonar.fallback.blacklisted.remove(inetAddress.toString())
          invocation.invocationSender.sendMessage(
            sonar.config.BLACKLIST_REMOVE
              .replace("%ip%", rawInetAddress)
          )
        }
      }

      "clear" -> {
        synchronized(sonar.fallback.blacklisted) {
          val blacklisted = sonar.fallback.blacklisted.size

          if (blacklisted == 0) {
            invocation.invocationSender.sendMessage(sonar.config.BLACKLIST_EMPTY)
            return
          }

          sonar.fallback.blacklisted.clear()

          invocation.invocationSender.sendMessage(
            sonar.config.BLACKLIST_CLEARED
              .replace("%removed%", sonar.formatter.format(blacklisted))
          )
        }
      }

      "size" -> {
        invocation.invocationSender.sendMessage(
          sonar.config.BLACKLIST_SIZE
            .replace("%amount%", sonar.formatter.format(sonar.fallback.blacklisted.size))
        )
      }

      else -> incorrectUsage(invocation.invocationSender)
    }
  }
}
