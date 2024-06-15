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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import java.util.UUID;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeUUID;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarInt;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class SpawnEntityPacket implements FallbackPacket {
  private int entityId, entityType;
  private double x, y, z;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    writeVarInt(byteBuf, entityId);

    final boolean v1_9orHigher = protocolVersion.compareTo(MINECRAFT_1_8) > 0;

    if (v1_9orHigher) {
      writeUUID(byteBuf, UUID.randomUUID());
    }

    if (protocolVersion.compareTo(MINECRAFT_1_14) >= 0) {
      writeVarInt(byteBuf, entityType);
    } else {
      byteBuf.writeByte(entityType);
    }

    if (v1_9orHigher) {
      byteBuf.writeDouble(x);
      byteBuf.writeDouble(y);
      byteBuf.writeDouble(z);
    } else {
      byteBuf.writeInt(floor(x * 32D));
      byteBuf.writeInt(floor(y * 32D));
      byteBuf.writeInt(floor(z * 32D));
    }

    byteBuf.writeByte(0); // pitch or yaw
    byteBuf.writeByte(0); // yaw or pitch

    if (protocolVersion.compareTo(MINECRAFT_1_19) >= 0) {
      byteBuf.writeByte(0); // head yaw
      writeVarInt(byteBuf, 0); // data
    } else {
      byteBuf.writeInt(0); // data
    }

    if (v1_9orHigher) {
      byteBuf.writeShort(0); // velocity X
      byteBuf.writeShort(0); // velocity Y
      byteBuf.writeShort(0); // velocity Z
    }
  }

  private static int floor(final double value) {
    final int __value = (int) value;
    return value < (double) __value ? __value - 1 : __value;
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
