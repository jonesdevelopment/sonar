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

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;

// TODO: load mappings from a separate file
@RequiredArgsConstructor
public enum EntityType {
  // Useful resources:
  // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
  // - https://pokechu22.github.io/Burger/1.20.4.html
  BOAT(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 1;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_14_4) <= 0) {
      return 5;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 6;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 7;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 8;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20_3) <= 0) {
      return 9;
    }
    return 10;
  }),
  MINECART(protocolVersion -> {
    if (protocolVersion.compareTo(MINECRAFT_1_13_2) <= 0) {
      return 10;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_14_4) <= 0) {
      return 41;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_15_2) <= 0) {
      return 42;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_16_4) <= 0) {
      return 45;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_18_2) <= 0) {
      return 50;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_1) <= 0) {
      return 53;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_19_3) <= 0) {
      return 54;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20_2) <= 0) {
      return 64;
    }
    if (protocolVersion.compareTo(MINECRAFT_1_20_3) <= 0) {
      return 65;
    }
    return 69;
  });

  private final Function<ProtocolVersion, Integer> function;

  public int getId(final @NotNull ProtocolVersion protocolVersion) {
    return function.apply(protocolVersion);
  }
}
