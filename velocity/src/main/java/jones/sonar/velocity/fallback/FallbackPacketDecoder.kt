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

import com.velocitypowered.proxy.connection.MinecraftConnection
import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import com.velocitypowered.proxy.protocol.MinecraftPacket
import com.velocitypowered.proxy.protocol.packet.ClientSettings
import com.velocitypowered.proxy.protocol.packet.KeepAlive
import com.velocitypowered.proxy.protocol.packet.PluginMessage
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import jones.sonar.api.fallback.FallbackConnection

class FallbackPacketDecoder(
    private val player: FallbackConnection<ConnectedPlayer, MinecraftConnection>,
    private val startKeepAliveId: Long
) : ChannelInboundHandlerAdapter() {

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is MinecraftPacket) {
            val legalPacket = msg is ClientSettings || msg is PluginMessage || msg is KeepAlive

            FallbackSessionHandler.checkFrame(player, legalPacket, "bad packet: " + msg.javaClass.simpleName)

            val hasFallbackHandler = player.connection.sessionHandler is FallbackSessionHandler

            if (msg is KeepAlive && msg.randomId == startKeepAliveId) {
                if (hasFallbackHandler) {
                    player.fail("duplicate packet")
                    return
                }

                player.connection.delayedWrite(FallbackPackets.getJoinPacketForVersion(player.protocolVersion))

                // Set session handler to custom fallback handler to intercept all incoming packets
                player.connection.sessionHandler = FallbackSessionHandler(
                    player.connection.sessionHandler, player
                )

                player.connection.flush()
                return  // Don't read this packet twice
            } else if (!hasFallbackHandler) {
                player.fail("handler not initialized yet")
                return  // Don't handle illegal packets
            }
        }

        // We want the backend server to actually receive the packets
        // We also want the session handler to handle the packets properly
        ctx.fireChannelRead(msg)
    }
}
