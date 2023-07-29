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

package xyz.jonesdev.sonar.velocity.fallback.handler;

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.exception.ReflectionException;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.velocity.SonarVelocity;
import xyz.jonesdev.sonar.velocity.fallback.FallbackPlayer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_ENCODER;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.getJoinPacketForVersion;

@Getter
public final class FallbackLoginHandler implements MinecraftSessionHandler {
  private final Fallback fallback;
  private final MinecraftConnection mcConnection;
  private final LoginInboundConnection inboundConnection;
  private final InitialLoginSessionHandler sessionHandler;
  private final VelocityServer server;

  private static final String MOJANG_HASJOINED_URL =
    System.getProperty("mojang.sessionserver",
        "https://sessionserver.mojang.com/session/minecraft/hasJoined")
      .concat("?username=%s&serverId=%s");
  private static final MethodHandle CONNECTED_PLAYER;
  private static final Field LOGIN_PACKET;

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

      LOGIN_PACKET = InitialLoginSessionHandler.class.getDeclaredField("login");
      LOGIN_PACKET.setAccessible(true);
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
    }
  }

  public FallbackLoginHandler(final Fallback fallback,
                              final MinecraftConnection mcConnection,
                              final LoginInboundConnection inboundConnection,
                              final InitialLoginSessionHandler sessionHandler,
                              final String username,
                              final InetAddress inetAddress,
                              final boolean onlineMode) {
    this.fallback = fallback;
    this.mcConnection = mcConnection;
    this.inboundConnection = inboundConnection;
    this.sessionHandler = sessionHandler;
    this.server = (VelocityServer) SonarVelocity.INSTANCE.getPlugin().getServer();

    // Create an instance for the connected player
    final ConnectedPlayer connectedPlayer;
    try {
      connectedPlayer = (ConnectedPlayer) CONNECTED_PLAYER.invokeExact(
        mcConnection.server,
        GameProfile.forOfflinePlayer(username),
        mcConnection,
        inboundConnection.getVirtualHost().orElse(null),
        onlineMode,
        inboundConnection.getIdentifiedKey()
      );
    } catch (Throwable throwable) {
      // This should not happen
      fallback.getLogger().error("Error processing {}: {}", username, throwable);
      mcConnection.close(true);
      return;
    }

    // Create an instance for the Fallback connection
    final FallbackPlayer fallbackPlayer = new FallbackPlayer(
      fallback, connectedPlayer, mcConnection, mcConnection.getChannel(),
      mcConnection.getChannel().pipeline(), inetAddress,
      ProtocolVersion.fromId(connectedPlayer.getProtocolVersion().getProtocol())
    );

    // Check if the player is already connected to the proxy
    if (!mcConnection.server.canRegisterConnection(connectedPlayer)) {
      fallbackPlayer.disconnect("Could not find any available servers.");
      return;
    }

    if (fallback.getSonar().getConfig().LOG_CONNECTIONS) {
      // Only log the processing message if the server isn't under attack.
      // We let the user override this through the configuration.
      if (!fallback.isUnderAttack() || fallback.getSonar().getConfig().LOG_DURING_ATTACK) {
        fallback.getLogger().info("Processing: {}{} ({})",
          username, fallback.getSonar().getConfig().formatAddress(inetAddress),
          fallbackPlayer.getProtocolVersion().getProtocol());
      }
    }

    // Mark the player as connected → verifying players
    fallback.getConnected().put(username, inetAddress);

    // Set compression
    final int threshold = mcConnection.server.getConfiguration().getCompressionThreshold();
    if (threshold >= 0 && fallbackPlayer.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
      mcConnection.write(new SetCompression(threshold));
      mcConnection.setCompressionThreshold(threshold);
    }

    // Send LoginSuccess packet to make the client think they successfully logged in
    final ServerLoginSuccess loginSuccess = new ServerLoginSuccess();

    loginSuccess.setUsername(connectedPlayer.getUsername());
    loginSuccess.setProperties(connectedPlayer.getGameProfileProperties());
    loginSuccess.setUuid(connectedPlayer.getUniqueId());

    mcConnection.write(loginSuccess);

    // Spoof online state
    mcConnection.setAssociation(connectedPlayer);
    mcConnection.setState(StateRegistry.PLAY);

    // Replace normal encoder to allow custom packets
    fallbackPlayer.getPipeline().replace(
      MINECRAFT_ENCODER,
      FALLBACK_PACKET_ENCODER,
      new FallbackPacketEncoder(fallbackPlayer.getProtocolVersion())
    );

    // Replace normal decoder to allow custom packets
    /*fallbackPlayer.getPipeline().replace(
      MINECRAFT_DECODER,
      FALLBACK_PACKET_DECODER,
      new FallbackPacketDecoder(
        fallbackPlayer.getProtocolVersion(),
        new FallbackVerificationHandler(fallbackPlayer)
      )
    );*/

    mcConnection.write(getJoinPacketForVersion(fallbackPlayer.getProtocolVersion()));
  }

  @Override
  public boolean handle(final LoginPluginResponse packet) {
    return sessionHandler.handle(packet);
  }

  @Override
  public void handleUnknown(final ByteBuf buf) {
    mcConnection.close(true);
  }

  @Override
  public void disconnected() {
    sessionHandler.disconnected();
  }
}
