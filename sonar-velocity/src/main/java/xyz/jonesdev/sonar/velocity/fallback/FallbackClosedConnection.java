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
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

public final class FallbackClosedConnection extends MinecraftConnection {
  public FallbackClosedConnection() {
    super(new Channel() {
      @Override
      public ChannelId id() {
        return null;
      }

      @Override
      public EventLoop eventLoop() {
        return null;
      }

      @Override
      public Channel parent() {
        return null;
      }

      @Override
      public ChannelConfig config() {
        return null;
      }

      @Override
      public boolean isOpen() {
        return false;
      }

      @Override
      public boolean isRegistered() {
        return false;
      }

      @Override
      public boolean isActive() {
        return false;
      }

      @Override
      public ChannelMetadata metadata() {
        return null;
      }

      @Override
      public SocketAddress localAddress() {
        return null;
      }

      @Override
      public SocketAddress remoteAddress() {
        return null;
      }

      @Override
      public ChannelFuture closeFuture() {
        return null;
      }

      @Override
      public boolean isWritable() {
        return false;
      }

      @Override
      public long bytesBeforeUnwritable() {
        return 0;
      }

      @Override
      public long bytesBeforeWritable() {
        return 0;
      }

      @Override
      public Unsafe unsafe() {
        return null;
      }

      @Override
      public ChannelPipeline pipeline() {
        return null;
      }

      @Override
      public ByteBufAllocator alloc() {
        return null;
      }

      @Override
      public Channel read() {
        return null;
      }

      @Override
      public Channel flush() {
        return null;
      }

      @Override
      public ChannelFuture bind(SocketAddress socketAddress) {
        return null;
      }

      @Override
      public ChannelFuture connect(SocketAddress socketAddress) {
        return null;
      }

      @Override
      public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1) {
        return null;
      }

      @Override
      public ChannelFuture disconnect() {
        return null;
      }

      @Override
      public ChannelFuture close() {
        return null;
      }

      @Override
      public ChannelFuture deregister() {
        return null;
      }

      @Override
      public ChannelFuture bind(SocketAddress socketAddress, ChannelPromise channelPromise) {
        return null;
      }

      @Override
      public ChannelFuture connect(SocketAddress socketAddress, ChannelPromise channelPromise) {
        return null;
      }

      @Override
      public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) {
        return null;
      }

      @Override
      public ChannelFuture disconnect(ChannelPromise channelPromise) {
        return null;
      }

      @Override
      public ChannelFuture close(ChannelPromise channelPromise) {
        return null;
      }

      @Override
      public ChannelFuture deregister(ChannelPromise channelPromise) {
        return null;
      }

      @Override
      public ChannelFuture write(Object o) {
        return null;
      }

      @Override
      public ChannelFuture write(Object o, ChannelPromise channelPromise) {
        return null;
      }

      @Override
      public ChannelFuture writeAndFlush(Object o, ChannelPromise channelPromise) {
        return null;
      }

      @Override
      public ChannelFuture writeAndFlush(Object o) {
        return null;
      }

      @Override
      public ChannelPromise newPromise() {
        return null;
      }

      @Override
      public ChannelProgressivePromise newProgressivePromise() {
        return null;
      }

      @Override
      public ChannelFuture newSucceededFuture() {
        return null;
      }

      @Override
      public ChannelFuture newFailedFuture(Throwable throwable) {
        return null;
      }

      @Override
      public ChannelPromise voidPromise() {
        return null;
      }

      @Override
      public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
        return null;
      }

      @Override
      public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
        return false;
      }

      @Override
      public int compareTo(@NotNull Channel o) {
        return 0;
      }
    }, null);
  }
}
