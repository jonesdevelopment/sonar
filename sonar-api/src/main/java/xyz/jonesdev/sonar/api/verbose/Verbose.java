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
import lombok.val;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.profiler.JVMProfiler;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.util.Collection;
import java.util.Vector;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;
import static xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter.INCOMING;
import static xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter.OUTGOING;

@Getter
public abstract class Verbose implements JVMProfiler {
  protected final @NotNull Collection<String> subscribers = new Vector<>(0);
  private final SystemTimer secondTimer = new SystemTimer();
  protected int joinsPerSecond, totalJoins;
  private int lastTotalJoins, animationIndex;

  // Run action bar verbose
  public final void update() {
    // Clean up all blacklisted IPs
    Sonar.get().getFallback().getBlacklisted().cleanUp(false);

    totalJoins = Statistics.TOTAL_TRAFFIC.get();

    // Statistically determine the joins per second without any caches
    if (totalJoins > 0 && secondTimer.elapsed(1000L)) {
      secondTimer.reset();
      joinsPerSecond = totalJoins - lastTotalJoins;
      lastTotalJoins = totalJoins;
    }

    // Send the action bar to all online players
    broadcast(prepareActionBarFormat());
  }

  protected @NotNull String prepareActionBarFormat() {
    return Sonar.get().getConfig().actionBarLayout
      .replace("%queued%",
        DECIMAL_FORMAT.format(Sonar.get().getFallback().getQueue().getQueuedPlayers().size()))
      .replace("%verifying%", DECIMAL_FORMAT.format(Sonar.get().getFallback().getConnected().size()))
      .replace("%blacklisted%",
        DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklisted().estimatedSize()))
      .replace("%total-joins%", DECIMAL_FORMAT.format(totalJoins))
      .replace("%per-second-joins%", DECIMAL_FORMAT.format(joinsPerSecond))
      .replace("%verify-total%", DECIMAL_FORMAT.format(Statistics.REAL_TRAFFIC.get()))
      .replace("%verify-success%",
        DECIMAL_FORMAT.format(Sonar.get().getVerifiedPlayerController().estimatedSize()))
      .replace("%verify-failed%", DECIMAL_FORMAT.format(Statistics.FAILED_VERIFICATIONS.get()))
      .replace("%incoming-traffic%", INCOMING.getCachedSecond())
      .replace("%outgoing-traffic%", OUTGOING.getCachedSecond())
      .replace("%incoming-traffic-ttl%", INCOMING.getCachedTtl())
      .replace("%outgoing-traffic-ttl%", OUTGOING.getCachedTtl())
      .replace("%used-memory%", formatMemory(getUsedMemory()))
      .replace("%free-memory%", formatMemory(getFreeMemory()))
      .replace("%total-memory%", formatMemory(getTotalMemory()))
      .replace("%max-memory%", formatMemory(getMaxMemory()))
      .replace("%animation%", nextAnimation());
  }

  protected final String nextAnimation() {
    val animations = Sonar.get().getConfig().animation;
    final int nextIndex = ++animationIndex % animations.size();
    return animations.toArray(new String[0])[nextIndex];
  }

  // Run action bar verbose
  protected void broadcast(final String message) {
    // This should be replaced by the wrapper
  }

  /**
   * @param subscriber Name of the player who subscribed
   * @return Whether the player is subscribed or not
   */
  public final boolean isSubscribed(final @NotNull String subscriber) {
    return subscribers.contains(subscriber);
  }

  /**
   * @param subscriber Name of the player to subscribe
   */
  public final void subscribe(final @NotNull String subscriber) {
    subscribers.add(subscriber);
  }

  /**
   * @param subscriber Name of the player to unsubscribe
   */
  public final void unsubscribe(final @NotNull String subscriber) {
    subscribers.remove(subscriber);
  }
}
