/*
 * Copyright (C) 2025 Sonar Contributors
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
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

@ChannelHandler.Sharable
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FallbackBandwidthHandler extends ChannelDuplexHandler {
  public static final ChannelHandler INSTANCE = new FallbackBandwidthHandler();

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      // Increment the incoming traffic by the number of readable bytes
      final int readableBytes = ((ByteBuf) msg).readableBytes();
      GlobalSonarStatistics.perSecondIncomingTraffic += readableBytes;
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
    if (msg instanceof ByteBuf) {
      // Increment the outgoing traffic by the number of readable bytes
      final int readableBytes = ((ByteBuf) msg).readableBytes();
      GlobalSonarStatistics.perSecondOutgoingTraffic += readableBytes;
    }
    ctx.write(msg, promise);
  }
}
