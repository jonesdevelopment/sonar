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

package jones.sonar.bungee.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jones.sonar.common.command.CommandHelper;
import jones.sonar.common.command.CommandInvocation;
import jones.sonar.common.command.InvocationSender;
import jones.sonar.common.command.subcommand.SubCommand;
import jones.sonar.common.command.subcommand.SubCommandManager;
import lombok.var;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class SonarCommand extends Command implements TabExecutor {
  private static final Cache<CommandSender, Long> delay = CacheBuilder.newBuilder()
    .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
    .build();
  private static final TextComponent ONLY_PLAYERS = new TextComponent(
    "§cYou can only execute this command as a player."
  );
  private static final TextComponent CANNOT_RUN_YET = new TextComponent(
    "§cYou can only execute this command every 0.5 seconds."
  );
  private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

  public SonarCommand() {
    super("sonar", "sonar.command");
  }

  @Override
  public void execute(final CommandSender sender, final String[] args) {
    final long timestamp = delay.asMap().getOrDefault(sender, -1L);
    final long currentTimestamp = System.currentTimeMillis();

    if (timestamp > 0L) {
      sender.sendMessage(CANNOT_RUN_YET);

      final double left = 0.5D - ((currentTimestamp - (double) timestamp) / 1000D);
      final String format = decimalFormat.format(left);

      final TextComponent pleaseWaitAnother = new TextComponent("§cPlease wait another §l" + format + "s§r§c.");

      sender.sendMessage(pleaseWaitAnother);
      return;
    }

    delay.put(sender, currentTimestamp);

    Optional<SubCommand> subCommand = Optional.empty();

    final var invocationSender = new InvocationSender<CommandSender>() {

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
          sender.sendMessage(new TextComponent(
            "§cYou do not have permission to execute this subcommand. §7(" + permission + ")"
          ));
          return;
        }
      }
    }

    if (!subCommand.isPresent()) {
      CommandHelper.printHelp(invocationSender);
      return;
    }

    subCommand.ifPresent(sub -> {
      if (sub.getInfo().onlyPlayers() && !(sender instanceof ProxiedPlayer)) {
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
  }

  private static final Collection<String> TAB_SUGGESTIONS = new ArrayList<>();

  @Override
  public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
    return args.length <= 1
      ? (TAB_SUGGESTIONS.isEmpty() ? refreshSuggestions() : TAB_SUGGESTIONS)
      : Collections.emptyList();
  }

  private static Collection<String> refreshSuggestions() {
    if (TAB_SUGGESTIONS.isEmpty()) {
      for (final SubCommand subCommand : SubCommandManager.getSubCommands()) {
        TAB_SUGGESTIONS.add(subCommand.getInfo().name());

        if (subCommand.getInfo().aliases().length > 0) {
          TAB_SUGGESTIONS.addAll(Arrays.asList(subCommand.getInfo().aliases()));
        }
      }
    }
    return TAB_SUGGESTIONS;
  }
}
