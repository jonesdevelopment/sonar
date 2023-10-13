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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_20_2;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.CONFIG;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.Direction.SERVERBOUND;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.GAME;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.readVarInt;

public final class FallbackPacketDecoder extends ChannelInboundHandlerAdapter {
  private final FallbackUser<?, ?> user;
  private final FallbackPacketListener listener;
  private @NotNull FallbackPacketRegistry.ProtocolRegistry registry;

  public FallbackPacketDecoder(final @NotNull FallbackUser<?, ?> user,
                               final @NotNull FallbackPacketListener listener) {
    this.user = user;
    this.registry = (user.getProtocolVersion().compareTo(MINECRAFT_1_20_2) >= 0 ? CONFIG : GAME)
      .getProtocolRegistry(SERVERBOUND, user.getProtocolVersion());
    this.listener = listener;
  }

  public void switchToGameState() {
    this.registry = GAME.getProtocolRegistry(SERVERBOUND, user.getProtocolVersion());
  }

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx,
                          final @NotNull Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      final ByteBuf byteBuf = (ByteBuf) msg;

      if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
        byteBuf.release();
        return;
      }

      final int originalReaderIndex = byteBuf.readerIndex();
      final int packetId = readVarInt(byteBuf);
      final FallbackPacket packet = registry.createPacket(packetId);

      if (packet == null) {
        byteBuf.readerIndex(originalReaderIndex);
        return;
      }

      try {
        doLengthSanityChecks(byteBuf, packet);

        try {
          packet.decode(byteBuf, user.getProtocolVersion());
        } catch (Throwable throwable) {
          user.fail("failed to decode packet (" + byteBuf.readableBytes() + " bytes)");
          throw new CorruptedFrameException("Failed to decode packet");
        }

        if (byteBuf.isReadable()) {
          user.fail("could not read packet to end (" + byteBuf.readableBytes() + " bytes left)");
          throw new CorruptedFrameException("Could not read packet to end");
        }

        listener.handle(packet);
      } finally {
        byteBuf.release();
      }
    } else {
      user.fail("packet is not a ByteBuf");
      throw new CorruptedFrameException("Packet is not a ByteBuf?!");
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
