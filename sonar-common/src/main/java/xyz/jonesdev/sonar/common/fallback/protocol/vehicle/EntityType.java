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

package xyz.jonesdev.sonar.common.fallback.protocol.vehicle;

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
        // 1.7-1.12.2
        return 358;
      case MINECRAFT_1_20_3:
        return 979;
    }
  });

  private final Function<ProtocolVersion, Integer> function;

  public int getId(final @NotNull ProtocolVersion protocolVersion) {
    return function.apply(protocolVersion);
  }
}
