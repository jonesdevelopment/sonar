/*
 * Copyright (C) 2026 Sonar Contributors
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

package xyz.jonesdev.sonar.paper.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.SonarCommand;

import java.util.Collection;
import java.util.Collections;

public final class PaperSonarCommand implements BasicCommand, SonarCommand {
  @Override
  public void execute(final CommandSourceStack commandSourceStack, final String @NonNull [] args) {
    final CommandSender sender = commandSourceStack.getSender();
    final InvocationSource invocationSource = new InvocationSource(
      sender instanceof Player ? ((Player) sender).getUniqueId() : null,
      sender,
      sender::hasPermission
    );
    handle(invocationSource, args);
  }

  @Override
  public @NonNull Collection<String> suggest(final CommandSourceStack commandSourceStack, final String @NonNull [] args) {
    // Do not allow tab completion if the player does not have the required permission
    return commandSourceStack.getSender().hasPermission("sonar.command") ?
      getCachedTabSuggestions(args) : Collections.emptyList();
  }
}
