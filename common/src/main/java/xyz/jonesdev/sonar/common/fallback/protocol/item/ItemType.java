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

package xyz.jonesdev.sonar.common.fallback.protocol.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.function.Function;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;

// TODO: load mappings from a separate file
@Getter
@RequiredArgsConstructor
public enum ItemType {
  // Useful resources:
  // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
  // - https://pokechu22.github.io/Burger/1.20.4.html
  // - https://github.com/ViaVersion/Mappings/tree/main/mappings
  FILLED_MAP(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_12_2)) {
      return 358;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_1)) {
      return 608;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 613;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 671;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 733;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 847;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 886;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 914;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_4)) {
      return 937;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20_2)) {
      return 941;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20_3)) {
      return 979;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 982;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 1022;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 1031;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_5)) {
      return 1042;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 1059;
    }
    return 1104;
  }, protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 26;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 36;
    }
    return 37;
  });

  private final Function<ProtocolVersion, Integer> id;
  private final Function<ProtocolVersion, Integer> components;
}
