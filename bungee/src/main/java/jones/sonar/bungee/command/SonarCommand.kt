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

package jones.sonar.bungee.command

import com.google.common.cache.CacheBuilder
import jones.sonar.api.Sonar
import jones.sonar.common.command.CommandInvocation
import jones.sonar.common.command.InvocationSender
import jones.sonar.common.command.subcommand.SubCommand
import jones.sonar.common.command.subcommand.SubCommandRegistry
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import net.md_5.bungee.command.ConsoleCommandSender
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class SonarCommand : Command("sonar", "sonar.command"), TabExecutor {
  @Suppress("deprecation", "redundantSuppression")
  override fun execute(sender: CommandSender, args: Array<String>) {
    if (sender !is ConsoleCommandSender) {
      // Checking if it contains will only break more since it can throw
      // a NullPointerException if the cache is being accessed from parallel threads
      val timestamp = DELAY.asMap().getOrDefault(sender, -1L)
      val currentTimestamp = System.currentTimeMillis()

      // There were some exploits with spamming commands in the past,
      // Spamming should be prevented, especially if some heavy operations are done,
      // which is not the case here but let's still stay safe!
      if (timestamp > 0L) {
        sender.sendMessage(TextComponent(Sonar.get().config.COMMAND_COOL_DOWN))

        // Format delay
        val left = 0.5 - (currentTimestamp - timestamp.toDouble()) / 1000.0
        sender.sendMessage(
          TextComponent(
            Sonar.get().config.COMMAND_COOL_DOWN_LEFT
              .replace("%time-left%", DECIMAL_FORMAT.format(left))
          )
        )
        return
      }
      DELAY.put(sender, currentTimestamp)
    }

    var subCommand = Optional.empty<SubCommand>()
    val invocationSender = InvocationSender { message -> sender.sendMessage(TextComponent(message)) }

    if (args.isNotEmpty()) {
      // Search subcommand if command arguments are present
      subCommand = SubCommandRegistry.getSubCommands().stream()
        .filter { sub: SubCommand ->
          (sub.info.name.equals(args[0], ignoreCase = true)
            || (sub.info.aliases.isNotEmpty()
            && Arrays.stream(sub.info.aliases)
            .anyMatch { alias: String -> alias.equals(args[0], ignoreCase = true) }))
        }
        .findFirst()

      // Check permissions for subcommands
      if (subCommand.isPresent) {
        if (!subCommand.get().info.onlyConsole
          && !sender.hasPermission(subCommand.get().permission)
        ) {
          invocationSender.sendMessage(
            Sonar.get().config.SUB_COMMAND_NO_PERM
              .replace("%permission%", subCommand.get().permission)
          )
          return
        }
      }
    }

    // No subcommand was found
    if (!subCommand.isPresent) {
      // Re-use the old, cached help message since we don't want to scan
      // for each subcommand and it's arguments/attributes every time
      // someone runs /sonar since the subcommand don't change
      if (CACHED_HELP.isEmpty()) {
        CACHED_HELP.add(EMPTY_TEXT_COMPONENT)
        CACHED_HELP.add(
          TextComponent(
            " §eRunning §lSonar §e"
              + Sonar.get().version
              + " on "
              + Sonar.get().platform.displayName
          )
        )
        CACHED_HELP.add(EMPTY_TEXT_COMPONENT)
        val helpComponent = TextComponent(
          " §7Need help?§b https://jonesdev.xyz/discord/"
        )
        helpComponent.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT,
          ComponentBuilder("§7Click to open Discord").create()
        )
        helpComponent.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, "https://jonesdev.xyz/discord/")
        CACHED_HELP.add(EMPTY_TEXT_COMPONENT)

        SubCommandRegistry.getSubCommands().forEach(Consumer { sub: SubCommand ->
          val component = TextComponent(
            " §a▪ §7/sonar "
              + sub.info.name
              + " §f"
              + sub.info.description
          )

          component.clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sonar " + sub.info.name + " ")
          component.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder(
            "§7Only players: §f" + (if (sub.info.onlyPlayers) "§a✔" else "§c✗")
              + "\n§7Require console: §f" + (if (sub.info.onlyConsole) "§a✔" else "§c✗")
              + "\n§7Permission: §f" + sub.permission
              + "\n§7Aliases: §f" + sub.aliases
          ).create())
          CACHED_HELP.add(component)
        })

        CACHED_HELP.add(EMPTY_TEXT_COMPONENT)
      }

      CACHED_HELP.forEach {
        sender.sendMessage(it)
      }
      return
    }

    // ifPresentOrElse() doesn't exist yet... (version compatibility)
    subCommand.ifPresent {
      if (it.info.onlyPlayers && sender !is ProxiedPlayer) {
        invocationSender.sendMessage(Sonar.get().config.PLAYERS_ONLY)
        return@ifPresent
      }

      if (it.info.onlyConsole && sender is ProxiedPlayer) {
        invocationSender.sendMessage(Sonar.get().config.CONSOLE_ONLY)
        return@ifPresent
      }

      val commandInvocation = CommandInvocation(
        sender.name,
        invocationSender,
        it,
        args
      )

      // The subcommands has arguments which are not present in the executed command
      if (it.info.arguments.isNotEmpty()
        && commandInvocation.arguments.size <= 1
      ) {
        invocationSender.sendMessage(
          Sonar.get().config.INCORRECT_COMMAND_USAGE
            .replace("%usage%", "${it.info.name} (${it.arguments})")
        )
        return@ifPresent
      }

      // Execute the sub command with the custom invocation properties
      it.execute(commandInvocation)
    }
  }

  // Tab completion handling
  override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
    return if (args.size <= 1) {
      if (TAB_SUGGESTIONS.isEmpty()) {
        for (subCommand in SubCommandRegistry.getSubCommands()) {
          TAB_SUGGESTIONS.add(subCommand.info.name)
          if (subCommand.info.aliases.isNotEmpty()) {
            TAB_SUGGESTIONS.addAll(listOf(*subCommand.info.aliases))
          }
        }
      }
      TAB_SUGGESTIONS
    } else if (args.size == 2) {
      if (ARG_TAB_SUGGESTIONS.isEmpty()) {
        for (subCommand in SubCommandRegistry.getSubCommands()) {
          ARG_TAB_SUGGESTIONS[subCommand.info.name] = subCommand.info.arguments
            .map { argument -> argument.name }
            .toList()
        }
      }

      val subCommandName = args[0].lowercase(Locale.getDefault())
      ARG_TAB_SUGGESTIONS.getOrDefault(subCommandName, emptyList())
    } else emptyList()
  }

  companion object {
    private val DELAY = CacheBuilder.newBuilder()
      .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
      .build<CommandSender, Long>()
    private val DECIMAL_FORMAT = DecimalFormat("#.##")
    private val TAB_SUGGESTIONS: MutableList<String> = ArrayList()
    private val ARG_TAB_SUGGESTIONS: MutableMap<String, List<String>> = HashMap()
    private val CACHED_HELP = Vector<TextComponent>()
    private val EMPTY_TEXT_COMPONENT = TextComponent(" ")
  }
}
