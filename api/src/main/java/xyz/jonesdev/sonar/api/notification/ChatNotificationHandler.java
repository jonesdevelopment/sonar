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
import net.kyori.adventure.title.Title;
import xyz.jonesdev.sonar.api.Sonar;

import java.util.List;
import java.util.UUID;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;
import static xyz.jonesdev.sonar.api.profiler.SimpleProcessProfiler.*;

@Getter
public final class ChatNotificationHandler extends NotificationHandler {

  @Override
  public void handleNotification() {
    // Don't prepare component if there are no subscribers
    if (subscribers.isEmpty()) {
      return;
    }

    // Prepare the chat notification
    final Component notificationTitle = MiniMessage.miniMessage().deserialize(
      Sonar.get0().getConfig().getMessagesConfig().getString("notifications.title"));
    final Component notificationSubtitle = MiniMessage.miniMessage().deserialize(
      Sonar.get0().getConfig().getMessagesConfig().getString("notifications.subtitle"));
    final Title title = Title.title(notificationTitle, notificationSubtitle);

    final List<String> chatNotification = Sonar.get0().getConfig().getMessagesConfig().getStringList("notifications.chat");
    final Component[] chatNotificationComponents = new Component[chatNotification.size()];

    for (int i = 0; i < chatNotificationComponents.length; i++) {
      chatNotificationComponents[i] = MiniMessage.miniMessage().deserialize(chatNotification.get(i),
        Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
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
    }

    // Send the title and chat messages to all online players
    for (final UUID subscriber : Sonar.get0().getChatNotificationHandler().getSubscribers()) {
      final Audience audience = Sonar.get0().audience(subscriber);
      if (audience == null) continue;

      // Send the message to all online subscribers
      for (Component component : chatNotificationComponents) {
        audience.sendMessage(component);
      }
      // Send the title to all online subscribers
      audience.showTitle(title);
    }
  }
}
