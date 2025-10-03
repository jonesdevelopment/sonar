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

package xyz.jonesdev.sonar.api.fallback.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum ProtocolVersion {
  UNKNOWN(-1, "Unknown"),
  MINECRAFT_1_7_2(4, "1.7.2"),
  MINECRAFT_1_7_6(5, "1.7.6"),
  MINECRAFT_1_8(47, "1.8"),
  MINECRAFT_1_9(107, "1.9"),
  MINECRAFT_1_9_1(108, "1.9.1"),
  MINECRAFT_1_9_2(109, "1.9.2"),
  MINECRAFT_1_9_4(110, "1.9.3"),
  MINECRAFT_1_10(210, "1.10"),
  MINECRAFT_1_11(315, "1.11"),
  MINECRAFT_1_11_1(316, "1.11.1"),
  MINECRAFT_1_12(335, "1.12"),
  MINECRAFT_1_12_1(338, "1.12.1"),
  MINECRAFT_1_12_2(340, "1.12.2"),
  MINECRAFT_1_13(393, "1.13"),
  MINECRAFT_1_13_1(401, "1.13.1"),
  MINECRAFT_1_13_2(404, "1.13.2"),
  MINECRAFT_1_14(477, "1.14"),
  MINECRAFT_1_14_1(480, "1.14.1"),
  MINECRAFT_1_14_2(485, "1.14.2"),
  MINECRAFT_1_14_3(490, "1.14.3"),
  MINECRAFT_1_14_4(498, "1.14.4"),
  MINECRAFT_1_15(573, "1.15"),
  MINECRAFT_1_15_1(575, "1.15.1"),
  MINECRAFT_1_15_2(578, "1.15.2"),
  MINECRAFT_1_16(735, "1.16"),
  MINECRAFT_1_16_1(736, "1.16.1"),
  MINECRAFT_1_16_2(751, "1.16.2"),
  MINECRAFT_1_16_3(753, "1.16.3"),
  MINECRAFT_1_16_4(754, "1.16.4"),
  MINECRAFT_1_17(755, "1.17"),
  MINECRAFT_1_17_1(756, "1.17.1"),
  MINECRAFT_1_18(757, "1.18"),
  MINECRAFT_1_18_2(758, "1.18.2"),
  MINECRAFT_1_19(759, "1.19"),
  MINECRAFT_1_19_1(760, "1.19.1"),
  MINECRAFT_1_19_3(761, "1.19.3"),
  MINECRAFT_1_19_4(762, "1.19.4"),
  MINECRAFT_1_20(763, "1.20"),
  MINECRAFT_1_20_2(764, "1.20.2"),
  MINECRAFT_1_20_3(765, "1.20.3"),
  MINECRAFT_1_20_5(766, "1.20.5"),
  MINECRAFT_1_21(767, "1.21"),
  MINECRAFT_1_21_2(768, "1.21.2"),
  MINECRAFT_1_21_4(769, "1.21.4"),
  MINECRAFT_1_21_5(770, "1.21.5"),
  MINECRAFT_1_21_6(771, "1.21.6"),
  MINECRAFT_1_21_7(772, "1.21.7"),
  MINECRAFT_1_21_9(773, "1.21.9");

  private final int protocol;
  private final String name;

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

  public boolean greaterThan(final ProtocolVersion that) {
    return compareTo(that) > 0;
  }

  public boolean greaterThanOrEquals(final ProtocolVersion that) {
    return compareTo(that) >= 0;
  }

  public boolean lessThan(final ProtocolVersion that) {
    return compareTo(that) < 0;
  }

  public boolean lessThanOrEquals(final ProtocolVersion that) {
    return compareTo(that) <= 0;
  }

  public boolean inBetween(final ProtocolVersion first, final ProtocolVersion last) {
    return compareTo(first) >= 0 && compareTo(last) <= 0;
  }

  public boolean isUnknown() {
    return this == UNKNOWN;
  }
}
