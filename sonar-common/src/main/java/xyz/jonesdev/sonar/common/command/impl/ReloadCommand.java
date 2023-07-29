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

package xyz.jonesdev.sonar.common.command.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

@SubcommandInfo(
  name = "reload",
  description = "Reload the configuration"
)
public final class ReloadCommand extends Subcommand {

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    final long startTime = System.currentTimeMillis();

    invocation.getSender().sendMessage(SONAR.getConfig().RELOADING);
    SONAR.reload();

    final long timeTaken = System.currentTimeMillis() - startTime;
    invocation.getSender().sendMessage(
      SONAR.getConfig().RELOADED
        .replace("%taken%", String.valueOf(timeTaken))
    );
  }
}
