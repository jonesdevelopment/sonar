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

import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import jones.sonar.api.fallback.FallbackConnection;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static com.velocitypowered.api.network.ProtocolVersion.*;
import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;

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

        final JoinGame joinGame = getForVersion(player.getProtocolVersion());

        if (player.getConnection().getType() == ConnectionTypes.LEGACY_FORGE) {
          doSafeClientServerSwitch(joinGame);
        } else {
          doFastClientServerSwitch(joinGame);
        }

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

  // Taken from Velocity
  private void doFastClientServerSwitch(final JoinGame joinGame) {
    // In order to handle switching to another server, you will need to send two packets:
    //
    // - The join game packet from the backend server, with a different dimension
    // - A respawn with the correct dimension
    //
    // Most notably, by having the client accept the join game packet, we can work around the need
    // to perform entity ID rewrites, eliminating potential issues from rewriting packets and
    // improving compatibility with mods.
    final Respawn respawn = Respawn.fromJoinGame(joinGame);

    if (player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_16) < 0) {
      // Before Minecraft 1.16, we could not switch to the same dimension without sending an
      // additional respawn. On older versions of Minecraft this forces the client to perform
      // garbage collection which adds additional latency.
      joinGame.setDimension(joinGame.getDimension() == 0 ? -1 : 0);
    }

    player.getConnection().delayedWrite(joinGame);
    player.getConnection().delayedWrite(respawn);
  }

  // Taken from Velocity
  private void doSafeClientServerSwitch(final JoinGame joinGame) {
    // Some clients do not behave well with the "fast" respawn sequence. In this case we will use
    // a "safe" respawn sequence that involves sending three packets to the client. They have the
    // same effect but tend to work better with buggier clients (Forge 1.8 in particular).

    // Send the JoinGame packet itself, unmodified.
    player.getConnection().delayedWrite(joinGame);

    // Send a respawn packet in a different dimension.
    final Respawn fakeSwitchPacket = Respawn.fromJoinGame(joinGame);
    fakeSwitchPacket.setDimension(joinGame.getDimension() == 0 ? -1 : 0);
    player.getConnection().delayedWrite(fakeSwitchPacket);

    // Now send a respawn packet in the correct dimension.
    final Respawn correctSwitchPacket = Respawn.fromJoinGame(joinGame);
    player.getConnection().delayedWrite(correctSwitchPacket);
  }

  private static JoinGame getForVersion(final int protocolVersion) {
    if (protocolVersion >= MINECRAFT_1_19_4.getProtocol()) {
      return FallbackPackets.JOIN_GAME_1_19_4;
    } else if (protocolVersion >= MINECRAFT_1_19_1.getProtocol()) {
      return FallbackPackets.JOIN_GAME_1_19_1;
    } else if (protocolVersion >= MINECRAFT_1_18_2.getProtocol()) {
      return FallbackPackets.JOIN_GAME_1_18_2;
    } else if (protocolVersion >= MINECRAFT_1_16_2.getProtocol()) {
      return FallbackPackets.JOIN_GAME_1_16_2;
    }
    return FallbackPackets.LEGACY_JOIN_GAME;
  }
}
