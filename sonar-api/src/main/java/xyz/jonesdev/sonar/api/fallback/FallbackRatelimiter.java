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

package xyz.jonesdev.sonar.api.fallback;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.jonesdev.cappuchino.ExpiringCache;

import java.net.InetAddress;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FallbackRatelimiter {
  public static final FallbackRatelimiter INSTANCE = new FallbackRatelimiter();
  @Setter
  private ExpiringCache<InetAddress> expiringCache;

  /**
   * Checks if the player has tried verifying too fast
   *
   * @param inetAddress IP address of the player
   * @return Whether the player is allowed to verify
   */
  public boolean shouldDeny(final InetAddress inetAddress) {
    expiringCache.cleanUp();
    if (expiringCache.has(inetAddress)) {
      return true;
    }
    expiringCache.put(inetAddress);
    return false;
  }
}
