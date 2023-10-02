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
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
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
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.common.fallback.FallbackChannelHandler;
import xyz.jonesdev.sonar.common.fallback.FallbackTimeoutHandler;
import xyz.jonesdev.sonar.common.fallback.traffic.TrafficChannelHooker;
import xyz.jonesdev.sonar.velocity.SonarVelocity;
import xyz.jonesdev.sonar.velocity.fallback.dummy.DummyConnection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.network.Connections.*;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_HANDLER;
import static xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil.isGeyserConnection;

@RequiredArgsConstructor
public final class FallbackListener {
  private final @NotNull Fallback fallback;

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
      throw new ReflectiveOperationException(throwable);
    }
  }

  private static void markConnectionAsDead(final MinecraftSessionHandler sessionHandler) throws Throwable {
    // The AuthSessionHandler isn't supposed to continue the connection process,
    // which is why we override the field value for the MinecraftConnection with
    // a fake connection.
    CONNECTION_FIELD.set(sessionHandler, CLOSED_MINECRAFT_CONNECTION);
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull PreLoginEvent event) throws Throwable {
    Statistics.TOTAL_TRAFFIC.increment();

    final InetAddress inetAddress = event.getConnection().getRemoteAddress().getAddress();

    val inboundConnection = (LoginInboundConnection) event.getConnection();
    val initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

    final MinecraftConnection mcConnection = initialConnection.getConnection();
    final Channel channel = mcConnection.getChannel();
    final ChannelPipeline pipeline = channel.pipeline();

    // Hook the custom traffic pipeline, so we can count the incoming and outgoing traffic
    TrafficChannelHooker.hook(pipeline, MINECRAFT_DECODER, MINECRAFT_ENCODER);

    // Check the blacklist here since we cannot let the player "ghost join"
    if (fallback.getBlacklisted().has(inetAddress.toString())) {
      markConnectionAsDead(mcConnection.getSessionHandler());
      initialConnection.getConnection().closeWith(Disconnect.create(
        Sonar.get().getConfig().BLACKLISTED,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // Don't continue the verification process if the verification is disabled
    if (!Sonar.get().getConfig().ENABLE_VERIFICATION) return;

    // Check if the player is already verified.
    // No one wants to be verified over and over again.
    final GameProfile gameProfile = GameProfile.forOfflinePlayer(event.getUsername());
    if (Sonar.get().getVerifiedPlayerController().has(inetAddress, gameProfile.getId())) return;

    // Completely skip Geyser connections (for now)
    if (isGeyserConnection(channel)) {
      // TODO: Do we need to log this?
      fallback.getLogger().info("Allowing Geyser connection: " + inetAddress);
      return;
    }

    // We now mark the connection as dead by using our dummy connection
    markConnectionAsDead(mcConnection.getSessionHandler());

    // Check if Fallback is already verifying a player
    // → is another player with the same IP address connected to Fallback?
    if (fallback.getConnected().containsKey(event.getUsername())
      || fallback.getConnected().containsValue(inetAddress)) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        Sonar.get().getConfig().ALREADY_VERIFYING,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // We cannot allow too many players on our Fallback server
    // There's technically no reason for limiting this, but we'll better stay safe.
    if (fallback.getConnected().size() > Sonar.get().getConfig().MAXIMUM_VERIFYING_PLAYERS) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        Sonar.get().getConfig().TOO_MANY_PLAYERS,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // Check if the IP address is currently being rate-limited
    if (!fallback.getRatelimiter().attempt(inetAddress)) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        Sonar.get().getConfig().TOO_FAST_RECONNECT,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // Check if the player is already queued since we don't want bots to flood the queue
    if (fallback.getQueue().getQueuedPlayers().containsKey(inetAddress)) {
      initialConnection.getConnection().closeWith(Disconnect.create(
        Sonar.get().getConfig().ALREADY_QUEUED,
        inboundConnection.getProtocolVersion()
      ));
      return;
    }

    // Run in the channel's event loop
    channel.eventLoop().execute(() -> {

      // Do not continue if the connection is closed or marked as disconnected
      if (mcConnection.isClosed() || mcConnection.isKnownDisconnect()) return;

      // We have to add this pipeline to monitor whenever the client disconnects
      // to remove them from the list of connected and queued players
      pipeline.addFirst(FALLBACK_HANDLER, new FallbackChannelHandler(event.getUsername()));

      // Queue the connection for further processing
      fallback.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {

        // Do not continue if the connection is closed or marked as disconnected
        if (mcConnection.isClosed() || mcConnection.isKnownDisconnect()) return;

        // Check if the username matches the valid name regex in order to prevent
        // UTF-16 names or other types of flood attacks
        if (!Sonar.get().getConfig().VALID_NAME_REGEX
          .matcher(event.getUsername()).matches()) {
          mcConnection.closeWith(Disconnect.create(Sonar.get().getConfig().INVALID_USERNAME, mcConnection.getProtocolVersion()));
          return;
        }

        // Add better timeout handler to avoid known exploits or issues
        // We also want to timeout bots quickly to avoid flooding
        pipeline.replace(
          READ_TIMEOUT,
          READ_TIMEOUT,
          new FallbackTimeoutHandler(
            Sonar.get().getConfig().VERIFICATION_READ_TIMEOUT,
            TimeUnit.MILLISECONDS
          )
        );

        // We need to determine if the player is premium before we handle the connection,
        // so we can create a ConnectedPlayer instance without having to spoof this
        final boolean onlineMode = !event.getResult().isForceOfflineMode()
          && (SonarVelocity.INSTANCE.getPlugin().getServer().getConfiguration().isOnlineMode()
          || event.getResult().isOnlineModeAllowed());

        // Replace the session handler to intercept all packets and handle them
        mcConnection.setSessionHandler(new FallbackSessionHandler(
          fallback, mcConnection, inboundConnection,
          gameProfile, inetAddress, onlineMode
        ));
      }));
    });
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull LoginEvent event) {
    val connectedPlayer = (ConnectedPlayer) event.getPlayer();

    if (Sonar.get().getConfig().LOCKDOWN_ENABLED) {
      if (!event.getPlayer().hasPermission("sonar.lockdown")) {
        connectedPlayer.getConnection().closeWith(Disconnect.create(
          Sonar.get().getConfig().LOCKDOWN_DISCONNECT, connectedPlayer.getProtocolVersion()
        ));

        if (Sonar.get().getConfig().LOCKDOWN_LOG_ATTEMPTS) {
          Sonar.get().getLogger().info(
            Sonar.get().getConfig().LOCKDOWN_CONSOLE_LOG
              .replace("%player%", event.getPlayer().getUsername())
              .replace("%ip%", Sonar.get().getConfig()
                .formatAddress(event.getPlayer().getRemoteAddress().getAddress()))
              .replace("%protocol%",
                String.valueOf(event.getPlayer().getProtocolVersion().getProtocol()))
          );
        }
        return;
      } else if (Sonar.get().getConfig().LOCKDOWN_ENABLE_NOTIFY) {
        event.getPlayer().sendMessage(
          Component.text(Sonar.get().getConfig().LOCKDOWN_NOTIFICATION)
        );
      }
    }

    final InetAddress inetAddress = event.getPlayer().getRemoteAddress().getAddress();

    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final int maxOnlinePerIp = Sonar.get().getConfig().MAXIMUM_ONLINE_PER_IP;

    if (maxOnlinePerIp > 0) {
      final long onlinePerIp = SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
        .filter(player -> Objects.equals(player.getRemoteAddress().getAddress(), inetAddress))
        .count();

      // We use '>=' because the player connecting to the server hasn't joined yet
      if (onlinePerIp >= maxOnlinePerIp) {
        connectedPlayer.getConnection().closeWith(Disconnect.create(
          Sonar.get().getConfig().TOO_MANY_ONLINE_PER_IP, connectedPlayer.getProtocolVersion()
        ));
      }
    }
  }
}
