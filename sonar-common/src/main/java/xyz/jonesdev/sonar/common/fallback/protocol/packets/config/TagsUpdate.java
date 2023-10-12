/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.config;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import java.util.HashMap;
import java.util.Map;

import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.readString;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.readVarIntArray;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.readVarInt;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class TagsUpdate implements FallbackPacket {
  private Map<String, Map<String, int[]>> tags;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    final Map<String, Map<String, int[]>> map = new HashMap<>();
    final int size = readVarInt(byteBuf);

    for (int i = 0; i < size; i++) {
      final String key = readString(byteBuf);
      final int innerSize = readVarInt(byteBuf);
      final Map<String, int[]> innerMap = new HashMap<>();

      for (int j = 0; j < innerSize; j++) {
        final String innerKey = readString(byteBuf);
        final int[] innerValue = readVarIntArray(byteBuf);
        innerMap.put(innerKey, innerValue);
      }
      map.put(key, innerMap);
    }
    tags = map;
  }

  @Override
  public int expectedMaxLength(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    return 0;
  }
}
