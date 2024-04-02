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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FallbackRatelimiter {
  static final FallbackRatelimiter INSTANCE = new FallbackRatelimiter();

  private Cache<InetAddress, Byte> attemptCache;
  @Getter
  private Cache<InetAddress, Integer> failCountCache;

  /**
   * We don't want to clean up the cache in the
   * {@link #attempt(InetAddress)} method because
   * it would take up too many resources.
   */
  public void cleanUpCache() {
    attemptCache.cleanUp();
    failCountCache.cleanUp();
  }

  /**
   * Checks if the player has tried verifying too fast
   *
   * @param inetAddress IP address of the player
   * @return Whether the player is allowed to verify
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean attempt(final @NotNull InetAddress inetAddress) {
    // Cache the IP address if it's not already cached
    if (!attemptCache.asMap().containsKey(inetAddress)) {
      // Cache the IP address
      attemptCache.put(inetAddress, (byte) 0);
      return true;
    }
    // Deny the connection attempt
    return false;
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
