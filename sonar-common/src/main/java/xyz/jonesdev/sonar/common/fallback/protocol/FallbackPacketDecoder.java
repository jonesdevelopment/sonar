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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_20_2;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.Direction.SERVERBOUND;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.GAME;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.LOGIN;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readVarInt;

public final class FallbackPacketDecoder extends ChannelInboundHandlerAdapter {
  private final ProtocolVersion protocolVersion;
  private FallbackPacketRegistry.ProtocolRegistry registry;
  @Setter
  private FallbackPacketListener listener;

  public FallbackPacketDecoder(final @NotNull ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
    updateRegistry(protocolVersion.compareTo(MINECRAFT_1_20_2) >= 0 ? LOGIN : GAME);
  }

  public void updateRegistry(final @NotNull FallbackPacketRegistry registry) {
    this.registry = registry.getProtocolRegistry(SERVERBOUND, protocolVersion);
  }

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx,
                          final @NotNull Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      final ByteBuf byteBuf = (ByteBuf) msg;

      try {
        // Release the ByteBuf if the connection is not active
        // or the ByteBuf doesn't contain any data to avoid
        // memory leaks or other potential exploits.
        if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
          return;
        }

        // Read the packet ID and then create the packet from it
        final int packetId = readVarInt(byteBuf);
        final FallbackPacket packet = registry.createPacket(packetId);

        // Skip the packet if it's not registered within Sonar's packet registry
        if (packet == null) {
          return;
        }

        // Ensure that the packet isn't too large or too small
        checkPacketSize(byteBuf, packet);

        try {
          // Try to decode the packet for the given protocol version
          packet.decode(byteBuf, protocolVersion);
        } catch (Throwable throwable) {
          throw new CorruptedFrameException("Failed to decode packet");
        }

        // Check if the packet still has bytes left after we decoded it
        if (byteBuf.isReadable()) {
          throw new CorruptedFrameException("Could not read packet to end");
        }

        // Let our verification handler process the packet
        if (listener != null) {
          listener.handle(packet);
        }

        // Fire channel read to avoid timeout
        ctx.fireChannelRead(packet);
      } finally {
        // Release the ByteBuf to avoid memory leaks
        byteBuf.release();
      }
    }
  }

  private void checkPacketSize(final @NotNull ByteBuf byteBuf,
                               final @NotNull FallbackPacket packet) throws Exception {
    final int expectedMaxLen = packet.expectedMaxLength(byteBuf, protocolVersion);
    if (expectedMaxLen != -1 && byteBuf.readableBytes() > expectedMaxLen) {
      throw new CorruptedFrameException("Packet too large");
    }

    final int expectedMinLen = packet.expectedMinLength(byteBuf, protocolVersion);
    if (expectedMinLen != -1 && byteBuf.readableBytes() < expectedMinLen) {
      throw new CorruptedFrameException("Packet too small");
    }
  }
}
