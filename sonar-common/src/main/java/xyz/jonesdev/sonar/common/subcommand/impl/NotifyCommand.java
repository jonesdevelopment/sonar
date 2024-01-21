/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.subcommand.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

@SubcommandInfo(
  name = "notify",
  description = "Enable or disable attack notifications"
)
public final class NotifyCommand extends Subcommand {

  @Override
  protected void execute(final @NotNull CommandInvocation invocation) {
    if (SONAR.getNotificationHandler().isSubscribed(invocation.getSender().getName())) {
      SONAR.getNotificationHandler().unsubscribe(invocation.getSender().getName());
      invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getNotificationsUnsubscribed());
      return;
    }

    invocation.getSender().sendMessage(SONAR.getConfig().getCommands().getNotificationsSubscribed());
    SONAR.getNotificationHandler().subscribe(invocation.getSender().getName());
  }
}
