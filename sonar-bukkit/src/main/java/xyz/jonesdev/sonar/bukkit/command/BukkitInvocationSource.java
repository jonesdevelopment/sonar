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

package xyz.jonesdev.sonar.bukkit.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.bukkit.SonarBukkit;

public final class BukkitInvocationSource extends InvocationSource {
  public BukkitInvocationSource(final @NotNull CommandSender sender) {
    super(sender instanceof Player ? ((Player) sender).getUniqueId() : null,
      SonarBukkit.INSTANCE.getBukkitAudiences().sender(sender),
      sender instanceof Player,
      sender::hasPermission);
  }
}
