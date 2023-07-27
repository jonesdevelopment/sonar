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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.common.exception.ReflectionException;
import xyz.jonesdev.sonar.common.fallback.FallbackChannelHandler;
import xyz.jonesdev.sonar.common.fallback.FallbackTimeoutHandler;
import xyz.jonesdev.sonar.common.geyser.GeyserValidator;
import xyz.jonesdev.sonar.velocity.SonarVelocity;
import xyz.jonesdev.sonar.velocity.fallback.dummy.DummyConnection;
import xyz.jonesdev.sonar.velocity.fallback.handler.FallbackLoginHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_HANDLER;
import static xyz.jonesdev.sonar.velocity.fallback.FallbackListener.CachedMessages.*;

@RequiredArgsConstructor
public final class FallbackListener {
  private final @NotNull Fallback fallback;

  public static class CachedMessages {
    static Component LOCKDOWN_DISCONNECT;
    static Component TOO_MANY_ONLINE_PER_IP;
    static Component TOO_MANY_PLAYERS;
    static Component BLACKLISTED;
    static Component ALREADY_VERIFYING;
    static Component ALREADY_QUEUED;
    static Component TOO_FAST_RECONNECT;
    public static Component UNEXPECTED_ERROR;
    public static Component VERIFICATION_FAILED;
    public static Component INVALID_USERNAME;
    public static Component VERIFICATION_SUCCESS;

    public static void update() {
      ALREADY_VERIFYING = Component.text(Sonar.get().getConfig().ALREADY_VERIFYING);
      ALREADY_QUEUED = Component.text(Sonar.get().getConfig().ALREADY_QUEUED);
      TOO_MANY_PLAYERS = Component.text(Sonar.get().getConfig().TOO_MANY_PLAYERS);
      BLACKLISTED = Component.text(Sonar.get().getConfig().BLACKLISTED);
      TOO_FAST_RECONNECT = Component.text(Sonar.get().getConfig().TOO_FAST_RECONNECT);
      UNEXPECTED_ERROR = Component.text(Sonar.get().getConfig().UNEXPECTED_ERROR);
      INVALID_USERNAME = Component.text(Sonar.get().getConfig().INVALID_USERNAME);
      VERIFICATION_SUCCESS = Component.text(Sonar.get().getConfig().VERIFICATION_SUCCESS);
      VERIFICATION_FAILED = Component.text(Sonar.get().getConfig().VERIFICATION_FAILED);
      TOO_MANY_ONLINE_PER_IP = Component.text(Sonar.get().getConfig().TOO_MANY_ONLINE_PER_IP);
      LOCKDOWN_DISCONNECT = Component.text(Sonar.get().getConfig().LOCKDOWN_DISCONNECT);
    }
  }

  private static final DummyConnection CLOSED_MINECRAFT_CONNECTION;

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final MethodHandle INITIAL_CONNECTION;
  private static final Field CONNECTION_FIELD;

  static {
    CLOSED_MINECRAFT_CONNECTION = new DummyConnection(null);

    try {
      CONNECTION_FIELD = InitialLoginSessionHandler.class.getDeclaredField("mcConnection");
      CONNECTION_FIELD.setAccessible(true);

      INITIAL_CONNECTION = MethodHandles.privateLookupIn(LoginInboundConnection.class, LOOKUP)
        .findGetter(LoginInboundConnection.class,
          "delegate",
          InitialInboundConnection.class
        );
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  public void handle(final PreLoginEvent event) throws Throwable {
    fallback.getSonar().getStatistics().increment("total");

    final InetAddress inetAddress = event.getConnection().getRemoteAddress().getAddress();

    val inboundConnection = (LoginInboundConnection) event.getConnection();
    val initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

    if (fallback.getBlacklisted().has(inetAddress.toString())) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        BLACKLISTED,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    if (fallback.getVerified().contains(inetAddress.toString())) return;
    if (!fallback.getSonar().getConfig().ENABLE_VERIFICATION) return;

    // Check if Fallback is already verifying a player
    // → is another player with the same IP address connected to Fallback?
    if (fallback.getConnected().containsKey(event.getUsername())
      || fallback.getConnected().containsValue(inetAddress)) {
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

    final MinecraftConnection mcConnection = initialConnection.getConnection();
    final Channel channel = mcConnection.getChannel();

    // Completely skip Geyser connections
    // TODO: different handling?
    if (GeyserValidator.isGeyser(channel)) {
      // TODO: Do we need to log this?
      fallback.getLogger().info("Allowing Geyser connection: " + inetAddress);
      return;
    }

    final InitialLoginSessionHandler sessionHandler = (InitialLoginSessionHandler) mcConnection.getSessionHandler();

    // The AuthSessionHandler isn't supposed to continue the connection process,
    // which is why we override the field value for the MinecraftConnection with
    // a fake connection
    CONNECTION_FIELD.set(sessionHandler, CLOSED_MINECRAFT_CONNECTION);

    // If we don't handle online/offline mode players correctly,
    // many plugins (especially Auth-related) will have issues
    //
    // We need to determine if the player is premium before we queue the connection,
    // and before we run everything in the event loop to avoid potential memory leaks
    final boolean isPremium = !event.getResult().isForceOfflineMode()
      && (SonarVelocity.INSTANCE.getPlugin().getServer().getConfiguration().isOnlineMode()
      || event.getResult().isOnlineModeAllowed());

    // Run in the channel's event loop
    channel.eventLoop().execute(() -> {

      // Do not continue if the connection is closed or marked as disconnected
      if (mcConnection.isClosed() || mcConnection.isKnownDisconnect()) return;

      final ChannelPipeline pipeline = channel.pipeline();

      // We have to add this pipeline to monitor whenever the client disconnects
      // to remove them from the list of connected and queued players
      pipeline.addFirst(FALLBACK_HANDLER, new FallbackChannelHandler(event.getUsername()));

      // Queue the connection for further processing
      fallback.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {

        // Do not continue if the connection is closed or marked as disconnected
        if (mcConnection.isClosed() || mcConnection.isKnownDisconnect()) return;

        // Check if the username matches the valid name regex in order to prevent
        // UTF-16 names or other types of flood attacks
        if (!fallback.getSonar().getConfig().VALID_NAME_REGEX
          .matcher(event.getUsername()).matches()) {
          mcConnection.closeWith(Disconnect.create(INVALID_USERNAME, mcConnection.getProtocolVersion()));
          return;
        }

        // Add better timeout handler to avoid known exploits or issues
        // We also want to timeout bots quickly to avoid flooding
        pipeline.replace(
          READ_TIMEOUT,
          READ_TIMEOUT,
          new FallbackTimeoutHandler(
            fallback.getSonar().getConfig().VERIFICATION_READ_TIMEOUT,
            TimeUnit.MILLISECONDS
          )
        );

        mcConnection.setSessionHandler(new FallbackLoginHandler(
          fallback, mcConnection, inboundConnection, sessionHandler,
          event.getUsername(), inetAddress, isPremium
        ));
      }));
    });
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final LoginEvent event) {
    val connectedPlayer = (ConnectedPlayer) event.getPlayer();

    if (fallback.getSonar().getConfig().LOCKDOWN_ENABLED) {
      if (!event.getPlayer().hasPermission("sonar.lockdown")) {
        connectedPlayer.getConnection().closeWith(Disconnect.create(
          LOCKDOWN_DISCONNECT, connectedPlayer.getProtocolVersion()
        ));

        if (fallback.getSonar().getConfig().LOCKDOWN_LOG_ATTEMPTS) {
          fallback.getSonar().getLogger().info(
            fallback.getSonar().getConfig().LOCKDOWN_CONSOLE_LOG
              .replace("%player%", event.getPlayer().getUsername())
              .replace("%ip%", event.getPlayer().getRemoteAddress().getAddress().toString())
              .replace("%protocol%",
                String.valueOf(event.getPlayer().getProtocolVersion().getProtocol()))
          );
        }
        return;
      } else if (fallback.getSonar().getConfig().LOCKDOWN_ENABLE_NOTIFY) {
        event.getPlayer().sendMessage(
          Component.text(fallback.getSonar().getConfig().LOCKDOWN_NOTIFICATION)
        );
      }
    }

    final InetAddress inetAddress = event.getPlayer().getRemoteAddress().getAddress();

    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final int maxOnlinePerIp = fallback.getSonar().getConfig().MAXIMUM_ONLINE_PER_IP;

    if (maxOnlinePerIp > 0) {
      final long onlinePerIp = SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
        .filter(player -> Objects.equals(player.getRemoteAddress().getAddress(), inetAddress))
        .count();

      // We use '>=' because the player connecting to the server hasn't joined yet
      if (onlinePerIp >= maxOnlinePerIp) {
        connectedPlayer.getConnection().closeWith(Disconnect.create(
          TOO_MANY_ONLINE_PER_IP, connectedPlayer.getProtocolVersion()
        ));
      }
    }
  }
}
