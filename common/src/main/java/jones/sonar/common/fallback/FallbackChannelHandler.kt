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
import java.io.IOException
import java.net.InetSocketAddress

@Sharable
class FallbackChannelHandler(private val fallback: Fallback) : ChannelInboundHandlerAdapter() {

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (ctx.channel().isActive) {
            ctx.close()

            // Clients throw an IOException if the connection is interrupted
            // unexpectedly - we cannot blacklist for this
            if (cause is IOException) return

            val inetAddress = (ctx.channel().remoteAddress() as InetSocketAddress).address

            fallback.blacklisted.add(inetAddress)
        }
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)

        val inetAddress = (ctx.channel().remoteAddress() as InetSocketAddress).address

        fallback.connected.remove(inetAddress)
        fallback.queue.queuedPlayers.remove(inetAddress)
    }

    companion object {

        @JvmField
        val INSTANCE = FallbackChannelHandler(Sonar.get().fallback)
    }
}
