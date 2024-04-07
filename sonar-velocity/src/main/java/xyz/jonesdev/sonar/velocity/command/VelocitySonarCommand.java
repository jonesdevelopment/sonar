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

package xyz.jonesdev.sonar.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.SonarCommand;

import java.util.List;

import static java.util.Collections.emptyList;

public final class VelocitySonarCommand implements SimpleCommand, SonarCommand {

  @Override
  public void execute(final @NotNull Invocation invocation) {
    // Create our own invocation source wrapper to handle messages properly
    final InvocationSource invocationSource = new InvocationSource(
      invocation.source() instanceof Player ? ((Player) invocation.source()).getUniqueId() : null,
      invocation.source(),
      invocation.source()::hasPermission);
    // Pass the invocation source and command arguments to our command handler
    handle(invocationSource, invocation.arguments());
  }

  @Override
  public List<String> suggest(final @NotNull Invocation invocation) {
    // Do not allow tab completion if the player does not have the required permission
    if (!invocation.source().hasPermission("sonar.command")) {
      return emptyList();
    }
    return getCachedTabSuggestions(invocation.arguments());
  }
}
