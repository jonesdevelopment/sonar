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
import xyz.jonesdev.sonar.common.fallback.FallbackChannelHandlerAdapter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;

import static net.md_5.bungee.netty.PipelineUtils.*;

public final class FallbackBungeeChannelHandler extends FallbackChannelHandlerAdapter {

  public FallbackBungeeChannelHandler(final @NotNull Channel channel) {
    super(channel);
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
  public void channelRead(final @NotNull ChannelHandlerContext ctx, final Object msg) throws Exception {
    // TODO: put this into a separate handler
    // Intercept any packets processed by BungeeCord
    if (listenForPackets && msg instanceof PacketWrapper) {
      final PacketWrapper packetWrapper = (PacketWrapper) msg;
      final DefinedPacket wrappedPacket = packetWrapper.packet;
      // Don't handle any invalid packets
      if (wrappedPacket != null) {
        // Intercept any handshake packet by the client
        if (wrappedPacket instanceof Handshake) {
          final Handshake handshake = (Handshake) wrappedPacket;
          handleHandshake(handshake.getHost(), handshake.getProtocolVersion());
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
          handleLogin(ctx, msg, loginRequest.getData(), socketAddress,
            PACKET_ENCODER, PACKET_DECODER, TIMEOUT_HANDLER, BOSS_HANDLER);
          packetWrapper.trySingleRelease();
          return;
        }
      }
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelRead(msg);
  }
}
