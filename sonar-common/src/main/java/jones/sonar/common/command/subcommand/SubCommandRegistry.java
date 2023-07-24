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

package jones.sonar.common.command.subcommand;

import jones.sonar.common.command.subcommand.impl.*;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collection;

@UtilityClass
// The methods are accessible externally
@SuppressWarnings("unused")
public class SubCommandRegistry {
  @Getter
  private final Collection<SubCommand> subCommands = Arrays.asList(
    new BlacklistCommand(),
    new WhitelistCommand(),
    new StatisticsCommand(),
    new DatabaseCommand(),
    new LockdownCommand(),
    new VerboseCommand(),
    new ReloadCommand(),
    new VersionCommand(),
    new DumpCommand()
  );

  public void register(final SubCommand... commands) {
    subCommands.addAll(Arrays.asList(commands));
  }

  public void unregister(final SubCommand... commands) {
    subCommands.removeAll(Arrays.asList(commands));
  }
}
