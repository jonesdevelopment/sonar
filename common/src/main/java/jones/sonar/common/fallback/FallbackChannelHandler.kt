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

package jones.sonar.common.fallback

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import jones.sonar.api.Sonar
import jones.sonar.api.fallback.Fallback
import java.net.InetSocketAddress

@Sharable
class FallbackChannelHandler(private val fallback: Fallback) : ChannelInboundHandlerAdapter() {

  @Throws(Exception::class)
  override fun channelInactive(ctx: ChannelHandlerContext) {
    ctx.fireChannelInactive()

    val inetAddress = (ctx.channel().remoteAddress() as InetSocketAddress).address

    // Remove the IP address from the queue
    fallback.connected.remove(inetAddress)
    fallback.queue.remove(inetAddress)
  }

  companion object {

    @JvmField
    val INSTANCE = FallbackChannelHandler(Sonar.get().fallback)
  }
}
