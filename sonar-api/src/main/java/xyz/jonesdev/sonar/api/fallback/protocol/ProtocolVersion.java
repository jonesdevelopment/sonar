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

package xyz.jonesdev.sonar.api.fallback.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum ProtocolVersion {
  UNKNOWN(-1),
  MINECRAFT_1_7_2(4),
  MINECRAFT_1_7_6(5),
  MINECRAFT_1_8(47),
  MINECRAFT_1_9(107),
  MINECRAFT_1_9_1(108),
  MINECRAFT_1_9_2(109),
  MINECRAFT_1_9_4(110),
  MINECRAFT_1_10(210),
  MINECRAFT_1_11(315),
  MINECRAFT_1_11_1(316),
  MINECRAFT_1_12(335),
  MINECRAFT_1_12_1(338),
  MINECRAFT_1_12_2(340),
  MINECRAFT_1_13(393),
  MINECRAFT_1_13_1(401),
  MINECRAFT_1_13_2(404),
  MINECRAFT_1_14(477),
  MINECRAFT_1_14_1(480),
  MINECRAFT_1_14_2(485),
  MINECRAFT_1_14_3(490),
  MINECRAFT_1_14_4(498),
  MINECRAFT_1_15(573),
  MINECRAFT_1_15_1(575),
  MINECRAFT_1_15_2(578),
  MINECRAFT_1_16(735),
  MINECRAFT_1_16_1(736),
  MINECRAFT_1_16_2(751),
  MINECRAFT_1_16_3(753),
  MINECRAFT_1_16_4(754),
  MINECRAFT_1_17(755),
  MINECRAFT_1_17_1(756),
  MINECRAFT_1_18(757),
  MINECRAFT_1_18_2(758),
  MINECRAFT_1_19(759),
  MINECRAFT_1_19_1(760),
  MINECRAFT_1_19_3(761),
  MINECRAFT_1_19_4(762),
  MINECRAFT_1_20(763),
  MINECRAFT_1_20_2(764),
  MINECRAFT_1_20_3(765),
  MINECRAFT_1_20_5(766),
  MINECRAFT_1_21(767);

  private final int protocol;

  public static final ProtocolVersion LATEST_VERSION;
  public static final Map<Integer, ProtocolVersion> ID_TO_PROTOCOL_CONSTANT;

  static {
    final Map<Integer, ProtocolVersion> versions = new HashMap<>();

    for (final ProtocolVersion version : values()) {
      versions.putIfAbsent(version.protocol, version);
    }

    ID_TO_PROTOCOL_CONSTANT = versions;
    LATEST_VERSION = values()[values().length - 1];
  }

  public static ProtocolVersion fromId(final int protocol) {
    return ID_TO_PROTOCOL_CONSTANT.getOrDefault(protocol, UNKNOWN);
  }

  public boolean inBetween(final ProtocolVersion first, final ProtocolVersion last) {
    return compareTo(first) >= 0 && compareTo(last) <= 0;
  }

  public boolean isUnknown() {
    return this == UNKNOWN;
  }
}
