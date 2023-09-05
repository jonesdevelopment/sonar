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

package xyz.jonesdev.sonar.api.verbose;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.profiler.JVMProfiler;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.util.Collection;
import java.util.Vector;

import static xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter.INCOMING;
import static xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter.OUTGOING;

@Getter
public abstract class Verbose implements JVMProfiler {
  protected final @NotNull Collection<String> subscribers = new Vector<>(0);
  private final SystemTimer secondTimer = new SystemTimer();
  protected int joinsPerSecond, totalJoins;
  private int lastTotalJoins;

  // Run action bar verbose
  public final void update() {
    // Clean up all blacklisted IPs
    Sonar.get().getFallback().getBlacklisted().cleanUp(false);

    totalJoins = Statistics.TOTAL_TRAFFIC.get();

    // Statistically determine the joins per second without any caches
    if (totalJoins > 0 && secondTimer.delay() >= 1000L) {
      secondTimer.reset();
      joinsPerSecond = totalJoins - lastTotalJoins;
      lastTotalJoins = totalJoins;
    }

    // Send the action bar to all online players
    broadcast(prepareActionBarFormat());
  }

  protected String prepareActionBarFormat() {
    return Sonar.get().getConfig().ACTION_BAR_LAYOUT
      .replace("%queued%",
        Sonar.DECIMAL_FORMAT.format(Sonar.get().getFallback().getQueue().getQueuedPlayers().size()))
      .replace("%verifying%", Sonar.DECIMAL_FORMAT.format(Sonar.get().getFallback().getConnected().size()))
      .replace("%blacklisted%",
        Sonar.DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklisted().estimatedSize()))
      .replace("%total-joins%", Sonar.DECIMAL_FORMAT.format(totalJoins))
      .replace("%per-second-joins%", Sonar.DECIMAL_FORMAT.format(joinsPerSecond))
      .replace("%verify-total%", Sonar.DECIMAL_FORMAT.format(Statistics.REAL_TRAFFIC.get()))
      .replace("%verify-success%",
        Sonar.DECIMAL_FORMAT.format(Sonar.get().getVerifiedPlayerController().estimatedSize()))
      .replace("%verify-failed%", Sonar.DECIMAL_FORMAT.format(Statistics.FAILED_VERIFICATIONS.get()))
      .replace("%incoming-traffic%", INCOMING.getCachedSecond())
      .replace("%outgoing-traffic%", OUTGOING.getCachedSecond())
      .replace("%incoming-traffic-ttl%", INCOMING.getCachedTtl())
      .replace("%outgoing-traffic-ttl%", OUTGOING.getCachedTtl())
      .replace("%used-memory%", formatMemory(getUsedMemory()))
      .replace("%free-memory%", formatMemory(getFreeMemory()))
      .replace("%total-memory%", formatMemory(getTotalMemory()))
      .replace("%max-memory%", formatMemory(getMaxMemory()))
      .replace("%animation%", VerboseAnimation.nextAnimation());
  }

  // Run action bar verbose
  protected abstract void broadcast(final String message);

  /**
   * @param subscriber Name of the player who subscribed
   * @return Whether the player is subscribed or not
   */
  public boolean isSubscribed(final @NotNull String subscriber) {
    return subscribers.contains(subscriber);
  }

  /**
   * @param subscriber Name of the player to subscribe
   */
  public void subscribe(final @NotNull String subscriber) {
    subscribers.add(subscriber);
  }

  /**
   * @param subscriber Name of the player to unsubscribe
   */
  public void unsubscribe(final @NotNull String subscriber) {
    subscribers.remove(subscriber);
  }
}
