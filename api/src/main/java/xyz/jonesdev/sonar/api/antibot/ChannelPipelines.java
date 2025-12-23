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

package xyz.jonesdev.sonar.api.antibot;

public interface ChannelPipelines {
  String SONAR_INACTIVE_LISTENER = "sonar-inactive-listener";
  String SONAR_INBOUND_HANDLER = "sonar-inbound-handler";
  String SONAR_FRAME_DECODER = "sonar-frame-decoder";
  String SONAR_FRAME_ENCODER = "sonar-frame-encoder";
  String SONAR_TIMEOUT = "sonar-timeout";
  String SONAR_PACKET_HANDLER = "sonar-packet-handler";
  String SONAR_PACKET_ENCODER = "sonar-packet-encoder";
  String SONAR_PACKET_DECODER = "sonar-packet-decoder";
  String SONAR_TAIL_EXCEPTIONS = "sonar-exception-tail";
  String SONAR_BANDWIDTH = "sonar-bandwidth-counter";
}
