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

package xyz.jonesdev.sonar.common.fallback.protocol.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.function.Function;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;

// TODO: load mappings from a separate file
@Getter
@RequiredArgsConstructor
public enum EntityType {
  // Useful resources:
  // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
  // - https://pokechu22.github.io/Burger/1.20.4.html
  BOAT(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 1;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_14_4)) {
      return 5;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 6;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 7;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 8;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20_3)) {
      return 9;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 10;
    }
    // 1.21.2 split the boat type in id registries.
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 85;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_5)) {
      return 84;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 85;
    }
    return 87;
  }),
  MINECART(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 10;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_14_4)) {
      return 41;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 42;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 45;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 50;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 53;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 54;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20_2)) {
      return 64;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20_3)) {
      return 65;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 69;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 82;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_5)) {
      return 81;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 82;
    }
    return 84;
  });

  private final Function<ProtocolVersion, Integer> id;
}
