/*
 * Copyright (C) 2024 Sonar Contributors
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;

import java.util.Collection;
import java.util.UUID;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;
import static xyz.jonesdev.sonar.api.jvm.JVMProcessInformation.*;

public interface Observable {
  Collection<UUID> getSubscribers();

  /**
   * @param uuid UUID of the player
   * @return Whether the audience is subscribed or not
   */
  default boolean isSubscribed(final @NotNull UUID uuid) {
    return getSubscribers().contains(uuid);
  }

  /**
   * @param uuid UUID of the player
   */
  default void subscribe(final UUID uuid) {
    getSubscribers().add(uuid);
  }

  /**
   * @param uuid UUID of the player
   */
  default void unsubscribe(final UUID uuid) {
    getSubscribers().remove(uuid);
  }

  // TODO: Make this a bit prettier and better for performance
  default @NotNull Component replaceStatistic(final @NotNull Component component) {
    final SonarStatistics statistics = Sonar.get().getStatistics();
    return component
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
        .build());
  }
}
