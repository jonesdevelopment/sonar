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

package xyz.jonesdev.sonar.common.fallback.protocol.entity;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.function.Function;

@RequiredArgsConstructor
public enum EntityType {
  BOAT(protocolVersion -> {
    // Useful resources:
    // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
    // - https://pokechu22.github.io/Burger/1.20.4.html
    switch (protocolVersion) {
      default:
      case MINECRAFT_1_7_2:
      case MINECRAFT_1_7_6:
      case MINECRAFT_1_8:
      case MINECRAFT_1_9:
      case MINECRAFT_1_9_1:
      case MINECRAFT_1_9_2:
      case MINECRAFT_1_9_4:
      case MINECRAFT_1_10:
      case MINECRAFT_1_11:
      case MINECRAFT_1_11_1:
      case MINECRAFT_1_12:
      case MINECRAFT_1_12_1:
      case MINECRAFT_1_12_2:
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
      case MINECRAFT_1_13_2:
        return 1;
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
      case MINECRAFT_1_14_4:
        return 5;
      case MINECRAFT_1_15:
      case MINECRAFT_1_15_1:
      case MINECRAFT_1_15_2:
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 6;
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 7;
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
      case MINECRAFT_1_19_3:
        return 8;
      case MINECRAFT_1_19_4:
      case MINECRAFT_1_20:
      case MINECRAFT_1_20_2:
      case MINECRAFT_1_20_3:
        return 9;
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 10;
    }
  }),
  MINECART(protocolVersion -> {
    switch (protocolVersion) {
      // 1.13-1.13.2
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
      case MINECRAFT_1_13_2:
        return 39;
      // 1.14-1.14.4
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
      case MINECRAFT_1_14_4:
        return 41;
      // 1.16-1.16.4
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 45;
      // 1.17-1.18.2
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 50;
     // 1.19-1.19.1
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
        return 53;
      // 1.19.3
      case MINECRAFT_1_19_3:
        return 54;
       // 1.19.4-1.20.2
      case MINECRAFT_1_19_4:
      case MINECRAFT_1_20:
      case MINECRAFT_1_20_2:
        return 64;
      // 1.20.3
      case MINECRAFT_1_20_3:
        return 65;
      // 1.20.5-1.21
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 69;
      // 1.7-1.12.2 & 1.15-1.15.2
      default:
        return 42;
    }
  });

  private final Function<ProtocolVersion, Integer> function;

  public int getId(final @NotNull ProtocolVersion protocolVersion) {
    return function.apply(protocolVersion);
  }
}
