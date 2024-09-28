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

package xyz.jonesdev.sonar.bungee.fallback;

import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.LoginRequest;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.common.fallback.FallbackInboundHandlerAdapter;
import xyz.jonesdev.sonar.common.util.exception.ReflectiveOperationException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;

import static xyz.jonesdev.sonar.common.fallback.protocol.packets.handshake.HandshakePacket.STATUS;

final class FallbackBungeeInboundHandler extends FallbackInboundHandlerAdapter {

  private static final MethodHandle CHANNEL_WRAPPER_GETTER;

  static {
    try {
      CHANNEL_WRAPPER_GETTER = MethodHandles.privateLookupIn(HandlerBoss.class, MethodHandles.lookup())
        .findGetter(HandlerBoss.class, "channel", ChannelWrapper.class);
    } catch (Exception exception) {
      throw new ReflectiveOperationException(exception);
    }
  }

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof PacketWrapper) {
      final PacketWrapper packetWrapper = (PacketWrapper) msg;
      final DefinedPacket wrappedPacket = packetWrapper.packet;
      // Skip any unknown or invalid packets
      if (wrappedPacket != null) {
        if (wrappedPacket instanceof Handshake) {
          final Handshake handshake = (Handshake) wrappedPacket;
          // We don't care about server pings; remove the handler
          if (handshake.getRequestedProtocol() == STATUS) {
            ctx.pipeline().remove(this);
          } else {
            handleHandshake(ctx, handshake.getHost(), handshake.getProtocolVersion());
          }
        } else if (wrappedPacket instanceof LoginRequest) {
          // Make sure to use the potentially modified, real IP
          final LoginRequest loginRequest = (LoginRequest) wrappedPacket;
          final HandlerBoss handlerBoss = ctx.pipeline().get(HandlerBoss.class);
          // This might sometimes happen if the client unexpectedly disconnects
          if (handlerBoss != null) {
            // Get the real IP address of the player through the channel wrapper
            final ChannelWrapper channelWrapper;
            try {
              channelWrapper = (ChannelWrapper) CHANNEL_WRAPPER_GETTER.invokeExact(handlerBoss);
            } catch (final Throwable throwable) {
              throw new ReflectiveOperationException(throwable);
            }
            // We've done our job - deject this pipeline
            ctx.pipeline().remove(this);
            // Make sure to mark this packet as released
            packetWrapper.trySingleRelease();
            // Let Sonar process the login packet
            final InetSocketAddress socketAddress = (InetSocketAddress) channelWrapper.getRemoteAddress();
            handleLogin(ctx, () -> ctx.fireChannelRead(msg), loginRequest.getData(), socketAddress);
            return;
          }
        }
      }
    }
    ctx.fireChannelRead(msg);
  }
}
