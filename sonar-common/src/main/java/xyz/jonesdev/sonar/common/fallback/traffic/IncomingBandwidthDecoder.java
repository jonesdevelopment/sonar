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

package xyz.jonesdev.sonar.common.fallback.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter;

@ChannelHandler.Sharable
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IncomingBandwidthDecoder extends ChannelInboundHandlerAdapter {
  public static final IncomingBandwidthDecoder INSTANCE = new IncomingBandwidthDecoder();

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx,
                          final @NotNull Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      TrafficCounter.INCOMING.increment(((ByteBuf) msg).readableBytes());
    }
    ctx.fireChannelRead(msg);
  }
}
