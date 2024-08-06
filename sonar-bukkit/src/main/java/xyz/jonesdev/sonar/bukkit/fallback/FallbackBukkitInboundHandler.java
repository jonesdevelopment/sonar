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

package xyz.jonesdev.sonar.bukkit.fallback;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.ChannelInactiveListener;
import xyz.jonesdev.sonar.api.fallback.FallbackPipelines;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.FallbackInboundHandlerAdapter;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.handshake.HandshakePacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginStartPacket;

import java.net.InetSocketAddress;
import java.util.Objects;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.Direction.SERVERBOUND;
import static xyz.jonesdev.sonar.common.fallback.protocol.packets.handshake.HandshakePacket.*;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readVarInt;

final class FallbackBukkitInboundHandler extends FallbackInboundHandlerAdapter {

  FallbackBukkitInboundHandler() {
    updateRegistry(FallbackPacketRegistry.HANDSHAKE, DEFAULT_PROTOCOL_VERSION);
    channelRemovalListener = ((pipeline, name, handler) -> {
      final ChannelInactiveListener inactiveListener = (ChannelInactiveListener) pipeline.get(FallbackPipelines.FALLBACK_INACTIVE_LISTENER);
      if (inactiveListener != null) {
        inactiveListener.add(handler);
      }
    });
  }

  private FallbackPacketRegistry.ProtocolRegistry registry;

  private static final ProtocolVersion DEFAULT_PROTOCOL_VERSION = ProtocolVersion.MINECRAFT_1_7_2;

  public void updateRegistry(final @NotNull FallbackPacketRegistry registry,
                             final @NotNull ProtocolVersion protocolVersion) {
    this.registry = registry.getProtocolRegistry(SERVERBOUND, protocolVersion);
  }

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      final ByteBuf byteBuf = (ByteBuf) msg;

      // Reset the reader index, so we can read the content
      // Make sure to store the original reader index
      final int originalReaderIndex = byteBuf.readerIndex();
      byteBuf.readerIndex(0);

      // Don't continue if the ByteBuf isn't readable or the channel is closed
      if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
        byteBuf.release();
        return;
      }

      final int packetId = readVarInt(byteBuf);
      final FallbackPacket packet = registry.createPacket(packetId);

      // Skip the packet if it's not registered within Sonar's packet registry
      if (packet == null) {
        // Release the ByteBuf to avoid memory leaks
        byteBuf.release();
        return;
      }

      try {
        // Try to decode the packet for the given protocol version
        packet.decode(byteBuf, protocolVersion == null ? DEFAULT_PROTOCOL_VERSION : protocolVersion);
      } catch (Throwable throwable) {
        // Release the ByteBuf to avoid memory leaks
        byteBuf.release();
        throw new CorruptedFrameException("Failed to decode packet");
      }

      // Check if the packet still has bytes left after we decoded it
      if (byteBuf.isReadable()) {
        // Release the ByteBuf to avoid memory leaks
        byteBuf.release();
        throw new CorruptedFrameException("Could not read packet to end");
      }

      // Useful resources:
      // https://wiki.vg/Protocol#Handshaking
      // https://wiki.vg/Protocol#Login
      if (packet instanceof HandshakePacket) {
        final HandshakePacket handshake = (HandshakePacket) packet;
        switch (handshake.getIntent()) {
          case STATUS:
            // We don't care about server pings or transfers; remove the handler
            ctx.channel().pipeline().remove(this);
            break;
          case LOGIN:
          case TRANSFER:
            // Let the actual handler know about the handshake packet
            handleHandshake(ctx.channel(), handshake.getHostname(), handshake.getProtocolVersionId());
            // Be ready for the next packet (which is supposed to be a login packet)
            updateRegistry(FallbackPacketRegistry.LOGIN, Objects.requireNonNull(protocolVersion));
            break;
          default:
            throw new CorruptedFrameException("Unknown handshake intent " + handshake.getIntent());
        }
      } else if (packet instanceof LoginStartPacket) {
        final LoginStartPacket loginStart = (LoginStartPacket) packet;
        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        // We've done our job - deject this pipeline
        ctx.channel().pipeline().remove(this);
        // Let Sonar process the login packet
        handleLogin(ctx.channel(), ctx, () -> {
          byteBuf.readerIndex(originalReaderIndex);
          ctx.fireChannelRead(byteBuf);
          final ChannelHandler fallbackInboundHandler = ctx.channel().pipeline().remove(FallbackPipelines.FALLBACK_INBOUND_HANDLER);
          if (fallbackInboundHandler != null) {
            channelRemovalListener.accept(ctx.pipeline(), FallbackPipelines.FALLBACK_INBOUND_HANDLER, fallbackInboundHandler);
          }
        }, loginStart.getUsername(), socketAddress);
        return;
      }

      byteBuf.readerIndex(originalReaderIndex);
      ctx.fireChannelRead(byteBuf);
    }
  }
}
