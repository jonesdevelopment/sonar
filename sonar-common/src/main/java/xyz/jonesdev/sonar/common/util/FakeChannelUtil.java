/*
 * Copyright (C) 2024 Sonar Contributors
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
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

// https://github.com/retrooper/packetevents/blob/2.0/api/src/main/java/com/github/retrooper/packetevents/util/FakeChannelUtil.java
@UtilityClass
public class FakeChannelUtil {

  /**
   * @param channel Channel of the player
   * @return Whether the channel is spoofed/faked
   */
  public boolean isFakePlayer(final @NotNull Channel channel) {
    final String simpleClassName = channel.getClass().getSimpleName();
    // Player spoof plugins use fake channels (e.g., Spoof Engine uses "FakeChannel")
    return "FakeChannel".equals(simpleClassName) || "SpoofedChannel".equals(simpleClassName);
  }
}
