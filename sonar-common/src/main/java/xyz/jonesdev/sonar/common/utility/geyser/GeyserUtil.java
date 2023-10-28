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

package xyz.jonesdev.sonar.common.utility.geyser;

import lombok.experimental.UtilityClass;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/**
 * Simple utility to determine if someone joins using GeyserMC
 */
@UtilityClass
public class GeyserUtil {

  private boolean FLOODGATE;

  static {
    try {
      Class.forName("org.geysermc.floodgate.api.FloodgateApi");
      FLOODGATE = true;
    } catch (Throwable throwable) {
      FLOODGATE = false;
    }
  }

  /**
   * @param uuid UUID of the player
   * @return Whether the player is on GeyserMC or not
   */
  public boolean isGeyserConnection(final UUID uuid) {
    // First, check if floodgate is even available
    if (!FLOODGATE) return false;
    // Check if the UUID is corresponding to a FloodgatePlayer
    return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
  }
}
