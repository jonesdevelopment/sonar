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

package xyz.jonesdev.sonar.common.service;

import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class ServiceRepository {
  private final ScheduledExecutorService QUEUE_SERVICE = Executors.newSingleThreadScheduledExecutor();
  private final ScheduledExecutorService TRAFFIC_SERVICE = Executors.newSingleThreadScheduledExecutor();
  private final ScheduledExecutorService VERBOSE_SERVICE = Executors.newSingleThreadScheduledExecutor();
  private boolean registered;

  public void register() {
    if (registered) {
      throw new IllegalStateException("Already registered");
    }
    registered = true;

    TRAFFIC_SERVICE.scheduleAtFixedRate(TrafficCounter::reset,
      1L, 1L, TimeUnit.SECONDS);
    QUEUE_SERVICE.scheduleAtFixedRate(Sonar.get().getFallback().getQueue()::poll,
      500L, 500L, TimeUnit.MILLISECONDS);
    VERBOSE_SERVICE.scheduleAtFixedRate(Sonar.get().getVerboseHandler()::update,
      200L, 200L, TimeUnit.MILLISECONDS);
  }

  public synchronized void shutdown() {
    if (!registered) {
      throw new IllegalStateException("Not registered");
    }
    registered = false;

    QUEUE_SERVICE.shutdown();
    TRAFFIC_SERVICE.shutdown();
    VERBOSE_SERVICE.shutdown();
  }
}
