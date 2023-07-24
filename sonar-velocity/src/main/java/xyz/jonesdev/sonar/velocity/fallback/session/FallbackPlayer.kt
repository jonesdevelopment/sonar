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

package xyz.jonesdev.sonar.velocity.fallback.session

import com.velocitypowered.proxy.connection.MinecraftConnection
import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import io.netty.channel.Channel
import io.netty.channel.ChannelPipeline
import xyz.jonesdev.sonar.api.fallback.Fallback
import xyz.jonesdev.sonar.api.fallback.FallbackConnection
import java.net.InetAddress

class FallbackPlayer(
  private val fallback: Fallback,
  private val player: ConnectedPlayer,
  private val connection: MinecraftConnection,
  private val channel: Channel,
  private val pipeline: ChannelPipeline,
  private val inetAddress: InetAddress,
  private val protocolVersion: Int,
) : FallbackConnection<ConnectedPlayer?, MinecraftConnection?> {

  override fun getFallback(): Fallback {
    return fallback
  }

  override fun getPlayer(): ConnectedPlayer {
    return player
  }

  override fun getConnection(): MinecraftConnection {
    return connection
  }

  override fun getChannel(): Channel {
    return channel
  }

  override fun getPipeline(): ChannelPipeline {
    return pipeline
  }

  override fun getInetAddress(): InetAddress {
    return inetAddress
  }

  override fun getProtocolVersion(): Int {
    return protocolVersion
  }
}
