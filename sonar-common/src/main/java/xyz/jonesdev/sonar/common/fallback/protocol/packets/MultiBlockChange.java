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

package xyz.jonesdev.sonar.common.fallback.protocol.packets;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.block.ChangedBlock;

import static xyz.jonesdev.sonar.common.fallback.protocol.ProtocolVersion.MINECRAFT_1_16_2;
import static xyz.jonesdev.sonar.common.protocol.VarIntUtil.writeVarInt;
import static xyz.jonesdev.sonar.common.protocol.VarIntUtil.writeVarLong;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MultiBlockChange implements FallbackPacket {
  private int chunkX;
  private int chunkZ;
  private ChangedBlock[] changedBlocks;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(MINECRAFT_1_16_2) < 0) {
      byteBuf.writeInt(chunkX);
      byteBuf.writeInt(chunkZ);
      writeVarInt(byteBuf, changedBlocks.length);

      for (final ChangedBlock block : changedBlocks) {
        byteBuf.writeShort(block.getPosition().getX()
          - (block.getPosition().getChunkX() << 4) << 12 | block.getPosition().getZ()
          - (block.getPosition().getChunkZ() << 4) << 8 | block.getPosition().getY());
        writeVarInt(byteBuf, block.getType().getId(protocolVersion));
      }
    } else {
      int blockY;
      int chunkY = 0;

      for (final ChangedBlock block : changedBlocks) {
        blockY = block.getPosition().getY();
        chunkY = blockY >> 4;
      }

      final long chunkPosition = ((long) chunkX & 0x3FFFFFL) << 42 | ((long) chunkZ & 0x3FFFFFL) << 20 | (long) chunkY & 0xFFFFFL;
      byteBuf.writeLong(chunkPosition);
      byteBuf.writeBoolean(true); // suppress light updates

      writeVarInt(byteBuf, changedBlocks.length);

      for (final ChangedBlock block : changedBlocks) {
        final int chunkPosCrammed = block.getPosition().getX()
          - (block.getPosition().getChunkX() << 4) << 8 | block.getPosition().getZ()
          - (block.getPosition().getChunkZ() << 4) << 4 | block.getPosition().getY()
          - (chunkY << 4);
        writeVarLong(byteBuf, (long) block.getType().getId(protocolVersion) << 12 | (long) chunkPosCrammed);
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
