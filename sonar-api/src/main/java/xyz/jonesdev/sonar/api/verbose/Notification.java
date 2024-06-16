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
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import xyz.jonesdev.sonar.api.Sonar;

import java.util.UUID;

import static xyz.jonesdev.sonar.api.Sonar.DECIMAL_FORMAT;
import static xyz.jonesdev.sonar.api.jvm.JVMProcessInformation.*;

@Getter
public final class Notification extends Observable {

  @Override
  public void observe() {
    // Don't prepare component if there are no subscribers
    if (subscribers.isEmpty()) {
      return;
    }

    // Prepare the chat notification
    final Component chatNotificationComponent = MiniMessage.miniMessage().deserialize(
      Sonar.get().getConfig().getMessagesConfig().getString(""),
      Placeholder.component("prefix", Sonar.get().getConfig().getPrefix()),
      Placeholder.unparsed("queued", DECIMAL_FORMAT.format(Sonar.get().getFallback().getQueue().getPlayers().size())),
      Placeholder.unparsed("verifying", DECIMAL_FORMAT.format(Sonar.get().getFallback().getConnected().size())),
      Placeholder.unparsed("blacklisted", DECIMAL_FORMAT.format(Sonar.get().getFallback().getBlacklist().estimatedSize())),
      Placeholder.unparsed("total-joins", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getTotalPlayersJoined())),
      Placeholder.unparsed("logins-per-second", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getLoginsPerSecond())),
      Placeholder.unparsed("connections-per-second", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getConnectionsPerSecond())),
      Placeholder.unparsed("verify-total", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getTotalAttemptedVerifications())),
      Placeholder.unparsed("verify-success", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getTotalSuccessfulVerifications())),
      Placeholder.unparsed("verify-failed", DECIMAL_FORMAT.format(Sonar.get().getStatistics().getTotalFailedVerifications())),
      Placeholder.unparsed("incoming-traffic", Sonar.get().getStatistics().getCurrentIncomingBandwidthFormatted()),
      Placeholder.unparsed("outgoing-traffic", Sonar.get().getStatistics().getCurrentOutgoingBandwidthFormatted()),
      Placeholder.unparsed("incoming-traffic-ttl", Sonar.get().getStatistics().getTotalIncomingBandwidthFormatted()),
      Placeholder.unparsed("outgoing-traffic-ttl", Sonar.get().getStatistics().getTotalOutgoingBandwidthFormatted()),
      Placeholder.unparsed("used-memory", formatMemory(getUsedMemory())),
      Placeholder.unparsed("free-memory", formatMemory(getFreeMemory())),
      Placeholder.unparsed("total-memory", formatMemory(getTotalMemory())),
      Placeholder.unparsed("max-memory", formatMemory(getMaxMemory())));

    // Send the title and chat messages to all online players
    for (final UUID subscriber : Sonar.get().getNotificationHandler().getSubscribers()) {
      final Audience audience = Sonar.get().audience(subscriber);
      if (audience == null) continue;

      // Send the message to all online subscribers
      audience.sendMessage(chatNotificationComponent);
      // Send the title to all online subscribers
      audience.showTitle(Sonar.get().getConfig().getNotifications().getTitle());
    }
  }
}
