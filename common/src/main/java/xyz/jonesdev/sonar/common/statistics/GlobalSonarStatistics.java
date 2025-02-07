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

package xyz.jonesdev.sonar.common.statistics;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.jetbrains.annotations.ApiStatus;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.profiler.SimpleProcessProfiler;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public final class GlobalSonarStatistics implements SonarStatistics {
  private static final Cache<Integer, Byte> LOGINS_PER_SECOND = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(1))
    .ticker(Ticker.systemTicker())
    .build();

  private static final Cache<Integer, Byte> CONNECTIONS_PER_SECOND = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(1))
    .ticker(Ticker.systemTicker())
    .build();

  private static final AtomicInteger ACTION_COUNTER = new AtomicInteger(Integer.MIN_VALUE);

  /**
   * Helper methods that make it easier to count new statistics
   */

  @ApiStatus.Internal
  public static void countConnection() {
    CONNECTIONS_PER_SECOND.put(ACTION_COUNTER.getAndIncrement(), (byte) 0);
  }

  @ApiStatus.Internal
  public static void countLogin() {
    LOGINS_PER_SECOND.put(ACTION_COUNTER.getAndIncrement(), (byte) 0);
    totalJoinedPlayers++;
  }

  // Cache all per-session statistics
  private static int totalJoinedPlayers;
  public static int totalSuccessfulVerifications;
  public static int totalFailedVerifications;
  public static int totalAttemptedVerifications;
  public static long totalBlacklistedPlayers;
  public static long totalIncomingTraffic;
  public static long totalOutgoingTraffic;
  public static long perSecondIncomingTraffic;
  public static long perSecondOutgoingTraffic;
  private static String perSecondIncomingTrafficFormatted;
  private static String perSecondOutgoingTrafficFormatted;

  public static void cleanUpCaches() {
    LOGINS_PER_SECOND.cleanUp();
    CONNECTIONS_PER_SECOND.cleanUp();
  }

  public static void hitEverySecond() {
    totalIncomingTraffic += perSecondIncomingTraffic;
    totalOutgoingTraffic += perSecondOutgoingTraffic;
    perSecondIncomingTrafficFormatted = SimpleProcessProfiler.formatMemory(perSecondIncomingTraffic);
    perSecondOutgoingTrafficFormatted = SimpleProcessProfiler.formatMemory(perSecondOutgoingTraffic);
    perSecondIncomingTraffic = 0L;
    perSecondOutgoingTraffic = 0L;
  }

  @Override
  public long getConnectionsPerSecond() {
    return CONNECTIONS_PER_SECOND.estimatedSize();
  }

  @Override
  public long getLoginsPerSecond() {
    return LOGINS_PER_SECOND.estimatedSize();
  }

  @Override
  public long getCurrentIncomingBandwidth() {
    return perSecondIncomingTraffic;
  }

  @Override
  public long getCurrentOutgoingBandwidth() {
    return perSecondOutgoingTraffic;
  }

  @Override
  public long getTotalIncomingBandwidth() {
    return totalIncomingTraffic;
  }

  @Override
  public long getTotalOutgoingBandwidth() {
    return totalOutgoingTraffic;
  }

  @Override
  public String getPerSecondIncomingBandwidthFormatted() {
    return perSecondIncomingTrafficFormatted;
  }

  @Override
  public String getPerSecondOutgoingBandwidthFormatted() {
    return perSecondOutgoingTrafficFormatted;
  }

  @Override
  public int getTotalPlayersJoined() {
    return totalJoinedPlayers;
  }

  @Override
  public int getTotalPlayersVerified() {
    return Sonar.get0().getVerifiedPlayerController().getCache().size();
  }

  @Override
  public int getTotalSuccessfulVerifications() {
    return totalSuccessfulVerifications;
  }

  @Override
  public int getTotalFailedVerifications() {
    return totalFailedVerifications;
  }

  @Override
  public long getCurrentAttemptedVerifications() {
    return Sonar.get0().getFallback().getConnected().size();
  }

  @Override
  public int getTotalAttemptedVerifications() {
    return totalAttemptedVerifications;
  }

  @Override
  public long getCurrentBlacklistSize() {
    return Sonar.get0().getFallback().getBlacklist().estimatedSize();
  }

  @Override
  public long getTotalBlacklistSize() {
    return totalBlacklistedPlayers;
  }
}
