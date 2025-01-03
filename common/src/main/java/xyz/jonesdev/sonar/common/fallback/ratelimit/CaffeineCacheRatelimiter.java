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

package xyz.jonesdev.sonar.common.fallback.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.ratelimit.Ratelimiter;

import java.net.InetAddress;
import java.time.Duration;

// Idea taken from Velocity
@Setter
@RequiredArgsConstructor
public final class CaffeineCacheRatelimiter implements Ratelimiter<InetAddress> {
  private long timeout;
  private Cache<InetAddress, Long> expiringCache;

  public CaffeineCacheRatelimiter(final @NotNull Duration duration) {
    this.timeout = duration.toNanos();
    this.expiringCache = Caffeine.newBuilder()
      .ticker(Ticker.systemTicker())
      .expireAfterWrite(duration)
      .build();
  }

  @Override
  public boolean attempt(final @NotNull InetAddress inetAddress) {
    final long expectedNewTimestamp = System.nanoTime() + timeout;
    final long last = expiringCache.get(inetAddress, result -> expectedNewTimestamp);
    return expectedNewTimestamp == last;
  }
}
