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
import jones.sonar.common.command.subcommand.SubCommandManager;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandHelper {
  public void printHelp(final InvocationSender<?> sender) {
    sender.sendMessage("§fThis server is running §6§lSonar §7"
      + Sonar.get().getVersion()
      + "§f on §7"
      + Sonar.get().getPlatform().getDisplayName());
    sender.sendMessage();

    SubCommandManager.getSubCommands().forEach(sub -> {
      sender.sendMessage(" §e● §7/sonar "
        + sub.getInfo().name()
        + " §f"
        + sub.getInfo().description()
      );
    });

    sender.sendMessage();
  }
}
