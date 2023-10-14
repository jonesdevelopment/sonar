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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.block.ChangedBlock;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarInt;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarLong;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"changedBlocks"})
public final class UpdateSectionBlocks implements FallbackPacket {
  private int sectionX, sectionZ;
  private ChangedBlock[] changedBlocks;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.compareTo(MINECRAFT_1_16_2) < 0) {
      byteBuf.writeInt(sectionX);
      byteBuf.writeInt(sectionZ);

      if (protocolVersion.compareTo(MINECRAFT_1_8) < 0) {
        byteBuf.writeShort(changedBlocks.length);
        byteBuf.writeInt(4 * changedBlocks.length);
      } else {
        writeVarInt(byteBuf, changedBlocks.length);
      }

      for (final ChangedBlock block : changedBlocks) {
        byteBuf.writeShort(block.getLegacyChunkPosCrammed());
        final int id = block.getType().getId(protocolVersion);
        if (protocolVersion.compareTo(MINECRAFT_1_13) >= 0) {
          writeVarInt(byteBuf, id);
        } else {
          final int shiftedBlockId = id << 4;
          if (protocolVersion.compareTo(MINECRAFT_1_8) < 0) {
            byteBuf.writeShort(shiftedBlockId);
          } else {
            writeVarInt(byteBuf, shiftedBlockId);
          }
        }
      }
    } else {
      final ChangedBlock lastChangedBlock = changedBlocks[changedBlocks.length - 1];
      final int sectionY = lastChangedBlock.getPosition().getY() >> 4;

      // Why is Mojang doing this? :(
      byteBuf.writeLong(
        ((long) (sectionX & 0x3FFFFF) << 42)
          | (sectionY & 0xFFFFF)
          | ((long) (sectionZ & 0x3FFFFF) << 20)
      );

      // 1.20+ don't have light update suppression
      if (protocolVersion.compareTo(MINECRAFT_1_20) < 0) {
        byteBuf.writeBoolean(true); // suppress light updates
      }

      writeVarInt(byteBuf, changedBlocks.length);

      for (final ChangedBlock block : changedBlocks) {
        final int shiftedBlockId = block.getType().getId(protocolVersion) << 12;
        final long positionIdValue = shiftedBlockId | block.getModernChunkPosCrammed();
        writeVarLong(byteBuf, positionIdValue);
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}