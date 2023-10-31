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

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Simple utility to determine if someone joins using GeyserMC
 */
@UtilityClass
public class GeyserUtil {

  /*
   * Geyser attribute key for every floodgate player
   */
  private AttributeKey<Object> playerAttribute() {
    return AttributeKey.valueOf("floodgate-player");
  }

  /**
   * @param channel Channel of the player
   * @return Whether the player is on GeyserMC or not
   */
  public boolean isGeyserConnection(final @NotNull Channel channel) {
    // https://discord.com/channels/613163671870242838/613170125696270357/1168599123889504266
    // Every floodgate player has a channel attribute called "floodgate-player"
    return channel.hasAttr(playerAttribute());
  }
}
