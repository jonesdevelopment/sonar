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

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;

import java.util.Collection;
import java.util.Vector;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;

@Getter
public final class Notification implements Observable {
  private final @NotNull Collection<String> subscribers = new Vector<>(0);

  public void sendAttackNotification() {
    // No need to do anything if we have no subscribers
    if (subscribers.isEmpty()) return;
    final SonarStatistics statistics = Sonar.get().getStatistics();
    // Prepare the title
    // TODO: Why do custom title times not work?
    //  Title.Times.times(Ticks.duration(5L), Ticks.duration(60L), Ticks.duration(10L));
    final Title title = Title.title(
      Sonar.get().getConfig().getNotifications().getNotificationTitle(),
      Sonar.get().getConfig().getNotifications().getNotificationSubtitle());
    // Prepare the chat message
    final Component chat = MiniMessage.miniMessage().deserialize(
      Sonar.get().getConfig().getNotifications().getNotificationChat()
        .replace("%queued%",
          DECIMAL_FORMAT.format(Sonar.get().getFallback().getQueue().getQueuedPlayers().size()))
        .replace("%verifying%", DECIMAL_FORMAT.format(Sonar.get().getFallback().getConnected().size()))
        .replace("%blacklisted%",
          DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklist().estimatedSize()))
        .replace("%total-joins%", DECIMAL_FORMAT.format(statistics.getTotalPlayersJoined()))
        .replace("%logins-per-second%", DECIMAL_FORMAT.format(statistics.getLoginsPerSecond()))
        .replace("%connections-per-second%", DECIMAL_FORMAT.format(statistics.getConnectionsPerSecond()))
        .replace("%verify-total%", DECIMAL_FORMAT.format(statistics.getTotalAttemptedVerifications()))
        .replace("%verify-success%",
          DECIMAL_FORMAT.format(Sonar.get().getVerifiedPlayerController().estimatedSize()))
        .replace("%verify-failed%", DECIMAL_FORMAT.format(statistics.getTotalFailedVerifications()))
        .replace("%incoming-traffic%", statistics.getCurrentIncomingBandwidthFormatted())
        .replace("%outgoing-traffic%", statistics.getCurrentOutgoingBandwidthFormatted())
        .replace("%incoming-traffic-ttl%", statistics.getTotalIncomingBandwidthFormatted())
        .replace("%outgoing-traffic-ttl%", statistics.getTotalOutgoingBandwidthFormatted()));
    // Send the title and chat messages to all online players
    for (final String subscriber : Sonar.get().getNotificationHandler().getSubscribers()) {
      final Audience audience = Sonar.AUDIENCES.get(subscriber);
      if (audience == null) continue;
      audience.showTitle(title);
      audience.sendMessage(chat);
    }
  }
}
