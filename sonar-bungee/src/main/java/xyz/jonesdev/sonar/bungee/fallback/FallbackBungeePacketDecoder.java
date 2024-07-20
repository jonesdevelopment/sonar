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

package xyz.jonesdev.sonar.bungee.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.LoginRequest;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.common.fallback.FallbackPacketDecoderAdapter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;

import static net.md_5.bungee.netty.PipelineUtils.BOSS_HANDLER;
import static net.md_5.bungee.netty.PipelineUtils.PACKET_ENCODER;

final class FallbackBungeePacketDecoder extends FallbackPacketDecoderAdapter {

  FallbackBungeePacketDecoder() {
    super(PACKET_ENCODER, BOSS_HANDLER);
  }

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
  public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {
    // Intercept any packets processed by BungeeCord
    if (msg instanceof PacketWrapper) {
      final PacketWrapper packetWrapper = (PacketWrapper) msg;
      final DefinedPacket wrappedPacket = packetWrapper.packet;
      // Don't handle any invalid packets
      if (wrappedPacket != null) {
        final Channel channel = ctx.channel();
        // Intercept any handshake packet by the client
        if (wrappedPacket instanceof Handshake) {
          final Handshake handshake = (Handshake) wrappedPacket;
          handleHandshake(channel, handshake.getHost(), handshake.getProtocolVersion());
          // We don't care about server pings; remove the handler
            if (handshake.getRequestedProtocol() == 1) {
            ctx.channel().pipeline().remove(this);
          }
        }
        // Intercept any server login packet by the client
        else if (wrappedPacket instanceof LoginRequest) {
          // Make sure to use the potentially modified, real IP
          final LoginRequest loginRequest = (LoginRequest) wrappedPacket;
          final HandlerBoss handlerBoss = channel.pipeline().get(HandlerBoss.class);
          final ChannelWrapper channelWrapper;
          try {
            channelWrapper = (ChannelWrapper) CHANNEL_WRAPPER_GETTER.invokeExact(handlerBoss);
          } catch (final Throwable throwable) {
            throw new ReflectiveOperationException(throwable);
          }
          final InetSocketAddress socketAddress = (InetSocketAddress) channelWrapper.getRemoteAddress();
          // We've done our job - deject this pipeline
          ctx.channel().pipeline().remove(this);
          // Make sure to mark this packet as released
          packetWrapper.trySingleRelease();
          // Let Sonar process the login packet
          handleLogin(channel, ctx, () -> ctx.fireChannelRead(msg), loginRequest.getData(), socketAddress);
          return;
        }
      }
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelRead(msg);
  }
}
