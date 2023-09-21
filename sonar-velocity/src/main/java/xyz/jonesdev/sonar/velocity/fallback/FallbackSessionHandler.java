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

package xyz.jonesdev.sonar.velocity.fallback;

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.common.fallback.FallbackVerificationHandler;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_ENCODER;

@Getter
public final class FallbackSessionHandler implements MinecraftSessionHandler {
  private final Fallback fallback;
  private final MinecraftConnection mcConnection;
  private final LoginInboundConnection inboundConnection;

  private static final MethodHandle CONNECTED_PLAYER;

  static {
    try {
      CONNECTED_PLAYER = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
        .findConstructor(ConnectedPlayer.class,
          MethodType.methodType(
            void.class,
            VelocityServer.class,
            GameProfile.class,
            MinecraftConnection.class,
            InetSocketAddress.class,
            boolean.class,
            IdentifiedKey.class
          )
        );
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }
  }

  FallbackSessionHandler(final Fallback fallback,
                         final @NotNull MinecraftConnection mcConnection,
                         final LoginInboundConnection inboundConnection,
                         final GameProfile gameProfile,
                         final InetAddress inetAddress,
                         final boolean onlineMode) {
    this.fallback = fallback;
    this.mcConnection = mcConnection;
    this.inboundConnection = inboundConnection;

    // Don't allow exceptions or disconnect messages
    mcConnection.setAssociation(null);

    // Create an instance for the connected player
    final ConnectedPlayer connectedPlayer;
    try {
      connectedPlayer = (ConnectedPlayer) CONNECTED_PLAYER.invokeExact(
        mcConnection.server,
        gameProfile,
        mcConnection,
        inboundConnection.getVirtualHost().orElse(null),
        onlineMode,
        inboundConnection.getIdentifiedKey()
      );
    } catch (Throwable throwable) {
      // This should not happen
      fallback.getLogger().error("Error processing {}: {}", gameProfile.getName(), throwable);
      mcConnection.close(true);
      return;
    }

    // Create an instance for the Fallback connection
    final FallbackPlayerWrapper fallbackPlayer = new FallbackPlayerWrapper(
      fallback, connectedPlayer, mcConnection, mcConnection.getChannel(),
      mcConnection.getChannel().pipeline(), inetAddress,
      ProtocolVersion.fromId(connectedPlayer.getProtocolVersion().getProtocol())
    );

    // Check if the player is already connected to the proxy
    if (!mcConnection.server.canRegisterConnection(connectedPlayer)) {
      fallbackPlayer.disconnect("Could not find any available servers.");
      return;
    }

    // The player joined the verification
    Statistics.REAL_TRAFFIC.increment();

    if (Sonar.get().getConfig().LOG_CONNECTIONS) {
      // Only log the processing message if the server isn't under attack.
      // We let the user override this through the configuration.
      if (!fallback.isPotentiallyUnderAttack() || Sonar.get().getConfig().LOG_DURING_ATTACK) {
        fallback.getLogger().info(Sonar.get().getConfig().VERIFICATION_CONNECT_LOG
          .replace("%name%", connectedPlayer.getUsername())
          .replace("%ip%", Sonar.get().getConfig().formatAddress(fallbackPlayer.getInetAddress()))
          .replace("%protocol%", String.valueOf(fallbackPlayer.getProtocolVersion().getProtocol())));
      }
    }

    // Mark the player as connected → verifying players
    fallback.getConnected().put(gameProfile.getName(), inetAddress);

    // This sometimes happens when the channel hangs, but the player is still connecting
    // This also fixes a weird issue with TCPShield and other reverse proxies
    if (fallbackPlayer.getPipeline().get(MINECRAFT_ENCODER) == null
      || fallbackPlayer.getPipeline().get(MINECRAFT_DECODER) == null) {
      mcConnection.close(true);
      return;
    }

    // Replace normal encoder to allow custom packets
    fallbackPlayer.getPipeline().replace(
      MINECRAFT_ENCODER,
      FALLBACK_PACKET_ENCODER,
      new FallbackPacketEncoder(fallbackPlayer.getProtocolVersion())
    );

    // Replace normal decoder to allow custom packets
    fallbackPlayer.getPipeline().replace(
      MINECRAFT_DECODER,
      FALLBACK_PACKET_DECODER,
      new FallbackPacketDecoder(
        fallbackPlayer,
        new FallbackVerificationHandler(
          fallbackPlayer,
          gameProfile.getName(),
          connectedPlayer.getUniqueId()
        )
      )
    );
  }

  @Override
  public void handleUnknown(final ByteBuf buf) {
    mcConnection.close(true);
  }
}
