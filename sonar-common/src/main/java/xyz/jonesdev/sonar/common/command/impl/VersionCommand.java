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
  name = "version",
  description = "Show version information"
)
public final class VersionCommand extends Subcommand {

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    invocation.getSender().sendMessage();
    invocation.getSender().sendMessage(" §eVersion information");
    invocation.getSender().sendMessage();
    invocation.getSender().sendMessage(" §a▪ §7Sonar version: §f" + SONAR.getVersion().getSemanticVersion());
    invocation.getSender().sendMessage(" §a▪ §7Commit SHA: §f" + SONAR.getVersion().getCommitSHA());
    invocation.getSender().sendMessage(" §a▪ §7Build number: §f" + SONAR.getVersion().getBuild());
    invocation.getSender().sendMessage(" §a▪ §7Platform: §f" + SONAR.getServer().getPlatform().getDisplayName());
    invocation.getSender().sendMessage();
  }
}
