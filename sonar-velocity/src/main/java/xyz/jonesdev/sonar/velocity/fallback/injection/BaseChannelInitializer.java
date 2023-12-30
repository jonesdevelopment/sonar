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

package xyz.jonesdev.sonar.velocity.fallback.injection;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.common.fallback.FallbackTimeoutHandler;
import xyz.jonesdev.sonar.velocity.fallback.FallbackMinecraftConnection;

import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.network.Connections.*;

@RequiredArgsConstructor
public final class BaseChannelInitializer extends ChannelInitializer<Channel> {
  private final VelocityServer server;

  // Taken from
  // https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/network/ServerChannelInitializer.java
  @Override
  protected void initChannel(final @NotNull Channel channel) {
    channel.pipeline()
      .addLast(LEGACY_PING_DECODER, new LegacyPingDecoder())
      .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
      .addLast(READ_TIMEOUT,
        new FallbackTimeoutHandler(server.getConfiguration().getReadTimeout(), TimeUnit.MILLISECONDS))
      .addLast(LEGACY_PING_ENCODER, LegacyPingEncoder.INSTANCE)
      .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
      .addLast(MINECRAFT_DECODER, new MinecraftDecoder(ProtocolUtils.Direction.SERVERBOUND))
      .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(ProtocolUtils.Direction.CLIENTBOUND));

    final MinecraftConnection connection = new FallbackMinecraftConnection(channel, server);
    connection.setActiveSessionHandler(StateRegistry.HANDSHAKE,
      new HandshakeSessionHandler(connection, server));
    channel.pipeline().addLast(Connections.HANDLER, connection);

    if (server.getConfiguration().isProxyProtocol()) {
      channel.pipeline().addFirst(new HAProxyMessageDecoder());
    }
  }
}
