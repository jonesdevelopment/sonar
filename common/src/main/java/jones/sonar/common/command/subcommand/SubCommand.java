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

package jones.sonar.common.command.subcommand;

import jones.sonar.api.Sonar;
import jones.sonar.common.command.CommandInvocation;
import jones.sonar.common.command.subcommand.argument.Argument;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public abstract class SubCommand {
  private final SubCommandInfo info;
  private final String permission, aliases, arguments;
  protected static final Sonar sonar = Sonar.get();

  public SubCommand() {
    info = getClass().getAnnotation(SubCommandInfo.class);
    permission = "sonar." + info.name();
    aliases = info.aliases().length == 0 ? "No aliases."
      : String.join(", ", info.aliases());

    arguments = info.arguments().length == 0 ? ""
      : Arrays.stream(info.arguments())
      .map(Argument::name)
      .collect(Collectors.joining(", "));
  }

  public abstract void execute(final CommandInvocation invocation);
}
