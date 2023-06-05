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

package jones.sonar.velocity.command

import com.github.benmanes.caffeine.cache.Caffeine
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import jones.sonar.common.command.CommandHelper
import jones.sonar.common.command.CommandInvocation
import jones.sonar.common.command.InvocationSender
import jones.sonar.common.command.subcommand.SubCommand
import jones.sonar.common.command.subcommand.SubCommandManager
import net.kyori.adventure.text.Component
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SonarCommand : SimpleCommand {
  override fun execute(invocation: SimpleCommand.Invocation) {
    val timestamp = delay.asMap().getOrDefault(invocation.source(), -1L)

    if (timestamp > 0L) {
      invocation.source().sendMessage(CANNOT_RUN_YET)

      val left = 0.5 - (System.currentTimeMillis() - timestamp.toDouble()) / 1000.0
      val format = decimalFormat.format(left)

      invocation.source().sendMessage(Component.text("§cPlease wait another §l" + format + "s§r§c."))
      return
    }

    delay.put(invocation.source(), System.currentTimeMillis())

    var subCommand = Optional.empty<SubCommand>()

    val invocationSender: InvocationSender<CommandSource?> = object : InvocationSender<CommandSource?> {

      override fun sendMessage(message: String) {
        invocation.source().sendMessage(Component.text(message))
      }

      override fun getPlayer(): CommandSource {
        return invocation.source()
      }
    }

    if (invocation.arguments().isNotEmpty()) {
      subCommand = SubCommandManager.getSubCommands().stream()
        .filter { sub: SubCommand ->
          (sub.info.name.equals(invocation.arguments()[0], ignoreCase = true)
            || (sub.info.aliases.isNotEmpty()
            && Arrays.stream(sub.info.aliases)
            .anyMatch { alias: String -> alias.equals(invocation.arguments()[0], ignoreCase = true) }))
        }
        .findFirst()

      if (subCommand.isPresent) {
        val permission = "sonar." + subCommand.get().info.name

        if (!invocation.source().hasPermission(permission)) {
          invocation.source().sendMessage(
            Component.text(
              "§cYou do not have permission to execute this subcommand. §7($permission)"
            )
          )
          return
        }
      }
    }

    subCommand.ifPresentOrElse({ sub: SubCommand ->
      if (sub.info.onlyPlayers && invocation.source() !is Player) {
        invocation.source().sendMessage(ONLY_PLAYERS)
        return@ifPresentOrElse
      }

      sub.execute(
        CommandInvocation(
          if (invocation.source() is Player) (invocation.source() as Player).username else "Console",
          invocationSender,
          sub,
          invocation.arguments()
        )
      )
    }) { CommandHelper.printHelp(invocationSender) }
  }

  override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
    return invocation.source().hasPermission("sonar.command")
  }

  companion object {
    private val delay = Caffeine.newBuilder()
      .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
      .build<CommandSource, Long>()
    private val ONLY_PLAYERS: Component = Component.text(
      "§cYou can only execute this command as a player."
    )
    private val CANNOT_RUN_YET: Component = Component.text(
      "§cYou can only execute this command every 0.5 seconds."
    )
    private val decimalFormat = DecimalFormat("#.#")
  }
}
