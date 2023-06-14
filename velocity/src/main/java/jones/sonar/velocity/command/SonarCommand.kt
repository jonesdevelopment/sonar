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
import jones.sonar.api.Sonar
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
    // Checking if it contains will only break more since it can throw
    // a NullPointerException if the cache is being accessed from parallel threads
    val timestamp = DELAY.asMap().getOrDefault(invocation.source(), -1L)
    val currentTimestamp = System.currentTimeMillis()

    // There were some exploits with spamming commands in the past
    // Spamming should be prevented especially if some heavy operations are done
    // which is not the case here but let's still stay safe!
    if (timestamp > 0L) {
      invocation.source().sendMessage(Component.text(Sonar.get().config.COMMAND_COOL_DOWN))

      // Format delay
      val left = 0.5 - (currentTimestamp - timestamp.toDouble()) / 1000.0

      invocation.source().sendMessage(Component.text(Sonar.get().config.COMMAND_COOL_DOWN_LEFT
        .replace("%time-left%", decimalFormat.format(left))))
      return
    }

    DELAY.put(invocation.source(), currentTimestamp)

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
      // Search subcommand if command arguments are present
      subCommand = SubCommandManager.getSubCommands().parallelStream()
        .filter { sub: SubCommand ->
          (sub.info.name.equals(invocation.arguments()[0], ignoreCase = true)
            || (sub.info.aliases.isNotEmpty()
            && Arrays.stream(sub.info.aliases)
            .anyMatch { alias: String -> alias.equals(invocation.arguments()[0], ignoreCase = true) }))
        }
        .findFirst()

      // Check permissions for subcommands
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
        invocation.source().sendMessage(Component.text(Sonar.get().config.PLAYERS_ONLY))
        return@ifPresentOrElse
      }

      val commandInvocation = CommandInvocation(
        if (invocation.source() is Player) (invocation.source() as Player).username else "Console",
        invocationSender,
        sub,
        invocation.arguments()
      )

      // The subcommands has arguments which are not present in the executed command
      if (sub.info.arguments.isNotEmpty()
        && commandInvocation.arguments.size <= 1) {
        invocationSender.sendMessage("§fAvailable command arguments for §e/sonar " + sub.info.name + "§f:")
        invocationSender.sendMessage()

        for (argument in sub.info.arguments) {
          invocationSender.sendMessage(
            " §e● §7/sonar "
              + sub.info.name
              + " "
              + argument.name
              + " §f"
              + argument.description
          )
        }

        invocationSender.sendMessage()
        return@ifPresentOrElse
      }

      // Execute the sub command with the custom invocation properties
      sub.execute(commandInvocation)
    }) {
      // No subcommand was found
      CommandHelper.printHelp(invocationSender)
    }
  }

  // Tab completion handling
  override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
    return if (invocation.arguments().size <= 1) {
      if (TAB_SUGGESTIONS.isEmpty()) {
        for (subCommand in SubCommandManager.getSubCommands()) {
          TAB_SUGGESTIONS.add(subCommand.info.name)

          if (subCommand.info.aliases.isNotEmpty()) {
            TAB_SUGGESTIONS.addAll(subCommand.info.aliases)
          }
        }
      }
      return TAB_SUGGESTIONS
    } else Collections.emptyList()
  }

  // Permission handling
  override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
    return invocation.source().hasPermission("sonar.command")
  }

  companion object {
    private val TAB_SUGGESTIONS = ArrayList<String>()
    private val DELAY = Caffeine.newBuilder()
      .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
      .build<CommandSource, Long>()
    private val decimalFormat = DecimalFormat("#.#")
  }
}
