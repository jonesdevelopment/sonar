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

package xyz.jonesdev.sonar.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

@RequiredArgsConstructor
public final class SonarPacketEncoder extends MessageToByteEncoder<SonarPacket> {
  private final ProtocolVersion protocolVersion;
  @Getter
  private SonarPacketRegistry packetRegistry;
  private SonarPacketRegistry.ProtocolRegistry protocolRegistry;

  public void updateRegistry(final @NotNull SonarPacketRegistry packetRegistry) {
    this.packetRegistry = packetRegistry;
    this.protocolRegistry = packetRegistry.getProtocolRegistry(SonarPacketRegistry.Direction.CLIENTBOUND, protocolVersion);
  }

  @Override
  protected void encode(final ChannelHandlerContext ctx,
                        final @NotNull SonarPacket packet,
                        final @NotNull ByteBuf out) throws Exception {
    final Class<? extends SonarPacket> originalPacket = packet instanceof SonarPacketSnapshot
      ? ((SonarPacketSnapshot) packet).getOriginalPacketClass() : packet.getClass();
    final int packetId = protocolRegistry.getPacketId(originalPacket);
    ProtocolUtil.writeVarInt(out, packetId);
    packet.encode(out, protocolVersion);
  }
}
