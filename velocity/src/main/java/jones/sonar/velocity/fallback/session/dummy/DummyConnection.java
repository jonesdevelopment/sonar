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

package jones.sonar.velocity.fallback.session.dummy;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.*;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DummyConnection extends MinecraftConnection {
  public DummyConnection(final VelocityServer server) {
    super(new Channel() {
      @Override
      public ChannelId id() {
        return null;
      }

      @Override
      public EventLoop eventLoop() {
        return new EventLoop() {
          @Override
          public EventLoopGroup parent() {
            return null;
          }

          @Override
          public EventLoop next() {
            return null;
          }

          @Override
          public ChannelFuture register(Channel channel) {
            return null;
          }

          @Override
          public ChannelFuture register(ChannelPromise promise) {
            return null;
          }

          @Override
          public ChannelFuture register(Channel channel, ChannelPromise promise) {
            return null;
          }

          @Override
          public boolean inEventLoop() {
            return false;
          }

          @Override
          public boolean inEventLoop(Thread thread) {
            return false;
          }

          @Override
          public <V> Promise<V> newPromise() {
            return null;
          }

          @Override
          public <V> ProgressivePromise<V> newProgressivePromise() {
            return null;
          }

          @Override
          public <V> Future<V> newSucceededFuture(V result) {
            return null;
          }

          @Override
          public <V> Future<V> newFailedFuture(Throwable cause) {
            return null;
          }

          @Override
          public boolean isShuttingDown() {
            return false;
          }

          @Override
          public Future<?> shutdownGracefully() {
            return null;
          }

          @Override
          public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
            return null;
          }

          @Override
          public Future<?> terminationFuture() {
            return null;
          }

          @Override
          public void shutdown() {

          }

          @Override
          public List<Runnable> shutdownNow() {
            return null;
          }

          @Override
          public Iterator<EventExecutor> iterator() {
            return null;
          }

          @Override
          public Future<?> submit(Runnable task) {
            return null;
          }

          @Override
          public <T> Future<T> submit(Runnable task, T result) {
            return null;
          }

          @Override
          public <T> Future<T> submit(Callable<T> task) {
            return null;
          }

          @Override
          public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return null;
          }

          @Override
          public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return null;
          }

          @Override
          public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                                                        TimeUnit unit) {
            return null;
          }

          @Override
          public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                                                           TimeUnit unit) {
            return null;
          }

          @Override
          public boolean isShutdown() {
            return false;
          }

          @Override
          public boolean isTerminated() {
            return false;
          }

          @Override
          public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
            return false;
          }

          @NotNull
          @Override
          public <T> List<java.util.concurrent.Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return null;
          }

          @NotNull
          @Override
          public <T> List<java.util.concurrent.Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks,
                                                                    long timeout, @NotNull TimeUnit unit) throws InterruptedException {
            return null;
          }

          @NotNull
          @Override
          public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException,
            ExecutionException {
            return null;
          }

          @Override
          public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout,
                                 @NotNull TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
            return null;
          }

          @Override
          public void execute(@NotNull Runnable command) {

          }
        };
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
      public ChannelFuture bind(SocketAddress localAddress) {
        return null;
      }

      @Override
      public ChannelFuture connect(SocketAddress remoteAddress) {
        return null;
      }

      @Override
      public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
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
      public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return null;
      }

      @Override
      public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return null;
      }

      @Override
      public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return null;
      }

      @Override
      public ChannelFuture disconnect(ChannelPromise promise) {
        return null;
      }

      @Override
      public ChannelFuture close(ChannelPromise promise) {
        return null;
      }

      @Override
      public ChannelFuture deregister(ChannelPromise promise) {
        return null;
      }

      @Override
      public ChannelFuture write(Object msg) {
        return null;
      }

      @Override
      public ChannelFuture write(Object msg, ChannelPromise promise) {
        return null;
      }

      @Override
      public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return null;
      }

      @Override
      public ChannelFuture writeAndFlush(Object msg) {
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
      public ChannelFuture newFailedFuture(Throwable cause) {
        return null;
      }

      @Override
      public ChannelPromise voidPromise() {
        return null;
      }

      @Override
      public <T> Attribute<T> attr(AttributeKey<T> key) {
        return null;
      }

      @Override
      public <T> boolean hasAttr(AttributeKey<T> key) {
        return false;
      }

      @Override
      public int compareTo(@NotNull Channel o) {
        return 0;
      }
    }, server);
  }

  @Override
  public boolean isClosed() {
    return true;
  }
}
