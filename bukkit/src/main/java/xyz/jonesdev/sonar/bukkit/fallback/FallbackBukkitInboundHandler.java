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

package xyz.jonesdev.sonar.bukkit.fallback;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.FallbackInboundHandlerAdapter;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.handshake.HandshakePacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginStartPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import java.net.InetSocketAddress;
import java.util.Objects;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_INBOUND_HANDLER;
import static xyz.jonesdev.sonar.common.fallback.protocol.packets.handshake.HandshakePacket.*;

final class FallbackBukkitInboundHandler extends FallbackInboundHandlerAdapter {
  private static final ProtocolVersion DEFAULT_PROTOCOL_VERSION = ProtocolVersion.MINECRAFT_1_7_2;

  private FallbackPacketRegistry.ProtocolRegistry registry;

  FallbackBukkitInboundHandler() {
    updateRegistry(FallbackPacketRegistry.HANDSHAKE, DEFAULT_PROTOCOL_VERSION);

    channelRemovalListener = (pipeline, name, handler) -> {
      final ChannelInactiveListener inactiveListener = pipeline.get(ChannelInactiveListener.class);

      if (inactiveListener != null) {
        inactiveListener.add(handler);
      }
    };
  }

  public void updateRegistry(final @NotNull FallbackPacketRegistry registry,
                             final @NotNull ProtocolVersion protocolVersion) {
    this.registry = registry.getProtocolRegistry(FallbackPacketRegistry.Direction.SERVERBOUND, protocolVersion);
  }

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      final ByteBuf byteBuf = (ByteBuf) msg;

      /*
       * Reset the reader index, so we can read the content;
       * We also have to make sure to store the original reader index.
       * We do this to 'spoof' the packet state - we want to read it without the server knowing.
       */
      final int originalReaderIndex = byteBuf.readerIndex();
      byteBuf.readerIndex(0);

      // Don't continue if the ByteBuf isn't readable or the channel is closed
      if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
        byteBuf.release();
        return;
      }

      final int packetId = ProtocolUtil.readVarInt(byteBuf);
      final FallbackPacket packet = registry.createPacket(packetId);

      // Skip the packet if it's not registered within Sonar's packet registry
      if (packet == null) {
        byteBuf.release();
        return;
      }

      try {
        // Try to decode the packet for the given protocol version
        packet.decode(byteBuf, protocolVersion == null ? DEFAULT_PROTOCOL_VERSION : protocolVersion);
      } catch (Throwable throwable) {
        byteBuf.release();
        throw ProtocolUtil.DEBUG ? new DecoderException(throwable) : QuietDecoderException.INSTANCE;
      }

      // Check if the packet still has bytes left after we decoded it
      if (byteBuf.isReadable()) {
        byteBuf.release();
        throw ProtocolUtil.DEBUG ? new DecoderException("Could not read packet to end ("
          + byteBuf.readableBytes() + " bytes left)") : QuietDecoderException.INSTANCE;
      }

      /*
       * Useful resources:
       * https://wiki.vg/Protocol#Handshaking
       * https://wiki.vg/Protocol#Login
       */
      if (packet instanceof HandshakePacket) {
        final HandshakePacket handshake = (HandshakePacket) packet;
        switch (handshake.getIntent()) {
          case STATUS:
            // We don't care about server pings or transfers; remove the handler
            ctx.pipeline().remove(this);
            break;
          case LOGIN:
          case TRANSFER:
            // Let the actual handler know about the handshake packet
            handleHandshake(ctx, handshake.getHostname(), handshake.getProtocolVersionId());
            // Be ready for the next packet (which is supposed to be a login packet)
            updateRegistry(FallbackPacketRegistry.LOGIN, Objects.requireNonNull(protocolVersion));
            break;
          default:
            throw ProtocolUtil.DEBUG ? new DecoderException("Bad handshake intent " + handshake.getIntent())
              : QuietDecoderException.INSTANCE;
        }
      } else if (packet instanceof LoginStartPacket) {
        final LoginStartPacket loginStart = (LoginStartPacket) packet;
        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        // We've done our job - deject this pipeline
        ctx.pipeline().remove(this);
        // Let Sonar process the login packet
        handleLogin(ctx, () -> {
          byteBuf.readerIndex(originalReaderIndex);
          ctx.fireChannelRead(byteBuf.retain());
          // TODO: recode this?
          final ChannelHandler inboundHandler = ctx.pipeline().remove(FALLBACK_INBOUND_HANDLER);
          if (inboundHandler != null) {
            channelRemovalListener.accept(ctx.pipeline(), FALLBACK_INBOUND_HANDLER, inboundHandler);
          }
        }, loginStart.getUsername(), socketAddress);
        byteBuf.release();
        return;
      }

      byteBuf.readerIndex(originalReaderIndex);
      ctx.fireChannelRead(byteBuf);
    }
  }
}
