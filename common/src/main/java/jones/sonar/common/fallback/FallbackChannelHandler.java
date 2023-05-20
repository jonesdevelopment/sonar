/*
 * Copyright (C) 2023, jones
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

package jones.sonar.common.fallback;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import jones.sonar.api.Sonar;
import jones.sonar.api.fallback.Fallback;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public final class FallbackChannelHandler extends ChannelInboundHandlerAdapter {
  public static final FallbackChannelHandler INSTANCE = new FallbackChannelHandler(Sonar.get().getFallback());
  private final Fallback fallback;

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
    if (ctx.channel().isActive()) {
      ctx.close();

      // Clients throw an IOException if the connection is interrupted
      // unexpectedly - we cannot blacklist for this
      if (cause instanceof IOException) return;

      final InetAddress inetAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();

      fallback.getBlacklisted().add(inetAddress);
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);

    final InetAddress inetAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();

    fallback.getConnected().remove(inetAddress);
    fallback.getQueue().getQueuedPlayers().remove(inetAddress);
  }
}
