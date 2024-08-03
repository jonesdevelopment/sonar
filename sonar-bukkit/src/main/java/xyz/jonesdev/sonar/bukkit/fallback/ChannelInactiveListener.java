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
  public static final @NotNull String NAME = "sonar-inactive-listener";

  private final Set<ChannelInboundHandler> handlers = new HashSet<>();

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    final ProxyChannelHandlerContext proxy = new ProxyChannelHandlerContext(ctx);
    for (ChannelInboundHandler handler : handlers) {
      try {
        handler.channelInactive(proxy);
      } catch (Exception e) {
        handler.exceptionCaught(proxy, e);
      }
    }
    super.channelInactive(ctx);
  }

  public void add(ChannelHandler handler) {
    if (handler instanceof ChannelInboundHandler) {
      handlers.add((ChannelInboundHandler) handler);
    }
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class ProxyChannelHandlerContext implements ChannelHandlerContext {
    private final ChannelHandlerContext ctx;

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
    public ChannelHandlerContext fireChannelInactive() {
      return this;
    }

    @Override
    public ChannelHandlerContext fireExceptionCaught(Throwable throwable) {
      return null;
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
