/*
 * Copyright (C) 2023 jones
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

package jones.sonar.velocity.fallback.session;

import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import jones.sonar.api.fallback.FallbackConnection;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16;
import static jones.sonar.api.fallback.FallbackPipelines.RESPAWN;

@RequiredArgsConstructor
final class FallbackRespawnHandler extends ChannelOutboundHandlerAdapter {
  private final @NotNull FallbackConnection<ConnectedPlayer, MinecraftConnection> player;

  @Override
  public void write(final ChannelHandlerContext ctx,
                    final Object msg,
                    final ChannelPromise promise) throws Exception {
    if (msg instanceof JoinGame joinGame) {

      // Remove the pipeline to avoid issues
      player.getPipeline().remove(RESPAWN);

      // Fix the chunks
      if (player.getConnection().getType() == ConnectionTypes.LEGACY_FORGE) {
        doSafeClientServerSwitch(joinGame);
      } else {
        doFastClientServerSwitch(joinGame);
      }
      return;
    }

    // Allow any other packet
    ctx.write(msg, promise);
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
}
