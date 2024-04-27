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
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_20_2;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.Direction.SERVERBOUND;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.GAME;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.LOGIN;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.readVarInt;

public final class FallbackPacketDecoder extends ChannelInboundHandlerAdapter {
  private final FallbackUser user;
  private final FallbackPacketListener listener;
  private FallbackPacketRegistry.ProtocolRegistry registry;

  public FallbackPacketDecoder(final @NotNull FallbackUser user,
                               final @NotNull FallbackPacketListener listener) {
    this.user = user;
    this.listener = listener;
    updateRegistry(user.getProtocolVersion().compareTo(MINECRAFT_1_20_2) >= 0 ? LOGIN : GAME);
  }

  public void updateRegistry(final @NotNull FallbackPacketRegistry registry) {
    this.registry = registry.getProtocolRegistry(SERVERBOUND, user.getProtocolVersion());
  }

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx,
                          final @NotNull Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      final ByteBuf byteBuf = (ByteBuf) msg;

      // Release the ByteBuf if the connection is not active
      // or the ByteBuf doesn't contain any data to avoid
      // memory leaks or other potential exploits.
      if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
        byteBuf.release();
        return;
      }

      final int originalReaderIndex = byteBuf.readerIndex();
      // Read the packet ID and then create the packet from it
      final int packetId = readVarInt(byteBuf);
      final FallbackPacket packet = registry.createPacket(packetId);

      // If the packet isn't found, skip it
      if (packet == null) {
        byteBuf.readerIndex(originalReaderIndex);
        return;
      }


      try {
        // Ensure that the packet isn't too large or too small
        doLengthSanityChecks(byteBuf, packet);

        try {
          // Try to decode the packet for the given protocol version
          packet.decode(byteBuf, user.getProtocolVersion());
        } catch (Throwable throwable) {
          user.fail("failed to decode packet (" + byteBuf.readableBytes() + " bytes)");
          throw new CorruptedFrameException("Failed to decode packet");
        }

        // Check if the packet still has bytes left after we decoded it
        if (byteBuf.isReadable()) {
          user.fail("could not read packet to end (" + byteBuf.readableBytes() + " bytes left)");
          throw new CorruptedFrameException("Could not read packet to end");
        }

        // Let our verification handler process the packet
        listener.handle(packet);

        // Fire channel read to avoid timeout
        ctx.fireChannelRead(packet);
      } finally {
        // Release the ByteBuf to avoid memory leaks
        byteBuf.release();
      }
    }
  }

  private void doLengthSanityChecks(final ByteBuf byteBuf,
                                    final @NotNull FallbackPacket packet) throws Exception {
    final int expectedMaxLen = packet.expectedMaxLength(byteBuf, user.getProtocolVersion());
    if (expectedMaxLen != -1 && byteBuf.readableBytes() > expectedMaxLen) {
      user.fail("packet too large (" + byteBuf.readableBytes() + " bytes)");
      throw new CorruptedFrameException("Packet too large");
    }

    final int expectedMinLen = packet.expectedMinLength(byteBuf, user.getProtocolVersion());
    if (byteBuf.readableBytes() < expectedMinLen) {
      user.fail("packet too small (" + byteBuf.readableBytes() + " bytes)");
      throw new CorruptedFrameException("Packet too small");
    }
  }
}
