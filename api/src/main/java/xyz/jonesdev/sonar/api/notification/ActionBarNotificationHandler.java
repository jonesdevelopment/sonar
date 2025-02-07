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

package xyz.jonesdev.sonar.api.notification;

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.api.tracker.AttackTracker;

import java.util.UUID;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;
import static xyz.jonesdev.sonar.api.profiler.SimpleProcessProfiler.*;
import static xyz.jonesdev.sonar.api.timer.SystemTimer.DATE_FORMATTER;

@Getter
public final class ActionBarNotificationHandler extends NotificationHandler {
  private int animationIndex;

  @Override
  public void handleNotification() {
    // Don't prepare component if there are no subscribers
    if (subscribers.isEmpty()) {
      return;
    }

    // Prepare the action bar verbose
    final AttackTracker.AttackStatistics attackStatistics = Sonar.get0().getAttackTracker().getCurrentAttack();
    final SystemTimer attackTimer = attackStatistics == null ? null : attackStatistics.getDuration();
    final String attackDuration = attackTimer == null ? "00:00" : DATE_FORMATTER.format(attackTimer.delay());

    final Component actionBarComponent = MiniMessage.miniMessage().deserialize(attackTimer == null
        ? Sonar.get0().getConfig().getMessagesConfig().getString("verbose.layout.normal")
        : Sonar.get0().getConfig().getMessagesConfig().getString("verbose.layout.attack"),
      Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
      Placeholder.unparsed("attack-duration", attackDuration),
      Placeholder.unparsed("animation", nextAnimation()),
      Placeholder.unparsed("queued", DECIMAL_FORMAT.format(Sonar.get0().getFallback().getQueue().getPlayers().size())),
      Placeholder.unparsed("verifying", DECIMAL_FORMAT.format(Sonar.get0().getFallback().getConnected().size())),
      Placeholder.unparsed("blacklisted", DECIMAL_FORMAT.format(Sonar.get0().getFallback().getBlacklist().estimatedSize())),
      Placeholder.unparsed("total-joins", DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getTotalPlayersJoined())),
      Placeholder.unparsed("logins-per-second", DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getLoginsPerSecond())),
      Placeholder.unparsed("connections-per-second", DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getConnectionsPerSecond())),
      Placeholder.unparsed("verify-total", DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getTotalAttemptedVerifications())),
      Placeholder.unparsed("verify-success", DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getTotalSuccessfulVerifications())),
      Placeholder.unparsed("verify-failed", DECIMAL_FORMAT.format(Sonar.get0().getStatistics().getTotalFailedVerifications())),
      Placeholder.unparsed("incoming-traffic", Sonar.get0().getStatistics().getPerSecondIncomingBandwidthFormatted()),
      Placeholder.unparsed("outgoing-traffic", Sonar.get0().getStatistics().getPerSecondOutgoingBandwidthFormatted()),
      Placeholder.unparsed("incoming-traffic-ttl", formatMemory(Sonar.get0().getStatistics().getTotalIncomingBandwidth())),
      Placeholder.unparsed("outgoing-traffic-ttl", formatMemory(Sonar.get0().getStatistics().getTotalOutgoingBandwidth())),
      Placeholder.unparsed("used-memory", formatMemory(getUsedMemory())),
      Placeholder.unparsed("free-memory", formatMemory(getFreeMemory())),
      Placeholder.unparsed("total-memory", formatMemory(getTotalMemory())),
      Placeholder.unparsed("max-memory", formatMemory(getMaxMemory())));

    // Send the action bar to all online players
    for (final UUID subscriber : subscribers) {
      final Audience audience = Sonar.get0().audience(subscriber);
      if (audience == null) continue;

      // Send the action bar to all online subscribers
      audience.sendActionBar(actionBarComponent);
    }
  }

  public String nextAnimation() {
    final var animations = Sonar.get0().getConfig().getVerboseAnimation();
    final int nextIndex = ++animationIndex % animations.size();
    return animations.toArray(new String[0])[nextIndex];
  }
}
