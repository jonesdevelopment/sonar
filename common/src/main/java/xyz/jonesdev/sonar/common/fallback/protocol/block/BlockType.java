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

package xyz.jonesdev.sonar.common.fallback.protocol.block;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.function.Function;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;

// TODO: load mappings from a separate file
@Getter
@SuppressWarnings("unused")
@RequiredArgsConstructor
public enum BlockType {
  // Useful resources:
  // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
  // - https://pokechu22.github.io/Burger/
  // - https://github.com/ViaVersion/Mappings/tree/main/mappings
  ENCHANTMENT_TABLE(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_12_2)) {
      return 116;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_1)) {
      return 4612;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 4613;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 5116;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_1)) {
      return 5132;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 5136;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 5333;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 5719;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 7159;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_4)) {
      return 7385;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 7389;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 7619;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 8163;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 8173;
    }
    return 9250;
  }, protocolVersion -> (double) 0.75f),
  TRAPDOOR(protocolVersion -> {
    // We have to use wooden trapdoors for 1.7 since 1.7 doesn't have iron trapdoors
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_7_6)) {
      return 96;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_12_2)) {
      return 167;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_1)) {
      return 6509;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 6510;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 7016;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_1)) {
      return 7552;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 7556;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 7802;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 8293;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 9937;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_4)) {
      return 10269;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20)) {
      return 10273;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 10414;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 10749;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 11293;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 11303;
    }
    return 12380;
  }, protocolVersion -> 0.1875),
  END_PORTAL_FRAME(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_12_2)) {
      return 120;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_1)) {
      return 4633;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 4634;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 5137;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_1)) {
      return 5153;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 5157;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 5358;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 5744;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 7184;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_4)) {
      return 7410;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 7414;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 7644;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 8185;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 8195;
    }
    return 9272;
  }, protocolVersion -> 0.8125),
  DAYLIGHT_SENSOR(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_12_2)) {
      return 151;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_1)) {
      return 5651;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 5652;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 6158;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_1)) {
      return 6694;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 6698;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 6916;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 7327;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 8811;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_4)) {
      return 9063;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20)) {
      return 9067;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 9207;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 9462;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 10006;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 10016;
    }
    return 11093;
  }, protocolVersion -> 0.375),
  COBBLESTONE_WALL(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_12_2)) {
      return 139;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_1)) {
      return 5196;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 5197;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 5700;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_1)) {
      return 5660;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 5664;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 5866;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 6252;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 7692;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_4)) {
      return 7918;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 7922;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 8152;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 8696;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 8706;
    }
    return 9783;
  }, protocolVersion -> 1.5),
  STONE_SLABS(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_12_2)) {
      return 44;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_1)) {
      return 7296;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 7297;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 7809;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_1)) {
      return 8345;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 8349;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 8595;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 9092;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 10748;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_4)) {
      return 11086;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20)) {
      return 11090;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 11231;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 11566;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 12110;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 12120;
    }
    return 13197;
  }, protocolVersion -> 0.5),
  WHITE_CARPET(protocolVersion -> {
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_12_2)) {
      return 171;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_1)) {
      return 6823;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_13_2)) {
      return 6824;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_15_2)) {
      return 7330;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_1)) {
      return 7866;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_16_4)) {
      return 7870;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_18_2)) {
      return 8116;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_1)) {
      return 8607;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_3)) {
      return 10251;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_19_4)) {
      return 10583;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_20)) {
      return 10587;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21)) {
      return 10728;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_2)) {
      return 11063;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_4)) {
      return 11607;
    }
    if (protocolVersion.lessThanOrEquals(MINECRAFT_1_21_7)) {
      return 11617;
    }
    return 12694;
  }, protocolVersion -> protocolVersion.compareTo(MINECRAFT_1_8) < 0 ? 0 : 0.0625);

  private final Function<ProtocolVersion, Integer> id;
  private final Function<ProtocolVersion, Double> blockHeight;
}
