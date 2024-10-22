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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.entity.EntityType;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class SpawnEntityPacket implements FallbackPacket {
  private int entityId;
  private EntityType entityType;
  private double x, y, z;
  private int data;
  private double velocityX, velocityY, velocityZ;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    ProtocolUtil.writeVarInt(byteBuf, entityId);

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_9)) {
      ProtocolUtil.writeUUID(byteBuf, UUID.randomUUID());
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_14)) {
      ProtocolUtil.writeVarInt(byteBuf, entityType.getId().apply(protocolVersion));
    } else {
      byteBuf.writeByte(entityType.getId().apply(protocolVersion));
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_9)) {
      byteBuf.writeDouble(x);
      byteBuf.writeDouble(y);
      byteBuf.writeDouble(z);
    } else {
      byteBuf.writeInt((int) (x * 32D));
      byteBuf.writeInt((int) (y * 32D));
      byteBuf.writeInt((int) (z * 32D));
    }

    byteBuf.writeByte(0); // pitch or yaw
    byteBuf.writeByte(0); // yaw or pitch

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19)) {
      byteBuf.writeByte(0); // head yaw
      ProtocolUtil.writeVarInt(byteBuf, data); // data
    } else {
      byteBuf.writeInt(data); // data
    }

    if (data > 0 || protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_9)) {
      byteBuf.writeShort((int) (velocityX * 8000D));
      byteBuf.writeShort((int) (velocityY * 8000D));
      byteBuf.writeShort((int) (velocityZ * 8000D));
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
