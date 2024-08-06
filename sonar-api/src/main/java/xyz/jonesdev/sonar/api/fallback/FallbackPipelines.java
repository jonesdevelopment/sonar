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

public interface FallbackPipelines {
  String FALLBACK_INACTIVE_LISTENER = "sonar-inactive-listener";
  String FALLBACK_INBOUND_HANDLER = "sonar-inbound-handler";
  String FALLBACK_FRAME_DECODER = "sonar-frame-decoder";
  String FALLBACK_FRAME_ENCODER = "sonar-frame-encoder";
  String FALLBACK_TIMEOUT = "sonar-timeout";
  String FALLBACK_PACKET_HANDLER = "sonar-packet-handler";
  String FALLBACK_PACKET_ENCODER = "sonar-packet-encoder";
  String FALLBACK_PACKET_DECODER = "sonar-packet-decoder";
  String FALLBACK_TAIL_EXCEPTIONS = "sonar-exception-tail";
  String FALLBACK_BANDWIDTH = "sonar-bandwidth-counter";
}
