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
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockUpdate;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarInt;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarLong;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"blockUpdates"})
public final class UpdateSectionBlocksPacket implements FallbackPacket {
  private int sectionX, sectionZ;
  private BlockUpdate[] blockUpdates;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.compareTo(MINECRAFT_1_16_2) < 0) {
      byteBuf.writeInt(sectionX);
      byteBuf.writeInt(sectionZ);

      if (protocolVersion.compareTo(MINECRAFT_1_8) < 0) {
        byteBuf.writeShort(blockUpdates.length);
        byteBuf.writeInt(4 * blockUpdates.length);
      } else {
        writeVarInt(byteBuf, blockUpdates.length);
      }

      for (final BlockUpdate block : blockUpdates) {
        byteBuf.writeShort(block.getLegacyBlockState());
        final int blockId = block.getType().getId(protocolVersion);
        if (protocolVersion.compareTo(MINECRAFT_1_13) >= 0) {
          writeVarInt(byteBuf, blockId);
        } else {
          final int shiftedBlockId = blockId << 4;
          if (protocolVersion.compareTo(MINECRAFT_1_8) < 0) {
            byteBuf.writeShort(shiftedBlockId);
          } else {
            writeVarInt(byteBuf, shiftedBlockId);
          }
        }
      }
    } else {
      // We only need one Y position
      final int sectionY = blockUpdates[0].getPosition().getY() >> 4;

      // https://wiki.vg/Protocol#Update_Section_Blocks
      byteBuf.writeLong(((sectionX & 0x3FFFFFL) << 42) | (sectionY & 0xFFFFF) | ((sectionZ & 0x3FFFFFL) << 20));

      // 1.20+ don't have light update suppression
      if (protocolVersion.compareTo(MINECRAFT_1_20) < 0) {
        byteBuf.writeBoolean(true); // suppress light updates
      }

      writeVarInt(byteBuf, blockUpdates.length);

      for (final BlockUpdate block : blockUpdates) {
        // https://wiki.vg/Protocol#Update_Section_Blocks
        final int shiftedBlockId = block.getType().getId(protocolVersion) << 12;
        final long positionIdValue = shiftedBlockId | block.getBlockState();
        writeVarLong(byteBuf, positionIdValue);
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
