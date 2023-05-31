/*
 * Copyright (C) 2023 jones
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

package jones.sonar.velocity.fallback.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_17;

@Data
public final class Transaction implements MinecraftPacket {
  private int windowId, actionId;
  private boolean accepted;

  @Override
  public void decode(final ByteBuf byteBuf,
                     final ProtocolUtils.Direction direction,
                     final ProtocolVersion protocolVersion) {
    if (protocolVersion.getProtocol() <= MINECRAFT_1_17.getProtocol()) {
      windowId = byteBuf.readByte();
      actionId = byteBuf.readShort();
      accepted = byteBuf.readBoolean();
    } else {
      windowId = 0;
      actionId = byteBuf.readInt();
      accepted = true;
    }
  }

  @Override
  public void encode(final ByteBuf byteBuf,
                     final ProtocolUtils.Direction direction,
                     final ProtocolVersion protocolVersion) {
    if (protocolVersion.getProtocol() <= MINECRAFT_1_17.getProtocol()) {
      byteBuf.writeByte(windowId);
      byteBuf.writeShort(actionId);
      byteBuf.writeBoolean(accepted);
    } else {
      byteBuf.writeInt(actionId);
    }
  }

  @Override
  public boolean handle(final MinecraftSessionHandler handler) {
    return false;
  }
}
