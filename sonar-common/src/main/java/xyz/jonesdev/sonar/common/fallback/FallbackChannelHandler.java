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

package xyz.jonesdev.sonar.common.fallback;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import xyz.jonesdev.sonar.api.Sonar;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@RequiredArgsConstructor
public final class FallbackChannelHandler extends ChannelInboundHandlerAdapter {
  private final String username;

  @Override
  public void channelUnregistered(final ChannelHandlerContext ctx) {
    ctx.fireChannelUnregistered();

    final InetAddress inetAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();

    // Remove the IP address from the queue
    Sonar.get().getFallback().getConnected().remove(username);
    Sonar.get().getFallback().getQueue().remove(inetAddress);
  }
}
