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

package jones.sonar.bukkit.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jones.sonar.common.command.CommandHelper;
import jones.sonar.common.command.CommandInvocation;
import jones.sonar.common.command.InvocationSender;
import jones.sonar.common.command.subcommand.SubCommand;
import jones.sonar.common.command.subcommand.SubCommandManager;
import lombok.var;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class SonarCommand implements CommandExecutor {
  private static final Cache<CommandSender, Long> delay = CacheBuilder.newBuilder()
    .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
    .build();
  private static final String ONLY_PLAYERS = "§cYou can only execute this command as a player.";
  private static final String CANNOT_RUN_YET = "§cYou can only execute this command every 0.5 seconds.";
  private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

  @Override
  public boolean onCommand(final CommandSender sender,
                           final Command command,
                           final String label,
                           final String[] args) {
    final long timestamp = delay.asMap().getOrDefault(sender, -1L);

    if (timestamp > 0L) {
      sender.sendMessage(CANNOT_RUN_YET);

      final double left = 0.5D - ((System.currentTimeMillis() - (double) timestamp) / 1000D);
      final String format = decimalFormat.format(left);

      final String pleaseWaitAnother = "§cPlease wait another §l" + format + "s§r§c.";

      sender.sendMessage(pleaseWaitAnother);
      return false;
    }

    delay.put(sender, System.currentTimeMillis());

    var subCommand = Optional.<SubCommand>empty();

    var invocationSender = new InvocationSender<CommandSender>() {

      @Override
      public void sendMessage(final String message) {
        sender.sendMessage(message);
      }

      @Override
      public CommandSender getPlayer() {
        return sender;
      }
    };

    if (args.length > 0) {
      subCommand = SubCommandManager.getSubCommands().stream()
        .filter(sub -> sub.getInfo().name().equalsIgnoreCase(args[0])
          || (sub.getInfo().aliases().length > 0
          && Arrays.stream(sub.getInfo().aliases())
          .anyMatch(alias -> alias.equalsIgnoreCase(args[0]))))
        .findFirst();

      if (subCommand.isPresent()) {
        final String permission = "sonar." + subCommand.get().getInfo().name();

        if (!sender.hasPermission(permission)) {
          sender.sendMessage(
            "§cYou do not have permission to execute this subcommand. §7(" + permission + ")"
          );
          return false;
        }
      }
    }

    if (!subCommand.isPresent()) {
      CommandHelper.printHelp(invocationSender);
      return false;
    }

    subCommand.ifPresent(sub -> {
      if (sub.getInfo().onlyPlayers() && !(sender instanceof Player)) {
        sender.sendMessage(ONLY_PLAYERS);
        return;
      }

      final CommandInvocation commandInvocation = new CommandInvocation(
        sender.getName(),
        invocationSender,
        sub,
        args
      );

      sub.execute(commandInvocation);
    });
    return false;
  }
}
