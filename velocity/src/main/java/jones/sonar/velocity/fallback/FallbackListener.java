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
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import jones.sonar.api.Sonar;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.common.fallback.FallbackChannelHandler;
import jones.sonar.common.fallback.FallbackTimeoutHandler;
import jones.sonar.velocity.SonarVelocity;
import jones.sonar.velocity.fallback.session.FallbackPlayer;
import jones.sonar.velocity.fallback.session.FallbackSessionHandler;
import jones.sonar.velocity.fallback.session.dummy.DummyConnection;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;
import static jones.sonar.api.fallback.FallbackPipelines.DECODER;
import static jones.sonar.api.fallback.FallbackPipelines.HANDLER;
import static jones.sonar.velocity.fallback.FallbackListener.CachedMessages.*;
import static jones.sonar.velocity.fallback.FallbackPackets.LEGACY_JOIN_GAME;

@RequiredArgsConstructor
public final class FallbackListener {
  private final Fallback fallback;

  // We need to cache if the joining player is a premium player or not
  // If we don't do that, many authentication plugins can potentially break
  private final Collection<String> premium = new Vector<>(1);

  public static class CachedMessages {
    static PreLoginEvent.PreLoginComponentResult TOO_MANY_PLAYERS;
    static PreLoginEvent.PreLoginComponentResult BLACKLISTED;
    static PreLoginEvent.PreLoginComponentResult ALREADY_VERIFYING;
    static PreLoginEvent.PreLoginComponentResult TOO_MANY_ONLINE_PER_IP;
    static Component TOO_MANY_VERIFICATIONS;

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
      TOO_MANY_ONLINE_PER_IP = PreLoginEvent.PreLoginComponentResult.denied(
        Component.text(Sonar.get().getConfig().TOO_MANY_ONLINE_PER_IP)
      );
      TOO_MANY_VERIFICATIONS = Component.text(Sonar.get().getConfig().TOO_MANY_VERIFICATIONS);
    }
  }

  private static final DummyConnection CLOSED_MINECRAFT_CONNECTION;

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final MethodHandle INITIAL_CONNECTION;
  private static final MethodHandle CONNECTED_PLAYER;
  public static final Field CONNECTION_FIELD;

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
        .findGetter(LoginInboundConnection.class,
          "delegate",
          InitialInboundConnection.class
        );
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  /**
   * Pre-login
   *
   * @param event PreLoginEvent
   */
  @Subscribe(order = PostOrder.LAST)
  public void handle(final PreLoginEvent event) {
    fallback.getSonar().getStatistics().increment("total");

    final var inetAddress = event.getConnection().getRemoteAddress().getAddress();

    if (fallback.getBlacklisted().contains(inetAddress)) {
      event.setResult(BLACKLISTED);
      return;
    }

    // Check if the amount of online players using the same ip address as
    // the connecting player is greater than the configured amount
    final int maxOnlinePerIp = fallback.getSonar().getConfig().MAXIMUM_ONLINE_PER_IP;

    if (maxOnlinePerIp > 0) {
      final long onlinePerIp = SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
        // We have to do this since it's 2 different instances...
        .filter(player -> Objects.equals(player.getRemoteAddress().getAddress(), inetAddress))
        .count();

      // We use '>=' because the player connecting to the server hasn't joined yet
      if (onlinePerIp >= maxOnlinePerIp) {
        event.setResult(TOO_MANY_ONLINE_PER_IP);
        return;
      }
    }

    if (fallback.getVerified().contains(inetAddress)) return;
    if (!fallback.getSonar().getConfig().ENABLE_VERIFICATION) return;

    // Check if Fallback is already verifying a player
    // → is another player with the same ip address connected to Fallback?
    if (fallback.getConnected().contains(inetAddress)) {
      event.setResult(ALREADY_VERIFYING);
      return;
    }

    // We cannot allow too many players on our Fallback server
    if (fallback.getQueue().getQueuedPlayers().size() > fallback.getSonar().getConfig().MAXIMUM_QUEUED_PLAYERS
      || fallback.getConnected().size() > fallback.getSonar().getConfig().MAXIMUM_VERIFYING_PLAYERS) {
      event.setResult(TOO_MANY_PLAYERS);
      return;
    }

    /*
     * If we don't handle online/offline mode players correctly,
     * many plugins (especially Auth-related) will have issues
     */
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
    if (!fallback.getSonar().getConfig().ENABLE_VERIFICATION) return;

    final var inetAddress = event.getConnection().getRemoteAddress().getAddress();

    // We don't want to check players that have already been verified
    if (fallback.getVerified().contains(inetAddress)) return;

    final var inboundConnection = (LoginInboundConnection) event.getConnection();
    final var initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

    final MinecraftConnection mcConnection = initialConnection.getConnection();
    final Channel channel = mcConnection.getChannel();

    // The AuthSessionHandler isn't supposed to continue the connection process
    // which is why we override the field value for the MinecraftConnection with
    // a dummy connection
    CONNECTION_FIELD.set(mcConnection.getSessionHandler(), CLOSED_MINECRAFT_CONNECTION);

    // Run in the channel's event loop
    channel.eventLoop().execute(() -> {
      if (mcConnection.isClosed()) return;

      final ChannelPipeline pipeline = channel.pipeline();

      // Replace timeout handler to avoid known exploits or issues
      // We also want to timeout bots quickly to avoid flooding
      pipeline.replace(
        READ_TIMEOUT,
        READ_TIMEOUT,
        new FallbackTimeoutHandler(
          fallback.getSonar().getConfig().VERIFICATION_TIMEOUT,
          TimeUnit.MILLISECONDS
        )
      );

      // We have to add this pipeline to monitor whenever the client disconnects
      // to remove them from the list of connected and queued players
      pipeline.addFirst(HANDLER, FallbackChannelHandler.INSTANCE);

      // Queue the connection for further processing
      fallback.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {
        if (mcConnection.isClosed()) return;

        final boolean isPremium = premium.contains(event.getUsername());

        // Remove the player from the premium list in order to prevent memory leaks
        // We cannot rely on the DisconnectEvent since the server will not call it
        // -> we are intercepting the packets!
        premium.remove(event.getUsername());

        // Create an instance for the connected player
        final ConnectedPlayer player;
        try {
          player = (ConnectedPlayer) CONNECTED_PLAYER.invokeExact(
            mcConnection.server,
            event.getGameProfile(),
            mcConnection,
            inboundConnection.getVirtualHost().orElse(null),
            isPremium,
            inboundConnection.getIdentifiedKey()
          );
        } catch (Throwable throwable) {
          fallback.getLogger().error("Error while processing {}: {}", event.getUsername(), throwable);
          mcConnection.close(true);
          return;
        }

        // Check if the ip address had too many verifications or is rejoining too quickly
        if (!fallback.getAttemptLimiter().attempt(inetAddress)) {
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
        final FallbackPlayer fallbackPlayer = new FallbackPlayer(
          fallback,
          player, mcConnection, channel, pipeline, inetAddress,
          player.getProtocolVersion().getProtocol()
        );

        // ==================================================================
        if (!fallback.isUnderAttack() || fallback.getSonar().getConfig().LOG_DURING_ATTACK) {
          fallback.getLogger().info("Processing: {}{} ({})",
            event.getUsername(), inetAddress, fallbackPlayer.getProtocolVersion());
        }

        fallback.getConnected().add(inetAddress);
        // ==================================================================

        // Set compression
        if (fallback.getSonar().getConfig().ENABLE_COMPRESSION) {
          final int threshold = mcConnection.server.getConfiguration().getCompressionThreshold();

          if (threshold >= 0 && mcConnection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
            mcConnection.write(new SetCompression(threshold));
            mcConnection.setCompressionThreshold(threshold);
          }
        }

        // Send LoginSuccess packet to spoof our fake lobby
        final ServerLoginSuccess success = new ServerLoginSuccess();

        success.setUsername(player.getUsername());
        success.setProperties(player.getGameProfileProperties());
        success.setUuid(player.getUniqueId());

        mcConnection.write(success);

        // Set the state to a custom one, so we can receive and send more packets
        mcConnection.setAssociation(player);
        mcConnection.setState(StateRegistry.PLAY);

        final long keepAliveId = ThreadLocalRandom.current().nextInt();

        // We have to add this pipeline to monitor all incoming traffic
        // We add the pipeline after the MinecraftDecoder since we want
        // the packets to be processed and decoded already
        fallbackPlayer.getPipeline().addAfter(
          MINECRAFT_DECODER,
          DECODER,
          new FallbackPacketDecoder(fallbackPlayer, keepAliveId)
        );

        if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
          // ==================================================================
          // The first step of the verification is a simple KeepAlive packet
          // We don't want to waste resources by directly sending all packets to
          // the client which is why we first send a KeepAlive packet and then
          // wait for a valid response to continue the verification process
          final KeepAlive keepAlive = new KeepAlive();

          keepAlive.setRandomId(keepAliveId);

          mcConnection.write(keepAlive);
          // ==================================================================
        } else {
          // ==================================================================
          // KeepAlive packets do not exist during the login process on 1.7
          // We have to fall back to the regular method of verification
          mcConnection.delayedWrite(LEGACY_JOIN_GAME);

          // Set session handler to custom fallback handler to intercept all incoming packets
          mcConnection.setSessionHandler(new FallbackSessionHandler(
            mcConnection.getSessionHandler(), fallbackPlayer
          ));

          mcConnection.flush();
          // ==================================================================
        }
      }));
    });
  }
}
