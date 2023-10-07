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

package xyz.jonesdev.sonar.api.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

@Getter
@RequiredArgsConstructor
public abstract class InvocationSource {
  private final String name;
  private final Audience audience;

  /**
   * Sends an empty chat message to the command executor
   */
  public final void sendMessage() {
    sendMessage(Component.empty());
  }

  /**
   * Sends a message to the command executor
   */
  public final void sendMessage(final String legacy) {
    sendMessage(MiniMessage.miniMessage().deserialize(legacy));
  }

  /**
   * Sends a message to the command executor
   */
  public final void sendMessage(final Component component) {
    audience.sendMessage(component);
  }
}
