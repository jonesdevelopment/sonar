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
import java.net.InetAddress

@SubCommandInfo(
  name = "blacklist", description = "Manage blacklisted IP addresses"
)
class BlacklistCommand : SubCommand() {
  companion object {
    private val IP_REGEX =
      Regex("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])([.,])){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\$")
  }

  override fun execute(invocation: CommandInvocation) {
    if (invocation.arguments.size <= 1) {
      invocation.invocationSender.sendMessage("§a● §fSonar blacklist management")
      invocation.invocationSender.sendMessage()
      invocation.invocationSender.sendMessage(" §f/sonar blacklist add <IP address> §7Add an IP to the blacklist")
      invocation.invocationSender.sendMessage(" §f/sonar blacklist remove <IP address> §7Remove an IP from the blacklist")
      invocation.invocationSender.sendMessage(" §f/sonar blacklist clear §7Remove all IP addresses from the blacklist")
      invocation.invocationSender.sendMessage(" §f/sonar blacklist size §7View the current size of the blacklist")
      invocation.invocationSender.sendMessage()
      return
    }

    when (invocation.arguments[1]!!.lowercase()) {
      "add" -> {
        if (invocation.arguments.size <= 2) {
          invocation.invocationSender.sendMessage(
            Sonar.get().config.INCORRECT_COMMAND_USAGE
              .replace("%subcommand%", "blacklist add <IP address>")
          )
          return
        }

        val rawInetAddress = invocation.arguments[2]

        if (!rawInetAddress.matches(IP_REGEX)) {
          invocation.invocationSender.sendMessage(Sonar.get().config.INCORRECT_IP_ADDRESS)
          return
        }

        val inetAddress = InetAddress.getByName(rawInetAddress)

        if (inetAddress.isAnyLocalAddress || inetAddress.isLoopbackAddress) {
          invocation.invocationSender.sendMessage(Sonar.get().config.ILLEGAL_IP_ADDRESS)
          return
        }

        synchronized(Sonar.get().fallback.blacklisted) {
          if (Sonar.get().fallback.blacklisted.contains(inetAddress)) {
            invocation.invocationSender.sendMessage(Sonar.get().config.BLACKLIST_DUPLICATE)
            return
          }

          Sonar.get().fallback.blacklisted.add(inetAddress)
          invocation.invocationSender.sendMessage(
            Sonar.get().config.BLACKLIST_ADD
              .replace("%ip%", rawInetAddress)
          )
        }
      }

      "remove" -> {
        if (invocation.arguments.size <= 2) {
          invocation.invocationSender.sendMessage(
            Sonar.get().config.INCORRECT_COMMAND_USAGE
              .replace("%subcommand%", "blacklist remove <IP address>")
          )
          return
        }

        val rawInetAddress = invocation.arguments[2]

        if (!rawInetAddress.matches(IP_REGEX)) {
          invocation.invocationSender.sendMessage(Sonar.get().config.INCORRECT_IP_ADDRESS)
          return
        }

        val inetAddress = InetAddress.getByName(rawInetAddress)

        if (inetAddress.isAnyLocalAddress || inetAddress.isLoopbackAddress) {
          invocation.invocationSender.sendMessage(Sonar.get().config.ILLEGAL_IP_ADDRESS)
          return
        }

        synchronized(Sonar.get().fallback.blacklisted) {
          if (!Sonar.get().fallback.blacklisted.contains(inetAddress)) {
            invocation.invocationSender.sendMessage(Sonar.get().config.BLACKLIST_NOT_FOUND)
            return
          }

          Sonar.get().fallback.blacklisted.remove(inetAddress)
          invocation.invocationSender.sendMessage(
            Sonar.get().config.BLACKLIST_REMOVE
              .replace("%ip%", rawInetAddress)
          )
        }
      }

      "clear" -> {
        synchronized(Sonar.get().fallback.blacklisted) {
          val blacklisted = Sonar.get().fallback.blacklisted.size

          if (blacklisted == 0) {
            invocation.invocationSender.sendMessage(Sonar.get().config.BLACKLIST_EMPTY)
            return
          }

          Sonar.get().fallback.blacklisted.clear()

          invocation.invocationSender.sendMessage(
            Sonar.get().config.BLACKLIST_CLEARED
              .replace("%removed%", Sonar.get().formatter.format(blacklisted))
          )
        }
      }

      "size" -> {
        invocation.invocationSender.sendMessage(
          Sonar.get().config.BLACKLIST_SIZE
            .replace("%amount%", Sonar.get().formatter.format(Sonar.get().fallback.blacklisted.size))
        )
      }

      else -> invocation.invocationSender.sendMessage(
        Sonar.get().config.INCORRECT_COMMAND_ARG
          .replace("%subcommand%", "blacklist")
      )
    }
  }
}
