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

package xyz.jonesdev.sonar.common.fallback.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.traffic.TrafficCounter;

import static xyz.jonesdev.sonar.common.protocol.VarIntUtil.writeVarInt;

public final class FallbackPacketEncoder extends MessageToByteEncoder<FallbackPacket> {
  private final ProtocolVersion protocolVersion;
  private final FallbackPacketRegistry.ProtocolRegistry registry;

  public FallbackPacketEncoder(final ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
    this.registry = FallbackPacketRegistry.SONAR.getProtocolRegistry(
      FallbackPacketRegistry.Direction.CLIENTBOUND, protocolVersion
    );
  }

  @Override
  protected void encode(final ChannelHandlerContext ctx,
                        final FallbackPacket msg,
                        final ByteBuf out) throws Exception {
    final int packetId = registry.getPacketId(msg);
    writeVarInt(out, packetId);
    msg.encode(out, protocolVersion);
    TrafficCounter.OUTGOING.increment(out.readableBytes());
  }
}
