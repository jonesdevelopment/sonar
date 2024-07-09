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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.FallbackPacketDecoderAdapter;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.handshake.HandshakePacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginStartPacket;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.Direction.SERVERBOUND;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readVarInt;

final class FallbackBukkitPacketDecoder extends FallbackPacketDecoderAdapter {

  FallbackBukkitPacketDecoder() {
    super("encoder", "decoder", "packet_handler", "timeout");

    updateRegistry(FallbackPacketRegistry.HANDSHAKE, DEFAULT_PROTOCOL_VERSION);
  }

  private FallbackPacketRegistry.ProtocolRegistry registry;

  private static final ProtocolVersion DEFAULT_PROTOCOL_VERSION = ProtocolVersion.MINECRAFT_1_7_2;

  public void updateRegistry(final @NotNull FallbackPacketRegistry registry,
                             final @NotNull ProtocolVersion protocolVersion) {
    this.registry = registry.getProtocolRegistry(SERVERBOUND, protocolVersion);
  }

  @Override
  protected void decode(final @NotNull ChannelHandlerContext ctx,
                        final @NotNull Object msg,
                        final @NotNull List<Object> out) throws Exception {
    if (msg instanceof ByteBuf) {
      final ByteBuf originalByteBuf = (ByteBuf) msg;
      final ByteBuf byteBuf = ctx.alloc().buffer().writeBytes(originalByteBuf);
      final int originalReaderIndex = byteBuf.readerIndex();

      try {
        // Reset the reader index, so we can read the content
        byteBuf.readerIndex(0);

        // Don't continue if the ByteBuf isn't readable or the channel is closed
        if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
          return;
        }

        final int packetId = readVarInt(byteBuf);
        final FallbackPacket packet = registry.createPacket(packetId);

        // Skip the packet if it's not registered within Sonar's packet registry
        if (packet == null) {
          return;
        }

        try {
          // Try to decode the packet for the given protocol version
          packet.decode(byteBuf, protocolVersion == null ? DEFAULT_PROTOCOL_VERSION : protocolVersion);
        } catch (Throwable throwable) {
          throw new CorruptedFrameException("Failed to decode packet");
        }

        // Check if the packet still has bytes left after we decoded it
        if (byteBuf.isReadable()) {
          throw new CorruptedFrameException("Could not read packet to end");
        }

        // Useful resources:
        // https://wiki.vg/Protocol#Handshaking
        // https://wiki.vg/Protocol#Login
        if (packet instanceof HandshakePacket) {
          final HandshakePacket handshake = (HandshakePacket) packet;
          switch (handshake.getIntent()) {
            case 1:
              // We don't care about server pings; remove the handler
              ctx.channel().pipeline().remove(this);
              break;
            case 2:
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
            out.add(byteBuf.retain());
          }, loginStart.getUsername(), socketAddress);
          return;
        }

        byteBuf.readerIndex(originalReaderIndex);
        out.add(byteBuf.retain());
      } finally {
        byteBuf.release();
      }
    }
  }
}
