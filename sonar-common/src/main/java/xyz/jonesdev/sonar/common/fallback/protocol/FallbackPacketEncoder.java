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

package xyz.jonesdev.sonar.common.fallback.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.Direction.CLIENTBOUND;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.LOGIN;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarInt;

public final class FallbackPacketEncoder extends MessageToByteEncoder<FallbackPacket> {
  private final ProtocolVersion protocolVersion;
  @Getter
  private FallbackPacketRegistry packetRegistry;
  private FallbackPacketRegistry.ProtocolRegistry protocolRegistry;

  public FallbackPacketEncoder(final ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
    updateRegistry(LOGIN);
  }

  public void updateRegistry(final @NotNull FallbackPacketRegistry packetRegistry) {
    this.packetRegistry = packetRegistry;
    this.protocolRegistry = packetRegistry.getProtocolRegistry(CLIENTBOUND, protocolVersion);
  }

  @Override
  protected void encode(final ChannelHandlerContext ctx,
                        final @NotNull FallbackPacket packet,
                        final ByteBuf out) throws Exception {
    final FallbackPacket originalPacket = packet instanceof FallbackPacketSnapshot
      ? ((FallbackPacketSnapshot) packet).getOriginalPacket() : packet;
    final int packetId = protocolRegistry.getPacketId(originalPacket);
    writeVarInt(out, packetId);
    packet.encode(out, protocolVersion);
  }
}
