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

package xyz.jonesdev.sonar.common.fallback;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static xyz.jonesdev.sonar.common.statistics.CachedBandwidthStatistics.INCOMING;
import static xyz.jonesdev.sonar.common.statistics.CachedBandwidthStatistics.OUTGOING;

@ChannelHandler.Sharable
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FallbackBandwidthHandler extends ChannelDuplexHandler {
  public static final ChannelHandler INSTANCE = new FallbackBandwidthHandler();

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx,
                          final @NotNull Object msg) throws Exception {
    // We can only get the size of a message if it's a ByteBuf
    if (msg instanceof ByteBuf) {
      // Increment the incoming traffic by the number of readable bytes
      INCOMING.increment(((ByteBuf) msg).readableBytes());
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final ChannelPromise promise) throws Exception {
    // We can only get the size of a message if it's a ByteBuf
    if (msg instanceof ByteBuf) {
      // Increment the outgoing traffic by the number of readable bytes
      OUTGOING.increment(((ByteBuf) msg).readableBytes());
    }
    // Make sure to let the server handle the rest
    ctx.write(msg, promise);
  }
}
