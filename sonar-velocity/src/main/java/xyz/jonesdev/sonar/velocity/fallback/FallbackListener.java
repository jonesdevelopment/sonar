/*
 * Copyright (C) 2023-2024 Sonar Contributors
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
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyJoinEvent;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.statistics.Counters;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.common.fallback.FallbackChannelHandler;
import xyz.jonesdev.sonar.common.fallback.FallbackTimeoutHandler;
import xyz.jonesdev.sonar.common.fallback.FallbackVerificationHandler;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginSuccess;
import xyz.jonesdev.sonar.common.fallback.traffic.TrafficChannelHooker;
import xyz.jonesdev.sonar.velocity.SonarVelocity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.network.Connections.*;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil.isGeyserConnection;

public final class FallbackListener {
  private static final Fallback FALLBACK = Sonar.get().getFallback();

  private static final FallbackClosedConnection CLOSED_CONNECTION;

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final MethodHandle INITIAL_CONNECTION;
  private static final Field CONNECTION_FIELD;

  static {
    CLOSED_CONNECTION = new FallbackClosedConnection();

    try {
      CONNECTION_FIELD = InitialLoginSessionHandler.class.getDeclaredField("mcConnection");
      CONNECTION_FIELD.setAccessible(true);

      INITIAL_CONNECTION = MethodHandles.privateLookupIn(LoginInboundConnection.class, LOOKUP)
        .findGetter(LoginInboundConnection.class,
          "delegate",
          InitialInboundConnection.class);
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }
  }

  private static void markConnectionAsDead(final MinecraftSessionHandler sessionHandler) throws Throwable {
    // The AuthSessionHandler isn't supposed to continue the connection process,
    // which is why we override the field value for the MinecraftConnection with
    // a fake (closed) connection.
    CONNECTION_FIELD.set(sessionHandler, CLOSED_CONNECTION);
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull ConnectionHandshakeEvent event) throws Throwable {
    // Increase connections per second for the action bar verbose
    Counters.CONNECTIONS_PER_SECOND.put(System.nanoTime());
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull PreLoginEvent event) throws Throwable {
    // Increase joins per second for the action bar verbose
    Counters.LOGINS_PER_SECOND.put(System.nanoTime());

    final LoginInboundConnection inboundConnection = (LoginInboundConnection) event.getConnection();
    val initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

    final MinecraftConnection mcConnection = initialConnection.getConnection();
    final Channel channel = mcConnection.getChannel();

    // Run in the channel's event loop
    channel.eventLoop().execute(() -> {

      // Do not continue if the connection is closed or marked as disconnected
      if (mcConnection.isClosed() || mcConnection.isKnownDisconnect()) return;

      // Hook the custom traffic pipeline, so we can count the incoming and outgoing traffic
      final ChannelPipeline pipeline = channel.pipeline();
      TrafficChannelHooker.hook(pipeline, MINECRAFT_DECODER, MINECRAFT_ENCODER);

      final InetAddress inetAddress = event.getConnection().getRemoteAddress().getAddress();

      // Increase total traffic statistic
      Statistics.TOTAL_TRAFFIC.increment();

      try {
        val activeSessionHandler = mcConnection.getActiveSessionHandler();

        // Check the blacklist here since we cannot let the player "ghost join"
        if (FALLBACK.getBlacklist().has(inetAddress)) {
          // Mark the connection as dead to avoid unnecessary console logs
          markConnectionAsDead(activeSessionHandler);
          mcConnection.closeWith(DisconnectPacket.create(
            Sonar.get().getConfig().getVerification().getBlacklisted(),
            mcConnection.getProtocolVersion(), true));
          return;
        }

        // Don't continue the verification process if the verification is disabled
        if (!Sonar.get().getFallback().shouldVerifyNewPlayers()) return;

        // Completely skip Geyser connections
        if (isGeyserConnection(channel)) {
          FALLBACK.getLogger().info("Skipping Geyser player: {}{}",
            event.getUsername(), Sonar.get().getConfig().formatAddress(inetAddress));
          return;
        }

        // Check if the protocol ID of the player is not allowed to enter the server
        final int protocolId = mcConnection.getProtocolVersion().getProtocol();
        if (Sonar.get().getConfig().getVerification().getBlacklistedProtocols().contains(protocolId)) {
          // Mark the connection as dead to avoid unnecessary console logs
          markConnectionAsDead(activeSessionHandler);
          mcConnection.closeWith(DisconnectPacket.create(
            Sonar.get().getConfig().getVerification().getProtocolBlacklisted(),
            mcConnection.getProtocolVersion(), true));
          return;
        }

        // Check if the player is already verified.
        // No one wants to be verified over and over again.
        final GameProfile gameProfile = GameProfile.forOfflinePlayer(event.getUsername());
        if (Sonar.get().getVerifiedPlayerController().has(inetAddress, gameProfile.getId())) return;

        // Check if the protocol ID of the player is allowed to bypass verification
        if (Sonar.get().getConfig().getVerification().getWhitelistedProtocols().contains(protocolId)) return;

        // We now mark the connection as dead by using our fake connection
        markConnectionAsDead(activeSessionHandler);
        // Don't allow exceptions or disconnect messages
        mcConnection.setAssociation(null);

        // Check if the player is already queued since we don't want bots to flood the queue
        if (FALLBACK.getQueue().getQueuedPlayers().containsKey(inetAddress)) {
          mcConnection.closeWith(DisconnectPacket.create(
            Sonar.get().getConfig().getVerification().getAlreadyQueued(),
            mcConnection.getProtocolVersion(), true));
          return;
        }

        // Check if Fallback is already verifying a player
        // → is another player with the same IP address connected to Fallback?
        if (FALLBACK.getConnected().containsKey(event.getUsername())
          || FALLBACK.getConnected().containsValue(inetAddress)) {
          mcConnection.closeWith(DisconnectPacket.create(
            Sonar.get().getConfig().getVerification().getAlreadyVerifying(),
            mcConnection.getProtocolVersion(), true));
          return;
        }

        // Check if the IP address is currently being rate-limited
        if (!FALLBACK.getRatelimiter().attempt(inetAddress)) {
          mcConnection.closeWith(DisconnectPacket.create(
            Sonar.get().getConfig().getVerification().getTooFastReconnect(),
            mcConnection.getProtocolVersion(), true));
          return;
        }

        // We have to add this pipeline to monitor whenever the client disconnects
        // to remove them from the list of connected and queued players
        pipeline.addFirst(FALLBACK_HANDLER, new FallbackChannelHandler(event.getUsername(), inetAddress));

        // Queue the connection for further processing
        FALLBACK.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {

          // Do not continue if the connection is closed or marked as disconnected
          if (mcConnection.isClosed() || mcConnection.isKnownDisconnect()) return;

          // Check if the username matches the valid name regex to prevent
          // UTF-16 names or other types of exploits
          if (!Sonar.get().getConfig().getVerification().getValidNameRegex().matcher(event.getUsername()).matches()) {
            mcConnection.closeWith(DisconnectPacket.create(Sonar.get().getConfig().getVerification().getInvalidUsername(),
              mcConnection.getProtocolVersion(), true));
            return;
          }

          // Add better timeout handler to avoid known exploits or issues
          // We also want to timeout bots quickly to avoid flooding
          final int readTimeout = Sonar.get().getConfig().getVerification().getReadTimeout();
          pipeline.replace(READ_TIMEOUT, READ_TIMEOUT, new FallbackTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));

          // Create an instance for the Fallback connection
          final FallbackUserWrapper user = new FallbackUserWrapper(
            FALLBACK, mcConnection, mcConnection.getChannel(),
            mcConnection.getChannel().pipeline(), inetAddress,
            ProtocolVersion.fromId(protocolId));

          // Disconnect if the protocol version could not be resolved
          if (user.getProtocolVersion().isUnknown()) {
            mcConnection.closeWith(DisconnectPacket.create(Sonar.get().getConfig().getVerification().getInvalidProtocol(),
              mcConnection.getProtocolVersion(), true));
            return;
          }

          // The player joined the verification
          Statistics.REAL_TRAFFIC.increment();

          if (Sonar.get().getConfig().getVerification().isLogConnections()) {
            // Only log the processing message if the server isn't under attack.
            // We let the user override this through the configuration.
            if (!Sonar.get().getAttackTracker().isCurrentlyUnderAttack()
              || Sonar.get().getConfig().getVerification().isLogDuringAttack()) {
              FALLBACK.getLogger().info(Sonar.get().getConfig().getVerification().getConnectLog()
                .replace("%name%", gameProfile.getName())
                .replace("%ip%", Sonar.get().getConfig().formatAddress(user.getInetAddress()))
                .replace("%protocol%", String.valueOf(user.getProtocolVersion().getProtocol())));
            }
          }

          // Call the VerifyJoinEvent for external API usage
          Sonar.get().getEventManager().publish(new UserVerifyJoinEvent(gameProfile.getName(), user));

          // Mark the player as connected → verifying players
          FALLBACK.getConnected().put(gameProfile.getName(), inetAddress);

          // This sometimes happens when the channel hangs, but the player is still connecting
          // This also fixes a unique issue with TCPShield and other reverse proxies
          if (user.getPipeline().get(MINECRAFT_ENCODER) == null
            || user.getPipeline().get(MINECRAFT_DECODER) == null) {
            mcConnection.close(true);
            return;
          }

          // Replace normal encoder to allow custom packets
          final FallbackPacketEncoder encoder = new FallbackPacketEncoder(user.getProtocolVersion());
          user.getPipeline().replace(MINECRAFT_ENCODER, FALLBACK_PACKET_ENCODER, encoder);

          // Send LoginSuccess packet to make the client think they are joining the server
          user.write(new LoginSuccess(gameProfile.getName(), gameProfile.getId()));

          // The LoginSuccess packet has been sent, now we can change the registry state
          encoder.updateRegistry(user.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0
            ? FallbackPacketRegistry.CONFIG : FallbackPacketRegistry.GAME);

          // Replace normal decoder to allow custom packets
          user.getPipeline().replace(MINECRAFT_DECODER, FALLBACK_PACKET_DECODER, new FallbackPacketDecoder(user,
            new FallbackVerificationHandler(user, gameProfile.getName(), gameProfile.getId())));
        }));
      } catch (Throwable throwable) {
        throw new ReflectiveOperationException(throwable);
      }
    });
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull LoginEvent event) {
    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();

    if (maxOnlinePerIp > 0) {
      final ConnectedPlayer connectedPlayer = (ConnectedPlayer) event.getPlayer();
      final InetAddress inetAddress = event.getPlayer().getRemoteAddress().getAddress();

      // Check if the number of online players using the same IP address as
      // the connecting player is greater than the configured amount
      final long onlinePerIp = SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
        .filter(player -> Objects.equals(player.getRemoteAddress().getAddress(), inetAddress))
        .count();

      // We use '>=' because the player connecting to the server hasn't joined yet
      if (onlinePerIp >= maxOnlinePerIp) {
        connectedPlayer.getConnection().closeWith(DisconnectPacket.create(
          Sonar.get().getConfig().getTooManyOnlinePerIp(), connectedPlayer.getProtocolVersion(), true));
      }
    }
  }
}
