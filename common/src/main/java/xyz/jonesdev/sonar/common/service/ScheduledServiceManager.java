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

package xyz.jonesdev.sonar.common.service;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.update.UpdateChecker;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@UtilityClass
public final class ScheduledServiceManager {
  private final ScheduledExecutorService VERBOSE = createScheduledExecutor("sonar-verbose-thread");
  private final ScheduledExecutorService FALLBACK_QUEUE = createScheduledExecutor("sonar-queue-thread");
  private final ScheduledExecutorService STATISTICS = createScheduledExecutor("sonar-statistics-thread");
  private final ScheduledExecutorService UPDATE_NOTIFIER = createScheduledExecutor("sonar-update-notifier");

  private @NotNull ScheduledExecutorService createScheduledExecutor(final String threadName) {
    return Executors.newSingleThreadScheduledExecutor(runnable -> {
      final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
      thread.setName(threadName);
      thread.setDaemon(true);
      return thread;
    });
  }

  public void start() {
    VERBOSE.scheduleAtFixedRate(() -> {
      // Make sure to clean up the cached statistics since we don't want to display wrong values
      GlobalSonarStatistics.cleanUpCaches();
      Sonar.get0().getFallback().getBlacklist().cleanUp();
      // Update the attack tracker
      Sonar.get0().getAttackTracker().checkIfUnderAttack();
      // Publish the action bar notifications
      Sonar.get0().getActionBarNotificationHandler().handleNotification();
    }, 0L, 250L, TimeUnit.MILLISECONDS);

    FALLBACK_QUEUE.scheduleAtFixedRate(() -> Sonar.get0().getFallback().getQueue().poll(),
      1L, 1L, TimeUnit.SECONDS);

    STATISTICS.scheduleAtFixedRate(GlobalSonarStatistics::hitEverySecond,
      0L, 1L, TimeUnit.SECONDS);

    // This config setting only updates when the server is restarted
    if (Sonar.get0().getConfig().getGeneralConfig().getBoolean("general.check-for-updates")) {
      UPDATE_NOTIFIER.scheduleAtFixedRate(UpdateChecker::checkForUpdates,
        0L, 2L, TimeUnit.HOURS);
    }
  }

  public void stop() {
    VERBOSE.shutdown();
    FALLBACK_QUEUE.shutdown();
    STATISTICS.shutdown();
    UPDATE_NOTIFIER.shutdown();
  }
}
