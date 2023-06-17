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

package jones.sonar.velocity.fallback

import com.velocitypowered.proxy.protocol.MinecraftPacket
import com.velocitypowered.proxy.protocol.packet.ClientSettings
import com.velocitypowered.proxy.protocol.packet.KeepAlive
import com.velocitypowered.proxy.protocol.packet.PluginMessage
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import jones.sonar.velocity.fallback.FallbackPackets.getJoinPacketForVersion
import jones.sonar.velocity.fallback.session.FallbackPlayer
import jones.sonar.velocity.fallback.session.FallbackSessionHandler
import java.io.IOException
import java.net.InetSocketAddress

class FallbackPacketDecoder(
  private val fallbackPlayer: FallbackPlayer,
  private val startKeepAliveId: Long
) : ChannelInboundHandlerAdapter() {

  @Throws(Exception::class)
  @Suppress("OverridingDeprecatedMember")
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    if (ctx.channel().isActive) {
      ctx.close()

      // Clients can throw an IOException if the connection is interrupted unexpectedly
      // Velocity can throw an IllegalStateException if Sonar messed something up
      if (cause is IOException || cause is IllegalStateException) return

      // Blacklist the IP address
      val inetAddress = (ctx.channel().remoteAddress() as InetSocketAddress).address

      fallbackPlayer.fallback.blacklisted.add(inetAddress)
    }
  }

  @Throws(Exception::class)
  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    if (msg is MinecraftPacket) {
      val legalPacket = msg is ClientSettings || msg is PluginMessage || msg is KeepAlive || msg is ResourcePackResponse

      FallbackSessionHandler.checkFrame(fallbackPlayer, legalPacket, "bad packet: " + msg.javaClass.simpleName)

      val hasFallbackHandler = fallbackPlayer.connection.sessionHandler!! is FallbackSessionHandler

      if (msg is KeepAlive && msg.randomId == startKeepAliveId) {
        if (hasFallbackHandler) {
          fallbackPlayer.fail("handler already initialized")
          return
        }

        // Set session handler to custom fallback handler to intercept all incoming packets
        fallbackPlayer.connection.sessionHandler = FallbackSessionHandler(
          fallbackPlayer.connection.sessionHandler, fallbackPlayer
        )

        // Create JoinGame packet for the client's version
        val joinGame = getJoinPacketForVersion(fallbackPlayer.player.protocolVersion)

        // Send JoinGame packet
        fallbackPlayer.connection.write(joinGame)
        return // Don't read this packet twice
      } else if (!hasFallbackHandler) {
        fallbackPlayer.fail("handler not initialized yet")
        return // Don't handle illegal packets
      }
    }

    // The session handler must handle the packets properly
    ctx.fireChannelRead(msg)
  }
}
