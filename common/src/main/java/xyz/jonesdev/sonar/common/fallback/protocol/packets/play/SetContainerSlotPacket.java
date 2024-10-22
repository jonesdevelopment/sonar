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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.captcha.ItemType;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class SetContainerSlotPacket implements FallbackPacket {
  private int windowId, slot, count;
  private ItemType itemType;
  private CompoundBinaryTag compoundBinaryTag;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
      ProtocolUtil.writeVarInt(byteBuf, windowId);
    } else {
      byteBuf.writeByte(windowId);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_17_1)) {
      ProtocolUtil.writeVarInt(byteBuf, 0);
    }

    byteBuf.writeShort(slot);

    if (protocolVersion.inBetween(ProtocolVersion.MINECRAFT_1_13_2, ProtocolVersion.MINECRAFT_1_20_3)) {
      byteBuf.writeBoolean(true);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20_5)) {
      ProtocolUtil.writeVarInt(byteBuf, count);
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
      byteBuf.writeShort(itemType.getId(protocolVersion));
    } else {
      ProtocolUtil.writeVarInt(byteBuf, itemType.getId(protocolVersion));
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      byteBuf.writeByte(count);
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_13)) {
      byteBuf.writeShort(0); // data
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_17)) {
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
        byteBuf.writeShort(-1);
      } else {
        byteBuf.writeByte(0);
      }
    } else if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      ProtocolUtil.writeBinaryTag(byteBuf, protocolVersion, compoundBinaryTag);
    } else { // 1.20.5+
      // TODO: find a way to improve this
      // component
      ProtocolUtil.writeVarInt(byteBuf, 1); // component count to add
      ProtocolUtil.writeVarInt(byteBuf, 0); // component count to remove
      // single VarInt component
      ProtocolUtil.writeVarInt(byteBuf, protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_21_2) ? 26 : 36); // type
      ProtocolUtil.writeVarInt(byteBuf, 0); // data
    }
  }

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
