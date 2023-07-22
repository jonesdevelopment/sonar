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

class FallbackPacketDecoder(
  private val fallbackPlayer: FallbackPlayer,
  private val startKeepAliveId: Long,
) : ChannelInboundHandlerAdapter() {
  private var packets = 0
  private val loginTimestamp = System.currentTimeMillis()

  @Throws(Exception::class)
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    if (ctx.channel().isActive) {
      ctx.close()
    }
  }

  @Throws(Exception::class)
  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {

    // Check if the client is not sending a ton of packets to the server
    val checkPackets = ++packets <= fallbackPlayer.fallback.sonar.config.MAXIMUM_LOGIN_PACKETS
    FallbackSessionHandler.checkFrame(fallbackPlayer, checkPackets, "too many packets")

    // Check for timeout since the player could be sending packets but not important ones
    val delay = System.currentTimeMillis() - loginTimestamp
    val timeout = fallbackPlayer.fallback.sonar.config.VERIFICATION_TIMEOUT
    // Check if the time limit has exceeded
    if (delay > timeout) {
      // TODO: Can we fail the verification?
      fallbackPlayer.connection.close()
      return
    }

    if (msg is MinecraftPacket) {
      val legalPacket = msg is ClientSettings || msg is PluginMessage || msg is KeepAlive || msg is ResourcePackResponse

      // Check if the client is sending packets we don't want them to send
      FallbackSessionHandler.checkFrame(fallbackPlayer, legalPacket, "bad packet: " + msg.javaClass.simpleName)

      val hasFallbackHandler = fallbackPlayer.connection.sessionHandler!! is FallbackSessionHandler

      // KeepAlive received, start verification
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
