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

package jones.sonar.velocity.fallback;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import jones.sonar.api.fallback.FallbackConnection;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;
import static jones.sonar.velocity.fallback.FallbackPackets.getJoinPacketForVersion;

@RequiredArgsConstructor
public final class FallbackPacketDecoder extends ChannelInboundHandlerAdapter {
  private final @NotNull FallbackConnection<ConnectedPlayer, MinecraftConnection> player;
  private final long startKeepAliveId;

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx,
                          final @NotNull Object msg) throws Exception {
    if (msg instanceof MinecraftPacket packet) {
      final boolean legalPacket = packet instanceof ClientSettings
        || packet instanceof PluginMessage || packet instanceof KeepAlive;

      checkFrame(legalPacket, "unexpected packet: " + packet.getClass().getSimpleName());

      final boolean hasFallbackHandler = player.getConnection().getSessionHandler() instanceof FallbackSessionHandler;

      if (packet instanceof KeepAlive keepAlive && keepAlive.getRandomId() == startKeepAliveId) {
        if (hasFallbackHandler) {
          player.fail("duplicate packet");
          return;
        }

        player.getConnection().delayedWrite(getJoinPacketForVersion(player.getProtocolVersion()));

        // Set session handler to custom fallback handler to intercept all incoming packets
        player.getConnection().setSessionHandler(new FallbackSessionHandler(
          player.getConnection().getSessionHandler(), player
        ));

        player.getConnection().flush();
        return; // Don't read this packet twice
      } else if (!hasFallbackHandler) {
        player.fail("handler not initialized yet");
        return; // Don't handle illegal packets
      }
    }

    // We want the backend server to actually receive the packets
    // We also want the session handler to handle the packets properly
    ctx.fireChannelRead(msg);
  }
}
