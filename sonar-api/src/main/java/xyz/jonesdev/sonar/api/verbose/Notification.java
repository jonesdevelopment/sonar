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
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;

import java.util.Collection;
import java.util.UUID;
import java.util.Vector;

@Getter
public final class Notification implements Observable {
  private final @NotNull Collection<UUID> subscribers = new Vector<>(0);

  public void sendAttackNotification() {
    // Don't prepare component if there are no subscribers
    Component chat = null;
    // Send the title and chat messages to all online players
    for (final UUID subscriber : Sonar.get().getNotificationHandler().getSubscribers()) {
      final Audience audience = Sonar.get().audience(subscriber);
      if (audience == null) continue;

      // Only prepare component if there are subscribers
      if (chat == null) {
        chat = replaceStatistic(Sonar.get().getConfig().getNotifications().getNotificationChat());
      }
      // Send the message to all online subscribers
      audience.sendMessage(chat);
      // Send the title to all online subscribers
      audience.showTitle(Sonar.get().getConfig().getNotifications().getTitle());
    }
  }
}
