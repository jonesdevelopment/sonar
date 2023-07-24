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

package jones.sonar.common.fallback

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import jones.sonar.api.Sonar
import java.net.InetSocketAddress

class FallbackChannelHandler(
  private val username: String
) : ChannelInboundHandlerAdapter() {

  @Throws(Exception::class)
  override fun channelUnregistered(ctx: ChannelHandlerContext) {
    ctx.fireChannelUnregistered()

    val inetAddress = (ctx.channel().remoteAddress() as InetSocketAddress).address

    // Remove the IP address from the queue
    Sonar.get().fallback.connected.remove(username)
    Sonar.get().fallback.queue.remove(inetAddress)
  }
}
