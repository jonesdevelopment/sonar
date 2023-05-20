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

import jones.sonar.common.command.subcommand.impl.ReloadCommand;
import jones.sonar.common.command.subcommand.impl.StatisticsCommand;
import jones.sonar.common.command.subcommand.impl.VerboseCommand;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collection;

@UtilityClass
public class SubCommandManager {
  @Getter
  private final Collection<SubCommand> subCommands = Arrays.asList(
    new StatisticsCommand(),
    new VerboseCommand(),
    new ReloadCommand()
  );

  public void register(final SubCommand... commands) {
    subCommands.addAll(Arrays.asList(commands));
  }

  public void unregister(final SubCommand... commands) {
    subCommands.removeAll(Arrays.asList(commands));
  }
}
