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

package jones.sonar.velocity.fallback;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
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
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import jones.sonar.api.Sonar;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.common.fallback.FallbackChannelHandler;
import jones.sonar.common.fallback.FallbackTimeoutHandler;
import jones.sonar.common.geyser.GeyserValidator;
import jones.sonar.velocity.SonarVelocity;
import jones.sonar.velocity.fallback.session.FallbackPlayer;
import jones.sonar.velocity.fallback.session.FallbackSessionHandler;
import jones.sonar.velocity.fallback.session.dummy.DummyConnection;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.InetAddress;
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
  private final @NotNull Fallback fallback;

  // We need to cache if the joining player is a premium player or not
  // If we don't do that, many authentication plugins can potentially break
  private final Collection<String> premium = new Vector<>(1);

  public static class CachedMessages {
    static LoginEvent.ComponentResult LOCKDOWN_DISCONNECT;
    static Component TOO_MANY_PLAYERS;
    static Component BLACKLISTED;
    static Component ALREADY_VERIFYING;
    static Component ALREADY_QUEUED;
    static Component TOO_MANY_ONLINE_PER_IP;
    static Component TOO_FAST_RECONNECT;
    public static Component UNEXPECTED_ERROR;
    public static Component INVALID_USERNAME;

    public static void update() {
      ALREADY_VERIFYING = Component.text(Sonar.get().getConfig().ALREADY_VERIFYING);
      ALREADY_QUEUED = Component.text(Sonar.get().getConfig().ALREADY_QUEUED);
      TOO_MANY_PLAYERS = Component.text(Sonar.get().getConfig().TOO_MANY_PLAYERS);
      BLACKLISTED = Component.text(Sonar.get().getConfig().BLACKLISTED);
      TOO_MANY_ONLINE_PER_IP = Component.text(Sonar.get().getConfig().TOO_MANY_ONLINE_PER_IP);
      TOO_FAST_RECONNECT = Component.text(Sonar.get().getConfig().TOO_FAST_RECONNECT);
      LOCKDOWN_DISCONNECT = LoginEvent.ComponentResult.denied(
        Component.text(Sonar.get().getConfig().LOCKDOWN_DISCONNECT
      ));
      UNEXPECTED_ERROR = Component.text(Sonar.get().getConfig().UNEXPECTED_ERROR);
      INVALID_USERNAME = Component.text(Sonar.get().getConfig().INVALID_USERNAME);
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
  public void handle(final PreLoginEvent event) throws Throwable {
    fallback.getSonar().getStatistics().increment("total");

    final InetAddress inetAddress = event.getConnection().getRemoteAddress().getAddress();

    val inboundConnection = (LoginInboundConnection) event.getConnection();
    val initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

    if (fallback.getBlacklisted().contains(inetAddress.toString())) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        BLACKLISTED,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final int maxOnlinePerIp = fallback.getSonar().getConfig().MAXIMUM_ONLINE_PER_IP;

    if (maxOnlinePerIp > 0) {
      final long onlinePerIp = SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
        .filter(player -> Objects.equals(player.getRemoteAddress().getAddress(), inetAddress))
        .count();

      // We use '>=' because the player connecting to the server hasn't joined yet
      if (onlinePerIp >= maxOnlinePerIp) {
        initialConnection.getConnection().closeWith(Disconnect.create(
          TOO_MANY_ONLINE_PER_IP,
          inboundConnection.getProtocolVersion()
        ));
        return;
      }
    }

    if (fallback.getVerified().contains(inetAddress.toString())) return;
    if (!fallback.getSonar().getConfig().ENABLE_VERIFICATION) return;

    // Check if Fallback is already verifying a player
    // → is another player with the same IP address connected to Fallback?
    if (fallback.getConnected().contains(inetAddress.toString())) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        ALREADY_VERIFYING,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // We cannot allow too many players on our Fallback server
    if (fallback.getQueue().getQueuedPlayers().size() > fallback.getSonar().getConfig().MAXIMUM_QUEUED_PLAYERS
      || fallback.getConnected().size() > fallback.getSonar().getConfig().MAXIMUM_VERIFYING_PLAYERS) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        TOO_MANY_PLAYERS,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // Check if the IP address is reconnecting too quickly while being unverified
    if (!fallback.getAttemptLimiter().attempt(inetAddress)) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        TOO_FAST_RECONNECT,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // Check if the player is already queued since we don't want bots to flood the queue
    // TODO: do some performance testing
    if (fallback.getQueue().getQueuedPlayers().stream()
      .anyMatch(pair -> Objects.equals(pair.getFirst(), inetAddress))) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        ALREADY_QUEUED,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    /*
     * If we don't handle online/offline mode players correctly,
     * many plugins (especially Auth-related) will have issues
     */
    if (event.getResult().isForceOfflineMode()) return;
    if (!SonarVelocity.INSTANCE.getPlugin().getServer().getConfiguration().isOnlineMode()
      && !event.getResult().isOnlineModeAllowed()) return;

    // TODO: test with /premium
    premium.add(event.getUsername());
  }

  /**
   * Handles lockdown mode
   *
   * @param event LoginEvent
   */
  @Subscribe(order = PostOrder.LAST)
  public void handle(final LoginEvent event) {
    if (!fallback.getSonar().getConfig().LOCKDOWN_ENABLED) return;

    if (!event.getPlayer().hasPermission("sonar.lockdown")) {
      event.setResult(LOCKDOWN_DISCONNECT);
      if (fallback.getSonar().getConfig().LOCKDOWN_LOG_ATTEMPTS) {
        fallback.getSonar().getLogger().info(
          fallback.getSonar().getConfig().LOCKDOWN_CONSOLE_LOG
            .replace("%player%", event.getPlayer().getUsername())
            .replace("%ip%", event.getPlayer().getRemoteAddress().getAddress().toString())
            .replace("%protocol%",
              String.valueOf(event.getPlayer().getProtocolVersion().getProtocol()))
        );
      }
    } else if (fallback.getSonar().getConfig().LOCKDOWN_ENABLE_NOTIFY) {
      event.getPlayer().sendMessage(
        Component.text(fallback.getSonar().getConfig().LOCKDOWN_NOTIFICATION)
      );
    }
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

    final InetAddress inetAddress = event.getConnection().getRemoteAddress().getAddress();

    // We don't want to check players that have already been verified
    if (fallback.getVerified().contains(inetAddress.toString())) return;

    if (event.getConnection() instanceof InitialInboundConnection) {
      fallback.getLogger().error("Could not inject into GameProfileRequestEvent!");
      fallback.getLogger().error("Make sure to remove any other plugin that interferes with this event!");
      return;
    }

    val inboundConnection = (LoginInboundConnection) event.getConnection();
    val initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

    final MinecraftConnection mcConnection = initialConnection.getConnection();
    final Channel channel = mcConnection.getChannel();

    // Completely skip Geyser connections
    // TODO: different handling?
    if (GeyserValidator.isGeyser(channel)) {
      // TODO: Do we need to log this?
      fallback.getLogger().info("Allowing Geyser connection: " + inetAddress);
      return;
    }

    // The AuthSessionHandler isn't supposed to continue the connection process,
    // which is why we override the field value for the MinecraftConnection with
    // a fake connection
    CONNECTION_FIELD.set(mcConnection.getSessionHandler(), CLOSED_MINECRAFT_CONNECTION);

    // We need to determine if the player is premium before we queue the connection,
    // and before we run everything in the event loop to avoid potential memory leaks
    final boolean isPremium = premium.contains(event.getUsername());

    // Remove the player from the premium list in order to prevent memory leaks
    // We cannot rely on the DisconnectEvent since the server will not call it
    // -> we are intercepting the packets!
    premium.remove(event.getUsername());

    // Run in the channel's event loop
    channel.eventLoop().execute(() -> {

      // Do not continue if the connection is closed
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

        // Do not continue if the connection is closed
        if (mcConnection.isClosed()) return;

        // Check if the username matches the valid name regex in order to prevent
        // UTF-16 names or other types of flood attacks
        if (!fallback.getSonar().getConfig().VALID_NAME_REGEX
          .matcher(event.getUsername()).matches()) {
          mcConnection.closeWith(Disconnect.create(INVALID_USERNAME, mcConnection.getProtocolVersion()));
          return;
        }

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
          // This should not happen
          fallback.getLogger().error("Error while processing {}: {}", event.getUsername(), throwable);
          mcConnection.close(true);
          return;
        }

        // Check if the player is already connected to the proxy
        // We use the default Velocity method for this to avoid incompatibilities
        if (!mcConnection.server.canRegisterConnection(player)) {
          mcConnection.closeWith(Disconnect.create(
            Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED),
            mcConnection.getProtocolVersion()
          ));
          return;
        }

        // Create an instance for the Fallback connection
        final FallbackPlayer fallbackPlayer = new FallbackPlayer(
          fallback,
          player, mcConnection, channel, pipeline, inetAddress,
          player.getProtocolVersion().getProtocol()
        );

        if (fallback.getSonar().getConfig().LOG_CONNECTIONS) {
          // Only log the processing message if the server isn't under attack.
          // We let the user override this through the configuration.
          if (!fallback.isUnderAttack() || fallback.getSonar().getConfig().LOG_DURING_ATTACK) {
            fallback.getLogger().info("Processing: {}{} ({})",
              event.getUsername(), inetAddress, fallbackPlayer.getProtocolVersion());
          }
        }

        // Mark the player as connected → verifying players
        fallback.getConnected().add(inetAddress.toString());

        // Check if compression is enabled in the Sonar configuration
        if (fallback.getSonar().getConfig().ENABLE_COMPRESSION) {
          final int threshold = mcConnection.server.getConfiguration().getCompressionThreshold();

          // Set compression
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
          new FallbackPacketDecoder(
            fallbackPlayer,
            keepAliveId
          )
        );

        // ==================================================================
        if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
          // The first step of the verification is a simple KeepAlive packet
          // We don't want to waste resources by directly sending all packets to
          // the client, which is why we first send a KeepAlive packet and then
          // wait for a valid response to continue the verification process
          final KeepAlive keepAlive = new KeepAlive();

          keepAlive.setRandomId(keepAliveId);

          mcConnection.write(keepAlive);
        } else {
          // KeepAlive packets do not exist during the login process on 1.7.
          // We have to fall back to the regular method of verification

          // Set session handler to custom fallback handler to intercept all incoming packets
          mcConnection.setSessionHandler(new FallbackSessionHandler(
            mcConnection.getSessionHandler(), fallbackPlayer
          ));

          // Send JoinGame packet
          mcConnection.write(LEGACY_JOIN_GAME);
        }
        // ==================================================================
      }));
    });
  }
}
