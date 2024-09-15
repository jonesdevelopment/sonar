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

package xyz.jonesdev.sonar.common.fallback.protocol.captcha;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.function.Function;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;

// TODO: load mappings from a separate file
@RequiredArgsConstructor
public enum ItemType {
  // Useful resources:
  // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
  // - https://pokechu22.github.io/Burger/1.20.4.html
  // - https://github.com/ViaVersion/Mappings/tree/main/mappings
  FILLED_MAP(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_12_2) <= 0) {
      return 358;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_1) <= 0) {
      return 608;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 613;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 671;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 733;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 847;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 886;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 914;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_4) <= 0) {
      return 937;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20_2) <= 0) {
      return 941;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20_3) <= 0) {
      return 979;
    }
    return 982;
  });

  private final Function<ProtocolVersion, Integer> idFunction;

  public int getId(final @NotNull ProtocolVersion protocolVersion) {
    return idFunction.apply(protocolVersion);
  }
}
