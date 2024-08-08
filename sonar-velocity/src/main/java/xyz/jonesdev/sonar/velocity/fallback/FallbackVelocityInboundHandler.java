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

package xyz.jonesdev.sonar.velocity.fallback;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.common.fallback.FallbackInboundHandlerAdapter;

import java.net.InetSocketAddress;

import static com.velocitypowered.proxy.network.Connections.HANDLER;

final class FallbackVelocityInboundHandler extends FallbackInboundHandlerAdapter {

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {
    // Intercept any handshake packet by the client
    if (msg instanceof HandshakePacket handshake) {
      handleHandshake(ctx.channel(), handshake.getServerAddress(), handshake.getProtocolVersion().getProtocol());
      // We don't care about server pings; remove the handler
      if (handshake.getNextStatus() == 1) {
        ctx.channel().pipeline().remove(this);
      }
    }
    // Intercept any server login packet by the client
    else if (msg instanceof ServerLoginPacket serverLogin) {
      // Make sure to use the potentially modified, original IP
      final MinecraftConnection minecraftConnection = (MinecraftConnection) ctx.channel().pipeline().get(HANDLER);
      final InetSocketAddress socketAddress = (InetSocketAddress) minecraftConnection.getRemoteAddress();
      // We've done our job - deject this pipeline
      ctx.channel().pipeline().remove(this);
      // Let Sonar process the login packet
      handleLogin(ctx.channel(), ctx, () -> ctx.fireChannelRead(msg), serverLogin.getUsername(), socketAddress);
      return;
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelRead(msg);
  }
}
