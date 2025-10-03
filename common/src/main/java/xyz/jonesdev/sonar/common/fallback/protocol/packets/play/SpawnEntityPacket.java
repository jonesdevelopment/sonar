/*
 * Copyright (C) 2025 Sonar Contributors
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

      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_9)) {
        encodeVelocity(byteBuf, velocityX, velocityY, velocityZ);
      }
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

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_21_9)) {
      if (data > 0 || protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_9)) {
        byteBuf.writeShort((int) (velocityX * 8000D));
        byteBuf.writeShort((int) (velocityY * 8000D));
        byteBuf.writeShort((int) (velocityZ * 8000D));
      }
    }
  }

  private static void encodeVelocity(final ByteBuf byteBuf, final double x, final double y, final double z) {
    final double maxVal = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
    if (maxVal < 3.051944088384301E-5) {
      byteBuf.writeByte(0);
      return;
    }

    final long scale = (long) Math.ceil(maxVal);
    final boolean scaleTooLargeForBits = (scale & 3L) != scale;
    final long scaleBits = scaleTooLargeForBits ? scale & 3L | 4L : scale;
    final long encodedX = packLpVec3Component(x / scale) << 3;
    final long encodedY = packLpVec3Component(y / scale) << 18;
    final long encodedZ = packLpVec3Component(z / scale) << 33;
    final long packed = scaleBits | encodedX | encodedY | encodedZ;

    byteBuf.writeByte((byte) packed);
    byteBuf.writeByte((byte) (packed >> 8));
    byteBuf.writeInt((int) (packed >> 16));

    if (scaleTooLargeForBits) {
      ProtocolUtil.writeVarInt(byteBuf, (int) (scale >> 2));
    }
  }

  private static long packLpVec3Component(double d) {
    return Math.round((d * 0.5 + 0.5) * 32766.0);
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
