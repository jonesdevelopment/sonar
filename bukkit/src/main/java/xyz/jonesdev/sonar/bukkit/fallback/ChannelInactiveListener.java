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

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

public final class ChannelInactiveListener extends ChannelInboundHandlerAdapter {
  private final Set<ChannelInboundHandler> handlers = new HashSet<>();

  @Override
  public void channelInactive(final @NotNull ChannelHandlerContext ctx) throws Exception {
    final ProxiedChannelHandlerContext proxy = new ProxiedChannelHandlerContext(ctx);
    for (final ChannelInboundHandler handler : handlers) {
      try {
        handler.channelInactive(proxy);
      } catch (Exception exception) {
        handler.exceptionCaught(proxy, exception);
      }
    }
    super.channelInactive(ctx);
  }

  public void add(final @NotNull ChannelHandler channelHandler) {
    if (channelHandler instanceof ChannelInboundHandler) {
      handlers.add((ChannelInboundHandler) channelHandler);
    }
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class ProxiedChannelHandlerContext implements ChannelHandlerContext {
    private final ChannelHandlerContext ctx;

    // Overwrite to prevent call this method on next handler

    @Override
    public ChannelHandlerContext fireChannelInactive() {
      return this;
    }

    @Override
    public ChannelHandlerContext fireExceptionCaught(Throwable throwable) {
      return this;
    }

    @Override
    public Channel channel() {
      return ctx.channel();
    }

    @Override
    public EventExecutor executor() {
      return ctx.executor();
    }

    @Override
    public String name() {
      return ctx.name();
    }

    @Override
    public ChannelHandler handler() {
      return ctx.handler();
    }

    @Override
    public boolean isRemoved() {
      return ctx.isRemoved();
    }

    @Override
    public ChannelHandlerContext fireChannelRegistered() {
      return ctx.fireChannelRegistered();
    }

    @Override
    public ChannelHandlerContext fireChannelUnregistered() {
      return ctx.fireChannelUnregistered();
    }

    @Override
    public ChannelHandlerContext fireChannelActive() {
      return ctx.fireChannelActive();
    }

    @Override
    public ChannelHandlerContext fireUserEventTriggered(Object o) {
      return ctx.fireUserEventTriggered(o);
    }

    @Override
    public ChannelHandlerContext fireChannelRead(Object o) {
      return ctx.fireChannelRead(o);
    }

    @Override
    public ChannelHandlerContext fireChannelReadComplete() {
      return ctx.fireChannelReadComplete();
    }

    @Override
    public ChannelHandlerContext fireChannelWritabilityChanged() {
      return ctx.fireChannelWritabilityChanged();
    }

    @Override
    public ChannelFuture bind(SocketAddress socketAddress) {
      return ctx.bind(socketAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress) {
      return ctx.connect(socketAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1) {
      return ctx.connect(socketAddress, socketAddress1);
    }

    @Override
    public ChannelFuture disconnect() {
      return ctx.disconnect();
    }

    @Override
    public ChannelFuture close() {
      return ctx.close();
    }

    @Override
    public ChannelFuture deregister() {
      return ctx.deregister();
    }

    @Override
    public ChannelFuture bind(SocketAddress socketAddress, ChannelPromise channelPromise) {
      return ctx.bind(socketAddress, channelPromise);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, ChannelPromise channelPromise) {
      return ctx.connect(socketAddress, channelPromise);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) {
      return ctx.connect(socketAddress, socketAddress1, channelPromise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise channelPromise) {
      return ctx.disconnect(channelPromise);
    }

    @Override
    public ChannelFuture close(ChannelPromise channelPromise) {
      return ctx.close(channelPromise);
    }

    @Override
    public ChannelFuture deregister(ChannelPromise channelPromise) {
      return ctx.deregister(channelPromise);
    }

    @Override
    public ChannelHandlerContext read() {
      return ctx.read();
    }

    @Override
    public ChannelFuture write(Object o) {
      return ctx.write(o);
    }

    @Override
    public ChannelFuture write(Object o, ChannelPromise channelPromise) {
      return ctx.write(o, channelPromise);
    }

    @Override
    public ChannelHandlerContext flush() {
      return ctx.flush();
    }

    @Override
    public ChannelFuture writeAndFlush(Object o, ChannelPromise channelPromise) {
      return ctx.writeAndFlush(o, channelPromise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object o) {
      return ctx.writeAndFlush(o);
    }

    @Override
    public ChannelPromise newPromise() {
      return ctx.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
      return ctx.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
      return ctx.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable throwable) {
      return ctx.newFailedFuture(throwable);
    }

    @Override
    public ChannelPromise voidPromise() {
      return ctx.voidPromise();
    }

    @Override
    public ChannelPipeline pipeline() {
      return ctx.pipeline();
    }

    @Override
    public ByteBufAllocator alloc() {
      return ctx.alloc();
    }

    @Override
    @Deprecated
    public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
      return ctx.attr(attributeKey);
    }

    @Override
    @Deprecated
    public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
      return ctx.hasAttr(attributeKey);
    }
  }
}
