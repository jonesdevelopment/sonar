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
import net.kyori.adventure.text.TextReplacementConfig;
import org.jetbrains.annotations.ApiStatus;
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
import static xyz.jonesdev.sonar.api.timer.SystemTimer.DATE_FORMATTER;

@Getter
public final class Verbose implements Observable {
  private final @NotNull Collection<UUID> subscribers = new Vector<>(0);
  private int animationIndex;

  // Run action bar verbose
  public void update() {
    // Make sure to clean up the cached statistics
    Sonar.get().getStatistics().cleanUpCache();

    // Don't prepare component if there are no subscribers
    Component component = null;
    for (final UUID subscriber : subscribers) {
      final Audience audience = Sonar.get().audience(subscriber);
      if (audience == null) continue;

      // Only prepare component if there are subscribers
      if (component == null) {
        component = prepareActionBarFormat();
      }
      // Send the action bar to all online subscribers
      audience.sendActionBar(component);
    }
  }

  @ApiStatus.Internal
  public @NotNull Component prepareActionBarFormat() {
    final SonarStatistics statistics = Sonar.get().getStatistics();
    final AttackTracker.AttackStatistics attackStatistics = Sonar.get().getAttackTracker().getCurrentAttack();
    final SystemTimer attackDuration = attackStatistics == null ? null : attackStatistics.getDuration();
    return (attackDuration != null
      ? Sonar.get().getConfig().getVerbose().getActionBarLayoutDuringAttack()
      : Sonar.get().getConfig().getVerbose().getActionBarLayout())
      // TODO: Make this a bit prettier and better for performance
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%queued%")
        .replacement(DECIMAL_FORMAT.format(Sonar.get().getFallback().getQueue().getQueuedPlayers().size()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%verifying%")
        .replacement(DECIMAL_FORMAT.format(Sonar.get().getFallback().getConnected().size()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%blacklisted%")
        .replacement(DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklist().estimatedSize()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%total-joins%")
        .replacement(DECIMAL_FORMAT.format(statistics.getTotalPlayersJoined()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%logins-per-second%")
        .replacement(DECIMAL_FORMAT.format(statistics.getLoginsPerSecond()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%connections-per-second%")
        .replacement(DECIMAL_FORMAT.format(statistics.getConnectionsPerSecond()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%verify-total%")
        .replacement(DECIMAL_FORMAT.format(statistics.getTotalAttemptedVerifications()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%verify-success%")
        .replacement(DECIMAL_FORMAT.format(statistics.getTotalSuccessfulVerifications()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%verify-failed%")
        .replacement(DECIMAL_FORMAT.format(statistics.getTotalFailedVerifications()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%incoming-traffic%")
        .replacement(statistics.getCurrentIncomingBandwidthFormatted())
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%outgoing-traffic%")
        .replacement(statistics.getCurrentOutgoingBandwidthFormatted())
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%incoming-traffic-ttl%")
        .replacement(statistics.getTotalIncomingBandwidthFormatted())
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%outgoing-traffic-ttl%")
        .replacement(statistics.getTotalOutgoingBandwidthFormatted())
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%used-memory%")
        .replacement(formatMemory(getUsedMemory()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%free-memory%")
        .replacement(formatMemory(getFreeMemory()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%total-memory%")
        .replacement(formatMemory(getTotalMemory()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%max-memory%")
        .replacement(formatMemory(getMaxMemory()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%attack-duration%")
        .replacement(attackDuration == null ? "00:00" : DATE_FORMATTER.format(attackDuration.delay()))
        .build())
      .replaceText(TextReplacementConfig.builder().once().matchLiteral("%animation%")
        .replacement(nextAnimation())
        .build());
  }

  public String nextAnimation() {
    val animations = Sonar.get().getConfig().getVerbose().getAnimation();
    final int nextIndex = ++animationIndex % animations.size();
    return animations.toArray(new String[0])[nextIndex];
  }
}
