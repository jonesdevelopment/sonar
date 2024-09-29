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
  //STONE(protocolVersion -> 1, 1),
  ENCHANTMENT_TABLE(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_12_2) <= 0) {
      return 116;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_1) <= 0) {
      return 4612;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 4613;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 5116;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_1) <= 0) {
      return 5132;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 5136;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 5333;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 5719;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 7159;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return 7385;
    }
    return 7389;
  }, protocolVersion -> (double) 0.75f),
  TRAPDOOR(protocolVersion -> {
    // We have to use wooden trapdoors for 1.7 since 1.7 doesn't have iron trapdoors
    if (protocolVersion.compareTo(MINECRAFT_1_7_6) <= 0) {
      return 96;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_12_2) <= 0) {
      return 167;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_1) <= 0) {
      return 6509;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 6510;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 7016;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_1) <= 0) {
      return 7552;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 7556;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 7802;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 8293;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 9937;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return 10269;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20) <= 0) {
      return 10273;
    }
    return 10414;
  }, protocolVersion -> 0.1875),
  END_PORTAL_FRAME(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_12_2) <= 0) {
      return 120;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_1) <= 0) {
      return 4633;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 4634;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 5137;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_1) <= 0) {
      return 5153;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 5157;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 5358;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 5744;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 7184;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return 7410;
    }
    return 7414;
  }, protocolVersion -> 0.8125),
  DAYLIGHT_SENSOR(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_12_2) <= 0) {
      return 151;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_1) <= 0) {
      return 5651;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 5652;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 6158;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_1) <= 0) {
      return 6694;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 6698;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 6916;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 7327;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 8811;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return 9063;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20) <= 0) {
      return 9067;
    }
    return 9207;
  }, protocolVersion -> 0.375),
  COBBLESTONE_WALL(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_12_2) <= 0) {
      return 139;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_1) <= 0) {
      return 5196;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 5197;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 5700;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_1) <= 0) {
      return 5660;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 5664;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 5866;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 6252;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 7692;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return 7918;
    }
    return 7922;
  }, protocolVersion -> 1.5),
  STONE_SLABS(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_12_2) <= 0) {
      return 44;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_1) <= 0) {
      return 7296;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 7297;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 7809;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_1) <= 0) {
      return 8345;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 8349;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 8595;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 9092;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 10748;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return 11086;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20) <= 0) {
      return 11090;
    }
    return 11231;
  }, protocolVersion -> 0.5),
  WHITE_CARPET(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_12_2) <= 0) {
      return 171;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_1) <= 0) {
      return 6823;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 6824;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 7330;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_1) <= 0) {
      return 7866;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 7870;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 8116;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 8607;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 10251;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return 10583;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20) <= 0) {
      return 10587;
    }
    return 10728;
  }, protocolVersion -> protocolVersion.compareTo(MINECRAFT_1_8) < 0 ? 0 : 0.0625);

  private final Function<ProtocolVersion, Integer> id;
  private final Function<ProtocolVersion, Double> blockHeight;
}
