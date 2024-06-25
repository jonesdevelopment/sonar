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

package xyz.jonesdev.sonar.common.subcommands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

@SubcommandInfo(
  name = "reload"
)
public final class ReloadCommand extends Subcommand {

  @Override
  protected void execute(final @NotNull CommandInvocation invocation) {
    invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get().getConfig().getMessagesConfig().getString("commands.reload.start"),
      Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())));

    final SystemTimer timer = new SystemTimer();

    Sonar.get().reload();

    invocation.getSource().sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get().getConfig().getMessagesConfig().getString("commands.reload.finish"),
      Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
      Placeholder.unparsed("time-taken", String.valueOf(timer.delay()))));
  }
}
