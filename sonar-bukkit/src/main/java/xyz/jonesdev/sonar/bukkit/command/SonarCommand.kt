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

package xyz.jonesdev.sonar.bukkit.command

import com.google.common.cache.CacheBuilder
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.*
import org.bukkit.entity.Player
import xyz.jonesdev.sonar.api.Sonar
import xyz.jonesdev.sonar.api.command.InvocationSender
import xyz.jonesdev.sonar.common.command.CommandInvocation
import xyz.jonesdev.sonar.common.command.subcommand.SubCommand
import xyz.jonesdev.sonar.common.command.subcommand.SubCommandRegistry
import xyz.jonesdev.sonar.common.command.subcommand.argument.Argument
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.stream.Collectors

@Suppress("unstableApiUsage")
class SonarCommand : CommandExecutor, TabExecutor {
  override fun onCommand(
    sender: CommandSender,
    command: Command,
    label: String,
    args: Array<String>
  ): Boolean {
    if (sender !is ConsoleCommandSender) {
      // Checking if it contains will only break more since it can throw
      // a NullPointerException if the cache is being accessed from parallel threads
      val timestamp = DELAY.asMap().getOrDefault(sender, -1L)
      val currentTimestamp = System.currentTimeMillis()

      // There were some exploits with spamming commands in the past,
      // Spamming should be prevented, especially if some heavy operations are done,
      // which is not the case here but let's still stay safe!
      if (timestamp > 0L) {
        sender.sendMessage(Sonar.get().config.COMMAND_COOL_DOWN)

        // Format delay
        val left = 0.5 - (currentTimestamp - timestamp.toDouble()) / 1000.0
        sender.sendMessage(
          Sonar.get().config.COMMAND_COOL_DOWN_LEFT
            .replace("%time-left%", DECIMAL_FORMAT.format(left))
        )
        return false
      }
      DELAY.put(sender, currentTimestamp)
    }

    var subCommand = Optional.empty<SubCommand>()
    val invocationSender = object : InvocationSender {
      override fun getName(): String {
        return sender.name
      }

      override fun sendMessage(message: String) {
        sender.sendMessage(message)
      }
    }

    if (args.isNotEmpty()) {
      // Search subcommand if command arguments are present
      subCommand = SubCommandRegistry.getSubCommands().stream()
        .filter { sub: SubCommand ->
          (sub.info.name.equals(args[0], true)
            || (sub.info.aliases.isNotEmpty()
            && Arrays.stream(sub.info.aliases)
            .anyMatch { alias: String -> alias.equals(args[0], true) }))
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
          return false
        }
      }
    }

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
              + Sonar.get().server.platform.displayName
          )
        )
        CACHED_HELP.add(EMPTY_TEXT_COMPONENT)

        val helpComponent = TextComponent(" §7Need help?§b https://jonesdev.xyz/discord/")
        helpComponent.hoverEvent = HoverEvent(
          HoverEvent.Action.SHOW_TEXT,
          ComponentBuilder("§7Click to open Discord").create()
        )
        helpComponent.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, "https://jonesdev.xyz/discord/")
        CACHED_HELP.add(helpComponent)
        CACHED_HELP.add(EMPTY_TEXT_COMPONENT)

        SubCommandRegistry.getSubCommands().forEach(Consumer { sub: SubCommand ->
          val component = TextComponent(
            " §a▪ §7/sonar "
              + sub.info.name
              + " §f"
              + sub.info.description
          )

          component.clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sonar " + sub.info.name + " ")
          component.hoverEvent = HoverEvent(
            HoverEvent.Action.SHOW_TEXT, ComponentBuilder(
              "§7Only players: §f" + (if (sub.info.onlyPlayers) "§a✔" else "§c✗")
                + "\n§7Require console: §f" + (if (sub.info.onlyConsole) "§a✔" else "§c✗")
                + "\n§7Permission: §f" + sub.permission
                + "\n§7Aliases: §f" + sub.aliases
            ).create()
          )
          CACHED_HELP.add(component)
        })

        CACHED_HELP.add(EMPTY_TEXT_COMPONENT)
      }

      CACHED_HELP.forEach {
        if (sender is Player) {
          sender.spigot().sendMessage(it)
        } else {
          sender.sendMessage(it.toLegacyText())
        }
      }
      return false
    }

    // ifPresentOrElse() doesn't exist yet... (version compatibility)
    subCommand.ifPresent { sub: SubCommand ->
      if (sub.info.onlyPlayers && sender !is Player) {
        invocationSender.sendMessage(Sonar.get().config.PLAYERS_ONLY)
        return@ifPresent
      }

      if (sub.info.onlyConsole && sender !is ConsoleCommandSender) {
        invocationSender.sendMessage(Sonar.get().config.CONSOLE_ONLY)
        return@ifPresent
      }

      val commandInvocation = CommandInvocation(
        sender.name,
        invocationSender,
        sub,
        args
      )

      // The subcommands has arguments which are not present in the executed command
      if (sub.info.arguments.isNotEmpty()
        && commandInvocation.arguments.size <= 1
      ) {
        invocationSender.sendMessage(
          Sonar.get().config.INCORRECT_COMMAND_USAGE
            .replace("%usage%", sub.info.name + " (" + sub.arguments + ")")
        )
        return@ifPresent
      }

      // Execute the sub command with the custom invocation properties
      sub.execute(commandInvocation)
    }
    return false
  }

  // Tab completion handling
  override fun onTabComplete(
    sender: CommandSender, command: Command,
    alias: String, args: Array<String>
  ): List<String> {
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
          ARG_TAB_SUGGESTIONS[subCommand.info.name] = Arrays.stream(subCommand.info.arguments)
            .map { obj: Argument -> obj.name }
            .collect(Collectors.toList())
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
