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

package xyz.jonesdev.sonar.common.util.geyser;

import io.netty.channel.Channel;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Simple utility to determine if someone joins using GeyserMC
 */
@UtilityClass
public class GeyserValidator {

  /**
   * Checks if Geyser holds the parent channel of the player
   *
   * @param channel Channel of the player
   * @return Whether the player is on GeyserMC or not
   */
  public boolean isGeyserConnection(final @NotNull Channel channel) {
    // Get the parent channel of the original channel
    final Channel parent = channel.parent();
    // This shouldn't happen, but we want to stay safe here
    if (parent == null) return false;

    final Class<? extends Channel> clazz = parent.getClass();
    // Check if Geyser adapted the channel by checking for the package name
    return clazz.getCanonicalName().startsWith("org.geysermc");
  }
}
