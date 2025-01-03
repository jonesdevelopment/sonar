/*
 * Copyright (C) 2025 Sonar Contributors
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

package xyz.jonesdev.sonar.common.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

@UtilityClass
public class GeyserUtil {
  // https://github.com/GeyserMC/Floodgate/blob/master/core/src/main/java/org/geysermc/floodgate/module/CommonModule.java#L206
  private final AttributeKey<Object> PLAYER_ATTRIBUTE = AttributeKey.valueOf("floodgate-player");

  public boolean isGeyserConnection(final @NotNull Channel channel, final @NotNull InetSocketAddress originalAddress) {
    /*
     * This method performs two checks:
     * 1. It checks if the player is joining through Floodgate, which is installed on the server.
     * 2. It checks if the player is joining through a Floodgate proxy hosted externally.
     */
    return originalAddress.getPort() == 0 || channel.attr(PLAYER_ATTRIBUTE).get() != null;
  }
}
