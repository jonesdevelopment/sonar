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

package xyz.jonesdev.sonar.common.service;

import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.common.statistics.CachedBandwidthStatistics;

@UtilityClass
public final class SonarServiceThreadManager {
  private static final SonarServiceThread VERBOSE = new SonarServiceThread("sonar-verbose-thread", 250L,
    () -> {
      // Make sure to clean up the cached statistics
      // since we don't want to display wrong values.
      Sonar.get().getStatistics().cleanUpCache();
      // Update the attack tracker
      Sonar.get().getAttackTracker().checkIfUnderAttack();
      // Update the action bar verbose
      Sonar.get().getVerboseHandler().update();
    });
  private static final SonarServiceThread FALLBACK_QUEUE = new SonarServiceThread("sonar-fallback-queue-thread", 1000L,
    () -> Sonar.get().getFallback().getQueue().poll());
  private static final SonarServiceThread STATISTICS = new SonarServiceThread("sonar-statistics-thread", 1000L,
    () -> {
      // Clean up bandwidth statistics
      CachedBandwidthStatistics.reset();
      // Clean up the cache of rate-limited IPs
      Sonar.get().getFallback().getRatelimiter().cleanUpCache();
    });

  public void start() {
    VERBOSE.start();
    FALLBACK_QUEUE.start();
    STATISTICS.start();
  }

  public void stop() {
    VERBOSE.interrupt();
    FALLBACK_QUEUE.interrupt();
    STATISTICS.interrupt();
  }
}
