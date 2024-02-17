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

package xyz.jonesdev.sonar.api.statistics;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;

@SuppressWarnings("unused")
public interface Counters {
  @ApiStatus.Internal
  Cache<Long, Byte> LOGINS_PER_SECOND = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(1))
    .build();

  @ApiStatus.Internal
  Cache<Long, Byte> CONNECTIONS_PER_SECOND = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(1))
    .build();

  static long getConnectionsPerSecond() {
    return CONNECTIONS_PER_SECOND.estimatedSize();
  }

  static long getLoginsPerSecond() {
    return LOGINS_PER_SECOND.estimatedSize();
  }
}
