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

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_17;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class TransactionPacket implements FallbackPacket {
  private int windowId, transactionId;
  private boolean accepted;

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.compareTo(MINECRAFT_1_17) < 0) {
      windowId = byteBuf.readByte();
      transactionId = byteBuf.readShort();
      accepted = byteBuf.readBoolean();
    } else {
      transactionId = byteBuf.readInt();
      // Always set accepted to true since 1.17 or higher don't use
      // transactions for inventory confirmation anymore.
      accepted = true;
    }
  }

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.compareTo(MINECRAFT_1_17) < 0) {
      byteBuf.writeByte(windowId);
      byteBuf.writeShort((short) transactionId);
      byteBuf.writeBoolean(accepted);
    } else {
      byteBuf.writeInt(transactionId);
    }
  }
}
