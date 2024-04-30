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

package xyz.jonesdev.sonar.api.fallback;

public enum FallbackUserState {
  // 1.20.2 configuration state
  LOGIN_ACK, CONFIGURE,
  // pre-JOIN ping check
  KEEP_ALIVE,
  // post-JOIN checks
  CLIENT_SETTINGS, PLUGIN_MESSAGE, TRANSACTION,
  // PLAY checks
  TELEPORT, POSITION,
  // Vehicle check
  VEHICLE,
  // CAPTCHA
  CAPTCHA,
  // Done
  SUCCESS,
  // Placeholder
  FAILED;

  public boolean canReceivePackets() {
    return this != FAILED && this != SUCCESS;
  }

  public boolean shouldExpectMovement() {
    return this != LOGIN_ACK && this != VEHICLE && this != CAPTCHA;
  }
}
