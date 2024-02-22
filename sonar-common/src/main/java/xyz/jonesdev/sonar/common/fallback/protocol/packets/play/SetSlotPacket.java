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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.writeCompoundTag;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.writeNamelessCompoundTag;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarInt;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class SetSlotPacket implements FallbackPacket {
  private int windowId;
  private int slot;
  private int count;
  private int data;
  private int itemId;
  private CompoundBinaryTag compoundBinaryTag;

  public static final CompoundBinaryTag MAP_NBT = CompoundBinaryTag.builder()
    .put("map", IntBinaryTag.intBinaryTag(0)) // map type
    .build();

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    byteBuf.writeByte(windowId);

    if (protocolVersion.compareTo(MINECRAFT_1_17_1) >= 0) {
      writeVarInt(byteBuf, 0);
    }

    byteBuf.writeShort(slot);

    if (protocolVersion.compareTo(MINECRAFT_1_13_2) >= 0) {
      byteBuf.writeBoolean(true);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_13_2) < 0) {
      byteBuf.writeShort(itemId);
    } else {
      writeVarInt(byteBuf, itemId);
    }
    byteBuf.writeByte(count);
    if (protocolVersion.compareTo(MINECRAFT_1_13) < 0) {
      byteBuf.writeShort(data);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_17) < 0) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
        byteBuf.writeShort(-1);
      } else {
        byteBuf.writeByte(0);
      }
    } else {
      if (protocolVersion.compareTo(MINECRAFT_1_20_2) >= 0) {
        writeNamelessCompoundTag(byteBuf, compoundBinaryTag);
      } else {
        writeCompoundTag(byteBuf, compoundBinaryTag);
      }
    }
  }
}
