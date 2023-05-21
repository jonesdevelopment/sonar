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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import jones.sonar.api.Sonar;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.api.logger.Logger;
import jones.sonar.common.fallback.FallbackChannelHandler;
import jones.sonar.common.fallback.FallbackTimeoutHandler;
import jones.sonar.velocity.SonarVelocity;
import jones.sonar.velocity.fallback.dummy.DummyConnection;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static jones.sonar.api.fallback.FallbackPipelines.DECODER;
import static jones.sonar.api.fallback.FallbackPipelines.HANDLER;
import static jones.sonar.velocity.fallback.FallbackListener.CachedMessages.*;

@RequiredArgsConstructor
public final class FallbackListener {
  private final Logger logger;
  private final Fallback fallback;

  // We need to cache if the joining player is a premium player or not
  // If we don't do that, many authentication plugins can potentially break
  private final Collection<String> premium = new Vector<>();

  public static class CachedMessages {
    static PreLoginEvent.PreLoginComponentResult TOO_MANY_PLAYERS;
    static PreLoginEvent.PreLoginComponentResult BLACKLISTED;
    static PreLoginEvent.PreLoginComponentResult ALREADY_VERIFYING;
    static Component TOO_MANY_VERIFICATIONS;
    public static Component NO_SERVER_FOUND;

    public static void update() {
      ALREADY_VERIFYING = PreLoginEvent.PreLoginComponentResult.denied(
        Component.text(Sonar.get().getConfig().ALREADY_VERIFYING)
      );
      TOO_MANY_PLAYERS = PreLoginEvent.PreLoginComponentResult.denied(
        Component.text(Sonar.get().getConfig().TOO_MANY_PLAYERS)
      );
      BLACKLISTED = PreLoginEvent.PreLoginComponentResult.denied(
        Component.text(Sonar.get().getConfig().BLACKLISTED)
      );
      TOO_MANY_VERIFICATIONS = Component.text(Sonar.get().getConfig().TOO_MANY_VERIFICATIONS);
      NO_SERVER_FOUND = Component.text(Sonar.get().getConfig().NO_SERVER_FOUND);
    }
  }

  private static final DummyConnection CLOSED_MINECRAFT_CONNECTION;

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final MethodHandle INITIAL_CONNECTION;
  private static final MethodHandle CONNECTED_PLAYER;
  private static final Field CONNECTION_FIELD;

  static {
    CLOSED_MINECRAFT_CONNECTION = new DummyConnection(null);

    // https://github.com/Elytrium/LimboAPI/blob/ca6eb7155740bf3ff32596412a48e537fe55606d/plugin/src/main/java/net/elytrium/limboapi/injection/login/LoginListener.java#L239
    try {
      CONNECTION_FIELD = AuthSessionHandler.class.getDeclaredField("mcConnection");
      CONNECTION_FIELD.setAccessible(true);

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

      INITIAL_CONNECTION = MethodHandles.privateLookupIn(LoginInboundConnection.class, LOOKUP)
        .findGetter(LoginInboundConnection.class, "delegate", InitialInboundConnection.class);
    } catch (Throwable throwable) {
      throw new IllegalStateException();
    }
  }

  /**
   * If we don't handle online/offline mode players correctly,
   * many plugins (especially Auth-related) will have issues
   *
   * @param event PreLoginEvent
   */
  @Subscribe(order = PostOrder.LAST)
  public void handle(final PreLoginEvent event) {
    Sonar.get().getStatistics().increment("total");

    var inetAddress = event.getConnection().getRemoteAddress().getAddress();

    if (fallback.getBlacklisted().contains(inetAddress)) {
      event.setResult(BLACKLISTED);
      return;
    }

    if (fallback.getVerified().contains(inetAddress)) return;

    // Check if Fallback is already verifying a player
    // → is another player with the same ip address connected to Fallback?
    if (fallback.getConnected().contains(inetAddress)) {
      event.setResult(ALREADY_VERIFYING);
      return;
    }

    // We cannot allow too many players on our Fallback server
    if (fallback.getQueue().getQueuedPlayers().size() > Sonar.get().getConfig().MAXIMUM_QUEUED_PLAYERS
      || fallback.getConnected().size() > Sonar.get().getConfig().MAXIMUM_VERIFYING_PLAYERS) {
      event.setResult(TOO_MANY_PLAYERS);
      return;
    }

    if (event.getResult().isForceOfflineMode()) return;
    if (!SonarVelocity.INSTANCE.getPlugin().getServer().getConfiguration().isOnlineMode()
      && !event.getResult().isOnlineModeAllowed()) return;

    premium.add(event.getUsername());
  }

  /**
   * Handles inbound connections
   *
   * @param event GameProfileRequestEvent
   * @throws java.lang.Throwable Unexpected error
   */
  @Subscribe(order = PostOrder.LAST)
  public void handle(final GameProfileRequestEvent event) throws Throwable {
    var inetAddress = event.getConnection().getRemoteAddress().getAddress();

    // We don't want to check players that have already been verified
    if (fallback.getVerified().contains(inetAddress)) return;

    var inboundConnection = (LoginInboundConnection) event.getConnection();
    var initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

    var mcConnection = initialConnection.getConnection();
    var channel = mcConnection.getChannel();

    // The AuthSessionHandler isn't supposed to continue the connection process
    // which is why we override the field value for the MinecraftConnection with
    // a dummy connection
    CONNECTION_FIELD.set(mcConnection.getSessionHandler(), CLOSED_MINECRAFT_CONNECTION);

    channel.eventLoop().execute(() -> {
      if (mcConnection.isClosed()) return;

      // Replace timeout handler to avoid known exploits or issues
      // We also want to timeout bots quickly to avoid flooding
      channel.pipeline().replace(Connections.READ_TIMEOUT, Connections.READ_TIMEOUT,
        new FallbackTimeoutHandler(
          Sonar.get().getConfig().VERIFICATION_TIMEOUT,
          TimeUnit.MILLISECONDS
        ));

      // We have to add this pipeline to monitor whenever the client disconnects
      // to remove them from the list of connected and queued players
      channel.pipeline().addFirst(HANDLER, FallbackChannelHandler.INSTANCE);

      // Queue the connection for further processing
      fallback.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {
        if (mcConnection.isClosed()) return;

        // Most of the following code was taken from Velocity
        try {

          // Create an instance for player
          var player = (ConnectedPlayer) CONNECTED_PLAYER.invokeExact(
            mcConnection.server,
            event.getGameProfile(),
            mcConnection,
            inboundConnection.getVirtualHost().orElse(null),
            premium.contains(event.getUsername()),
            inboundConnection.getIdentifiedKey()
          );

          // Remove the player from the premium list in order to prevent memory leaks
          // We cannot rely on the DisconnectEvent since the server will not call it
          // -> we are intercepting the packets!
          premium.remove(event.getUsername());

          // Check if the ip address had too many verifications or is rejoining too quickly
          if (!fallback.getAttemptLimiter().allow(inetAddress)) {
            player.disconnect0(TOO_MANY_VERIFICATIONS, true);
            return;
          }

          // Check if the player is already connected to the proxy
          // We use the default Velocity method for this to avoid incompatibilities
          if (!mcConnection.server.canRegisterConnection(player)) {
            player.disconnect0(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED),
              true);
            return;
          }

          // Create an instance for the Fallback connection
          var fallbackPlayer = new FallbackPlayer(
            fallback,
            player, mcConnection, channel, channel.pipeline(), inetAddress,
            player.getProtocolVersion().getProtocol()
          );

          // ==================================================================
          if (!fallback.isUnderAttack()) {
            logger.info("[Fallback] Processing: {}{} ({})",
              event.getUsername(), inetAddress, fallbackPlayer.getProtocolVersion());
          }

          fallback.getConnected().add(inetAddress);
          // ==================================================================

          // Set compression
          var threshold = mcConnection.server.getConfiguration().getCompressionThreshold();

          if (threshold >= 0 && mcConnection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
            mcConnection.write(new SetCompression(threshold));
            mcConnection.setCompressionThreshold(threshold);
          }

          // Send LoginSuccess packet to spoof our fake lobby
          var success = new ServerLoginSuccess();

          success.setUsername(player.getUsername());
          success.setProperties(player.getGameProfileProperties());
          success.setUuid(player.getUniqueId());

          mcConnection.write(success);

          // Set the state to a custom one, so we can receive and send more packets
          mcConnection.setAssociation(player);
          mcConnection.setState(StateRegistry.PLAY);

          // ==================================================================
          // The first step of the verification is a simple KeepAlive packet
          // We don't want to waste resources by directly sending all packets to
          // the client which is why we first send a KeepAlive packet and then
          // wait for a valid response to continue the verification process
          final KeepAlive keepAlive = new KeepAlive();
          final long keepAliveId = ThreadLocalRandom.current().nextInt();

          keepAlive.setRandomId(keepAliveId);

          // We have to add this pipeline to monitor all incoming traffic
          // We add the pipeline after the MinecraftDecoder since we want
          // the packets to be processed and decoded already
          fallbackPlayer.getPipeline().addAfter(
            Connections.MINECRAFT_DECODER,
            DECODER,
            new FallbackPacketDecoder(fallbackPlayer, keepAliveId)
          );

          mcConnection.write(keepAlive);
          // ==================================================================
        } catch (Throwable throwable) {
          throw new RuntimeException(throwable);
        }
      }));
    });
  }
}
