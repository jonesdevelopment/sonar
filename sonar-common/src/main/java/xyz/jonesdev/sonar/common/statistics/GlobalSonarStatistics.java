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

package xyz.jonesdev.sonar.common.statistics;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.ApiStatus;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public final class GlobalSonarStatistics implements SonarStatistics {
  private static final Cache<Integer, Byte> LOGINS_PER_SECOND = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(1))
    .build();

  private static final Cache<Integer, Byte> CONNECTIONS_PER_SECOND = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(1))
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

  /**
   * Called every time the action bar updates to ensure fresh values
   */
  @Override
  public void cleanUpCache() {
    LOGINS_PER_SECOND.cleanUp();
    CONNECTIONS_PER_SECOND.cleanUp();
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
    return CachedBandwidthStatistics.INCOMING.getCurr();
  }

  @Override
  public long getCurrentOutgoingBandwidth() {
    return CachedBandwidthStatistics.OUTGOING.getCurr();
  }

  @Override
  public long getTotalIncomingBandwidth() {
    return CachedBandwidthStatistics.INCOMING.getTtl();
  }

  @Override
  public long getTotalOutgoingBandwidth() {
    return CachedBandwidthStatistics.OUTGOING.getTtl();
  }

  @Override
  public String getCurrentIncomingBandwidthFormatted() {
    return CachedBandwidthStatistics.INCOMING.getCachedSecond();
  }

  @Override
  public String getCurrentOutgoingBandwidthFormatted() {
    return CachedBandwidthStatistics.OUTGOING.getCachedSecond();
  }

  @Override
  public String getTotalIncomingBandwidthFormatted() {
    return CachedBandwidthStatistics.INCOMING.getCachedTtl();
  }

  @Override
  public String getTotalOutgoingBandwidthFormatted() {
    return CachedBandwidthStatistics.OUTGOING.getCachedTtl();
  }

  @Override
  public int getTotalPlayersJoined() {
    return totalJoinedPlayers;
  }

  @Override
  public int getTotalPlayersVerified() {
    return Sonar.get().getVerifiedPlayerController().estimatedSize();
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
    return Sonar.get().getFallback().getConnected().size();
  }

  @Override
  public int getTotalAttemptedVerifications() {
    return totalAttemptedVerifications;
  }

  @Override
  public long getCurrentBlacklistSize() {
    return Sonar.get().getFallback().getBlacklist().estimatedSize();
  }

  @Override
  public long getTotalBlacklistSize() {
    return totalBlacklistedPlayers;
  }
}
