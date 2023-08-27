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

public interface InvocationSender {

  /**
   * @return The name of the command executor
   */
  String getName();

  /**
   * Sends a message to the command executor
   *
   * @param message Deserialized message
   */
  void sendMessage(final String message);

  /**
   * Sends an empty chat message to the command executor
   */
  default void sendMessage() {
    sendMessage("");
  }
}
