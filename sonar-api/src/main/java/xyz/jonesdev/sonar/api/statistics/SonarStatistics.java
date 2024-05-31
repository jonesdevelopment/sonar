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

package xyz.jonesdev.sonar.api.statistics;

import org.jetbrains.annotations.ApiStatus;

@SuppressWarnings("unused")
public interface SonarStatistics {

  /**
   * @return Number of connection establishments per second
   */
  long getConnectionsPerSecond();

  /**
   * @return Number of logins per second
   */
  long getLoginsPerSecond();

  /**
   * @return Currently incoming bytes per second
   */
  long getCurrentIncomingBandwidth();

  /**
   * @return Currently outgoing bytes per second
   */
  long getCurrentOutgoingBandwidth();

  /**
   * @return Total incoming bytes per second
   */
  long getTotalIncomingBandwidth();

  /**
   * @return Total outgoing bytes per second
   */
  long getTotalOutgoingBandwidth();

  /**
   * @return Same as {@link SonarStatistics#getCurrentIncomingBandwidth} but formatted
   */
  String getCurrentIncomingBandwidthFormatted();

  /**
   * @return Same as {@link SonarStatistics#getCurrentOutgoingBandwidth} but formatted
   */
  String getCurrentOutgoingBandwidthFormatted();

  /**
   * @return Same as {@link SonarStatistics#getTotalIncomingBandwidth} but formatted
   */
  String getTotalIncomingBandwidthFormatted();

  /**
   * @return Same as {@link SonarStatistics#getTotalOutgoingBandwidth} but formatted
   */
  String getTotalOutgoingBandwidthFormatted();

  /**
   * @return Total number of players who logged into the server
   */
  int getTotalPlayersJoined();

  /**
   * @return Total number of players who tried to verify
   */
  int getTotalPlayersVerified();

  /**
   * @return Total number of players who passed the verification
   */
  int getTotalSuccessfulVerifications();

  /**
   * @return Total number of players who failed the verification
   */
  int getTotalFailedVerifications();

  /**
   * @return Total number of players who passed the verification
   */
  long getCurrentAttemptedVerifications();

  /**
   * @return Total number of players who tried verifying
   */
  int getTotalAttemptedVerifications();

  /**
   * @return Current number of IP addresses that are blacklisted
   */
  long getCurrentBlacklistSize();

  /**
   * @return Total number of cases where an IP was blacklisted
   * @apiNote This does not describe the current number of blacklisted IPs
   */
  long getTotalBlacklistSize();

  @ApiStatus.Internal
  void cleanUpCache();
}
