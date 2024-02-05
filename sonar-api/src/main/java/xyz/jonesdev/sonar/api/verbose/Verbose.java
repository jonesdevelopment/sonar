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

package xyz.jonesdev.sonar.api.verbose;

import lombok.Getter;
import lombok.val;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.attack.AttackTracker;
import xyz.jonesdev.sonar.api.profiler.JVMProfiler;
import xyz.jonesdev.sonar.api.statistics.Counters;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.util.Collection;
import java.util.Vector;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;
import static xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter.INCOMING;
import static xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter.OUTGOING;

@Getter
public final class Verbose implements Observable, JVMProfiler, Counters {
  private final @NotNull Collection<String> subscribers = new Vector<>(0);
  private int animationIndex;

  // Run action bar verbose
  public void update() {
    // Clean up all blacklisted IPs
    Sonar.get().getFallback().getBlacklist().cleanUp(false);
    // Clean up all counters
    LOGINS_PER_SECOND.cleanUp(false);
    CONNECTIONS_PER_SECOND.cleanUp(false);

    // Don't prepare component if there are no subscribers
    if (subscribers.isEmpty()) return;
    // Prepare the action bar format component
    final Component component = prepareActionBarFormat();
    // Send the action bar to all online players
    for (final String subscriber : subscribers) {
      final Audience audience = Sonar.AUDIENCES.get(subscriber);
      if (audience == null) continue;
      audience.sendActionBar(component);
    }
  }

  public @NotNull Component prepareActionBarFormat() {
    final AttackTracker.AttackStatistics attackStatistics = Sonar.get().getAttackTracker().getCurrentAttack();
    final SystemTimer attackDuration = attackStatistics == null ? null : attackStatistics.getDuration();
    return MiniMessage.miniMessage().deserialize((attackDuration != null
      ? Sonar.get().getConfig().getVerbose().getActionBarLayoutDuringAttack()
      : Sonar.get().getConfig().getVerbose().getActionBarLayout())
      .replace("%queued%",
        DECIMAL_FORMAT.format(Sonar.get().getFallback().getQueue().getQueuedPlayers().size()))
      .replace("%verifying%", DECIMAL_FORMAT.format(Sonar.get().getFallback().getConnected().size()))
      .replace("%blacklisted%",
        DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklist().estimatedSize()))
      .replace("%total-joins%", DECIMAL_FORMAT.format(Statistics.TOTAL_TRAFFIC.get()))
      .replace("%logins-per-second%", DECIMAL_FORMAT.format(LOGINS_PER_SECOND.estimatedSize()))
      .replace("%connections-per-second%", DECIMAL_FORMAT.format(CONNECTIONS_PER_SECOND.estimatedSize()))
      .replace("%verify-total%", DECIMAL_FORMAT.format(Statistics.REAL_TRAFFIC.get()))
      .replace("%verify-success%",
        DECIMAL_FORMAT.format(Sonar.get().getVerifiedPlayerController().estimatedSize()))
      .replace("%verify-failed%", DECIMAL_FORMAT.format(Statistics.FAILED_VERIFICATIONS.get()))
      .replace("%attack-duration%", attackDuration == null ? "00:00" : attackDuration.formattedDelay())
      .replace("%incoming-traffic%", INCOMING.getCachedSecond())
      .replace("%outgoing-traffic%", OUTGOING.getCachedSecond())
      .replace("%incoming-traffic-ttl%", INCOMING.getCachedTtl())
      .replace("%outgoing-traffic-ttl%", OUTGOING.getCachedTtl())
      .replace("%used-memory%", formatMemory(getUsedMemory()))
      .replace("%free-memory%", formatMemory(getFreeMemory()))
      .replace("%total-memory%", formatMemory(getTotalMemory()))
      .replace("%max-memory%", formatMemory(getMaxMemory()))
      .replace("%animation%", nextAnimation()));
  }

  public String nextAnimation() {
    val animations = Sonar.get().getConfig().getVerbose().getAnimation();
    final int nextIndex = ++animationIndex % animations.size();
    return animations.toArray(new String[0])[nextIndex];
  }
}
