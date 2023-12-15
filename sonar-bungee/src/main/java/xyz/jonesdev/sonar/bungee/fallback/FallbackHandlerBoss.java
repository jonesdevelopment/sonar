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

package xyz.jonesdev.sonar.bungee.fallback;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.connection.UpstreamBridge;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.PacketWrapper;
import org.jetbrains.annotations.NotNull;

public final class FallbackHandlerBoss extends HandlerBoss {
  private ChannelWrapper channel;
  private PacketHandler handler;

  @Override
  public void setHandler(final @NotNull PacketHandler handler) {
    this.handler = handler;
    super.setHandler(handler);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      channel = new ChannelWrapper(ctx);
      super.channelActive(ctx);
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      channel.markClosed();
      super.channelInactive(ctx);
    }
  }

  @Override
  public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      handler.writabilityChanged(channel);
      super.channelWritabilityChanged(ctx);
    }
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof HAProxyMessage || msg instanceof PacketWrapper) {
      super.channelRead(ctx, msg);
    }
  }

  @Override
  public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final Throwable throwable) throws Exception {
    if (ctx.channel().isActive()) {
      if (handler instanceof DownstreamBridge || handler instanceof UpstreamBridge) {
        try {
          handler.exception(throwable);
        } catch (Exception exception) {
          ProxyServer.getInstance().getLogger().severe(handler + " - exception processing exception: " + exception);
        }
      }

      channel.markClosed();
      ctx.close();
    }
  }
}
