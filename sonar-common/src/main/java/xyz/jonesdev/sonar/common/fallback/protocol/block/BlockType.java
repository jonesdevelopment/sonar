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
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.function.Function;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public enum BlockType {
  // Useful resources:
  // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
  // - https://pokechu22.github.io/Burger/
  // - https://github.com/ViaVersion/Mappings/tree/main/mappings
  STONE(protocolVersion -> 1, 1),
  ENCHANTMENT_TABLE(protocolVersion -> {
    switch (protocolVersion) {
      // 1.13-1.13.1
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
        return 4612;
      // 1.13.2-1.14.3
      case MINECRAFT_1_13_2:
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
        return 4613;
      // 1.14.4-1.15.2
      case MINECRAFT_1_14_4:
      case MINECRAFT_1_15:
      case MINECRAFT_1_15_1:
      case MINECRAFT_1_15_2:
        return 5116;
      // 1.16-1.16.1
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
        return 5132;
      // 1.16.2-1.16.4
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 5136;
      // 1.17-1.18.2
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 5333;
      // 1.19-1.19.1
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
        return 5719;
      // 1.19.3
      case MINECRAFT_1_19_3:
        return 7159;
      // 1.19.4
      case MINECRAFT_1_19_4:
        return 7385;
      // 1.20-1.21
      case MINECRAFT_1_20:
      case MINECRAFT_1_20_2:
      case MINECRAFT_1_20_3:
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 7389;
      // 1.7.2-1.12.2
      default:
        return 116;
    }
  }, 0.75f),
  TRAPDOOR(protocolVersion -> {
    switch (protocolVersion) {
      // 1.7.2-1.7.6
      case MINECRAFT_1_7_2:
      case MINECRAFT_1_7_6:
        // We have to use wooden trapdoors for 1.7 since 1.7 doesn't have iron trapdoors
        return 96;
      // 1.13-1.13.1
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
        return 6509;
      // 1.13.2-1.14.3
      case MINECRAFT_1_13_2:
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
        return 6510;
      // 1.14.4-1.15.2
      case MINECRAFT_1_14_4:
      case MINECRAFT_1_15:
      case MINECRAFT_1_15_1:
      case MINECRAFT_1_15_2:
        return 7016;
      // 1.16-1.16.1
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
        return 7552;
      // 1.16.2-1.16.4
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 7556;
      // 1.17-1.18.2
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 7802;
      // 1.19-1.19.1
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
        return 8293;
      // 1.19.3
      case MINECRAFT_1_19_3:
        return 9937;
      // 1.19.4
      case MINECRAFT_1_19_4:
        return 10269;
      // 1.20
      case MINECRAFT_1_20:
        return 10273;
      // 1.20.2-1.21
      case MINECRAFT_1_20_2:
      case MINECRAFT_1_20_3:
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 10414;
      // 1.8-1.12.2
      default:
        return 167;
    }
  }, 0.1875),
  BARRIER(protocolVersion -> {
    switch (protocolVersion) {
      case MINECRAFT_1_7_2:
      case MINECRAFT_1_7_6:
        // We have to use glass for 1.7 since 1.7 doesn't have barrier blocks
        return 20;
      // 1.13-1.13.1
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
        return 6493;
      // 1.13.2
      case MINECRAFT_1_13_2:
        return 6494;
      // 1.14-1.15.2
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
      case MINECRAFT_1_14_4:
      case MINECRAFT_1_15:
      case MINECRAFT_1_15_1:
      case MINECRAFT_1_15_2:
        return 7000;
      // 1.16-1.16.1
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
        return 7536;
      // 1.16.2-1.16.4
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 7540;
      // 1.17-1.18.2
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 7754;
      // 1.19-1.19.1
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
        return 8245;
      // 1.19.3
      case MINECRAFT_1_19_3:
        return 9889;
      // 1.19.4
      case MINECRAFT_1_19_4:
        return 10221;
      // 1.20
      case MINECRAFT_1_20:
        return 10225;
      // 1.20.2-1.21
      case MINECRAFT_1_20_2:
      case MINECRAFT_1_20_3:
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 10366;
      // 1.8-1.12.2
      default:
        return 166;
    }
  }, 1);

  private final Function<ProtocolVersion, Integer> idFunction;
  @Getter
  private final double blockHeight;

  public int getId(final @NotNull ProtocolVersion protocolVersion) {
    return idFunction.apply(protocolVersion);
  }
}
