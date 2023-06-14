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

package jones.sonar.common.command;

import jones.sonar.api.Sonar;
import jones.sonar.common.command.subcommand.SubCommand;
import jones.sonar.common.command.subcommand.SubCommandRegistry;
import jones.sonar.common.command.subcommand.argument.Argument;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CommandInvocation {
  private final String executorName;

  public String getExecutorName() {
    return executorName;
  }

  private final InvocationSender<?> invocationSender;

  public InvocationSender<?> getInvocationSender() {
    return invocationSender;
  }

  private final SubCommand command;

  public SubCommand getCommand() {
    return command;
  }

  private final String[] arguments;

  public String[] getArguments() {
    return arguments;
  }

  public static void printSubNotFound(final InvocationSender<?> invocationSender, final SubCommand subCommand) {
    invocationSender.sendMessage("§fAvailable command arguments for §e/sonar " + subCommand.getInfo().name() + "§f:");
    invocationSender.sendMessage();

    for (final Argument argument : subCommand.getInfo().arguments()) {
      invocationSender.sendMessage(
        " §e● §7/sonar "
          + subCommand.getInfo().name()
          + " "
          + argument.name()
          + " §f"
          + argument.description()
      );
    }

    invocationSender.sendMessage();
  }

  public static void printHelp(final InvocationSender<?> invocationSender) {
    invocationSender.sendMessage("§fThis server is running §6§lSonar §7"
      + Sonar.get().getVersion()
      + "§f on §7"
      + Sonar.get().getPlatform().getDisplayName());
    invocationSender.sendMessage();

    SubCommandRegistry.getSubCommands().forEach(sub -> {
      invocationSender.sendMessage(" §e● §7/sonar "
        + sub.getInfo().name()
        + " §f"
        + sub.getInfo().description()
      );
    });

    invocationSender.sendMessage();
  }
}
