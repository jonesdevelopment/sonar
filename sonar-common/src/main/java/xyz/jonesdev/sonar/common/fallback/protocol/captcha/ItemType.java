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

package xyz.jonesdev.sonar.common.fallback.protocol.captcha;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.function.Function;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public enum ItemType {
  FILLED_MAP(protocolVersion -> {
    // Useful resources:
    // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
    // - https://pokechu22.github.io/Burger/1.20.4.html
    // - https://github.com/ViaVersion/Mappings/tree/main/mappings
    switch (protocolVersion) {
      default:
        // 1.7-1.12.2
        return 358;
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
        return 608;
      case MINECRAFT_1_13_2:
        return 613;
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
      case MINECRAFT_1_14_4:
      case MINECRAFT_1_15:
      case MINECRAFT_1_15_1:
      case MINECRAFT_1_15_2:
        return 671;
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 733;
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 847;
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
        return 886;
      case MINECRAFT_1_19_3:
        return 914;
      case MINECRAFT_1_19_4:
        return 937;
      case MINECRAFT_1_20:
      case MINECRAFT_1_20_2:
        return 941;
      case MINECRAFT_1_20_3:
        return 979;
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 982;
    }
  });

  private final Function<ProtocolVersion, Integer> function;

  public int getId(final @NotNull ProtocolVersion protocolVersion) {
    return function.apply(protocolVersion);
  }
}
