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

package xyz.jonesdev.sonar.api.fallback;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class FallbackRatelimiter {
  private long timeout;
  private Cache<InetAddress, Long> attemptCache;
  @Getter
  private Cache<InetAddress, Integer> failCountCache;

  /**
   * Attempts to rate-limit the client
   *
   * @param inetAddress IP address of the player
   * @return Whether the player is allowed to verify
   */
  public boolean attempt(final @NotNull InetAddress inetAddress) {
    final long expectedTimestamp = System.currentTimeMillis() + timeout;
    // Check if the time since the last join has not exceeded the timeout
    // The idea for this check was Taken from Velocity
    final long last = attemptCache.get(inetAddress, result -> expectedTimestamp);
    return expectedTimestamp == last;
  }

  /**
   * Checks if the player is failing verifications too often
   * Increments the number of times this user has failed the verification
   *
   * @param inetAddress IP address of the player
   * @param count       Count of previously failed verifications
   */
  public void incrementFails(final @NotNull InetAddress inetAddress, final int count) {
    // Make sure to remove the old values from the cache
    if (count > 0) {
      failCountCache.invalidate(inetAddress);
    }
    failCountCache.put(inetAddress, count + 1);
  }
}
