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

package xyz.jonesdev.sonar.api.statistics;

@SuppressWarnings("unused")
public interface SonarStatistics {
  long getConnectionsPerSecond();

  long getLoginsPerSecond();

  long getCurrentIncomingBandwidth();

  long getCurrentOutgoingBandwidth();

  long getTotalIncomingBandwidth();

  long getTotalOutgoingBandwidth();

  String getPerSecondIncomingBandwidthFormatted();

  String getPerSecondOutgoingBandwidthFormatted();

  int getTotalPlayersJoined();

  int getTotalPlayersVerified();

  int getTotalSuccessfulVerifications();

  int getTotalFailedVerifications();

  long getCurrentAttemptedVerifications();

  int getTotalAttemptedVerifications();

  long getCurrentBlacklistSize();

  long getTotalBlacklistSize();
}
