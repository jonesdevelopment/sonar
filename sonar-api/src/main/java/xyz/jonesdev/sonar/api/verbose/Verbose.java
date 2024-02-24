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
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.util.Collection;
import java.util.UUID;
import java.util.Vector;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;
import static xyz.jonesdev.sonar.api.jvm.JVMProcessInformation.*;

@Getter
public final class Verbose implements Observable {
  private final @NotNull Collection<UUID> subscribers = new Vector<>(0);
  private int animationIndex;

  // Run action bar verbose
  public void update() {
    // Make sure to clean up the cached statistics
    Sonar.get().getStatistics().cleanUpCache();
    // Don't prepare component if there are no subscribers
    if (subscribers.isEmpty()) return;
    // Prepare the action bar format component
    final Component component = prepareActionBarFormat();
    // Send the action bar to all online players
    for (final UUID subscriber : subscribers) {
      final Audience audience = Sonar.get().audience(subscriber);
      if (audience == null) continue;
      audience.sendActionBar(component);
    }
  }

  public @NotNull Component prepareActionBarFormat() {
    final SonarStatistics statistics = Sonar.get().getStatistics();
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
      .replace("%blacklisted%", DECIMAL_FORMAT.format(statistics.getCurrentBlacklistSize()))
      .replace("%total-joins%", DECIMAL_FORMAT.format(statistics.getTotalPlayersJoined()))
      .replace("%logins-per-second%", DECIMAL_FORMAT.format(statistics.getLoginsPerSecond()))
      .replace("%connections-per-second%", DECIMAL_FORMAT.format(statistics.getConnectionsPerSecond()))
      .replace("%verify-total%", DECIMAL_FORMAT.format(statistics.getTotalAttemptedVerifications()))
      .replace("%verify-success%",
        DECIMAL_FORMAT.format(Sonar.get().getVerifiedPlayerController().estimatedSize()))
      .replace("%verify-failed%", DECIMAL_FORMAT.format(statistics.getTotalFailedVerifications()))
      .replace("%attack-duration%", attackDuration == null ? "00:00" : attackDuration.formattedDelay())
      .replace("%incoming-traffic%", statistics.getCurrentIncomingBandwidthFormatted())
      .replace("%outgoing-traffic%", statistics.getCurrentOutgoingBandwidthFormatted())
      .replace("%incoming-traffic-ttl%", statistics.getTotalIncomingBandwidthFormatted())
      .replace("%outgoing-traffic-ttl%", statistics.getTotalOutgoingBandwidthFormatted())
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
