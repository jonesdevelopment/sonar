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
import xyz.jonesdev.sonar.api.command.argument.Argument
import xyz.jonesdev.sonar.api.command.subcommand.SubCommand
import xyz.jonesdev.sonar.api.command.subcommand.SubCommandInfo
import java.net.InetAddress

@SubCommandInfo(
  name = "whitelist",
  description = "Manage verified IP addresses",
  arguments = [
    Argument("add"),
    Argument("remove"),
    Argument("size"),
  ],
)
class WhitelistCommand : SubCommand() {
  companion object {
    private val IP_REGEX =
      Regex("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])([.])){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\$")
  }

  override fun execute(invocation: CommandInvocation) {
    when (invocation.arguments[1]!!.lowercase()) {
      "add" -> {
        if (invocation.arguments.size <= 2) {
          invocation.invocationSender.sendMessage(
            sonar.config.INCORRECT_COMMAND_USAGE
              .replace("%usage%", "whitelist add <IP address>")
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

        synchronized(sonar.fallback.verified) {
          if (sonar.fallback.verified.contains(inetAddress.toString())) {
            invocation.invocationSender.sendMessage(sonar.config.WHITELIST_DUPLICATE)
            return
          }

          sonar.fallback.verified.add(inetAddress.toString())
          invocation.invocationSender.sendMessage(
            sonar.config.WHITELIST_ADD
              .replace("%ip%", rawInetAddress)
          )
        }
      }

      "remove" -> {
        if (invocation.arguments.size <= 2) {
          invocation.invocationSender.sendMessage(
            sonar.config.INCORRECT_COMMAND_USAGE
              .replace("%usage%", "whitelist remove <IP address>")
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

        synchronized(sonar.fallback.verified) {
          if (!sonar.fallback.verified.contains(inetAddress.toString())) {
            invocation.invocationSender.sendMessage(sonar.config.WHITELIST_NOT_FOUND)
            return
          }

          sonar.fallback.verified.remove(inetAddress.toString())
          invocation.invocationSender.sendMessage(
            sonar.config.WHITELIST_REMOVE
              .replace("%ip%", rawInetAddress)
          )
        }
      }

      "size" -> {
        invocation.invocationSender.sendMessage(
          sonar.config.WHITELIST_SIZE
            .replace("%amount%", Sonar.DECIMAL_FORMAT.format(sonar.fallback.verified.size))
        )
      }

      else -> incorrectUsage(invocation.invocationSender)
    }
  }
}
