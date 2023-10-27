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
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
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
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyJoinEvent;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
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
import xyz.jonesdev.sonar.velocity.fallback.dummy.DummyConnection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.network.Connections.*;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil.isGeyserConnection;

@RequiredArgsConstructor
public final class FallbackListener {
  private final @NotNull Fallback fallback;

  private static final DummyConnection CLOSED_MINECRAFT_CONNECTION;

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final MethodHandle INITIAL_CONNECTION;
  private static final MethodHandle CONNECTED_PLAYER;
  private static final Field CONNECTION_FIELD;
  private static Field SESSION_HANDLER_FIELD;

  static {
    CLOSED_MINECRAFT_CONNECTION = new DummyConnection(null);

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

      try {
        SESSION_HANDLER_FIELD = MinecraftConnection.class.getDeclaredField("activeSessionHandler");
      } catch (Throwable throwable) {
        // Velocity b266 changed the field name to "activeSessionHandler"
        SESSION_HANDLER_FIELD = MinecraftConnection.class.getDeclaredField("sessionHandler");
      }
      SESSION_HANDLER_FIELD.setAccessible(true);

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
    // Increase joins per second for the action bar verbose
    Sonar.get().getVerboseHandler().getJoinsPerSecond().put(System.nanoTime());

    val inboundConnection = (LoginInboundConnection) event.getConnection();
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
        // Backwards compatibility for Velocity b265 and below
        val activeSessionHandler = (MinecraftSessionHandler) SESSION_HANDLER_FIELD.get(mcConnection);

        // Check the blacklist here since we cannot let the player "ghost join"
        if (fallback.getBlacklisted().has(inetAddress.toString())) {
          markConnectionAsDead(activeSessionHandler);
          initialConnection.getConnection().closeWith(Disconnect.create(
            Sonar.get().getConfig().getVerification().getBlacklisted(),
            inboundConnection.getProtocolVersion()
          ));
          return;
        }

        // Don't continue the verification process if the verification is disabled
        if (!Sonar.get().getConfig().getVerification().isEnabled()) return;

        // Check if the player is already verified.
        // No one wants to be verified over and over again.
        final GameProfile gameProfile = GameProfile.forOfflinePlayer(event.getUsername());
        if (Sonar.get().getVerifiedPlayerController().has(inetAddress, gameProfile.getId())) return;

        // Completely skip Geyser connections (for now)
        if (isGeyserConnection(channel)) {
          // TODO: Do we need to log this?
          fallback.getLogger().info("Allowing Geyser connection: {}", inetAddress);
          return;
        }

        // We now mark the connection as dead by using our fake connection
        markConnectionAsDead(activeSessionHandler);

        // Check if the player is already queued since we don't want bots to flood the queue
        if (fallback.getQueue().getQueuedPlayers().containsKey(inetAddress)) {
          initialConnection.getConnection().closeWith(Disconnect.create(
            Sonar.get().getConfig().getVerification().getAlreadyQueued(),
            inboundConnection.getProtocolVersion()
          ));
          return;
        }

        // Check if Fallback is already verifying a player
        // → is another player with the same IP address connected to Fallback?
        if (fallback.getConnected().containsKey(event.getUsername())
          || fallback.getConnected().containsValue(inetAddress)) {
          initialConnection.getConnection().closeWith(Disconnect.create(
            Sonar.get().getConfig().getVerification().getAlreadyVerifying(),
            inboundConnection.getProtocolVersion()
          ));
          return;
        }

        // We cannot allow too many players on our Fallback server
        // There's technically no reason for limiting this, but we'll better stay safe.
        if (fallback.getConnected().size() > Sonar.get().getConfig().getVerification().getMaxVerifyingPlayers()) {
          initialConnection.getConnection().closeWith(Disconnect.create(
            Sonar.get().getConfig().getVerification().getTooManyPlayers(),
            inboundConnection.getProtocolVersion()
          ));
          return;
        }

        // Check if the IP address is currently being rate-limited
        if (!fallback.getRatelimiter().attempt(inetAddress)) {
          initialConnection.getConnection().closeWith(Disconnect.create(
            Sonar.get().getConfig().getVerification().getTooFastReconnect(),
            inboundConnection.getProtocolVersion()
          ));
          return;
        }

        // We have to add this pipeline to monitor whenever the client disconnects
        // to remove them from the list of connected and queued players
        pipeline.addFirst(FALLBACK_HANDLER, new FallbackChannelHandler(event.getUsername()));

        // Queue the connection for further processing
        fallback.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {

          // Do not continue if the connection is closed or marked as disconnected
          if (mcConnection.isClosed() || mcConnection.isKnownDisconnect()) return;

          // Check if the username matches the valid name regex to prevent
          // UTF-16 names or other types of exploits
          if (!Sonar.get().getConfig().getVerification().getValidNameRegex().matcher(event.getUsername()).matches()) {
            mcConnection.closeWith(Disconnect.create(Sonar.get().getConfig().getVerification().getInvalidUsername(),
              mcConnection.getProtocolVersion()));
            return;
          }

          // Add better timeout handler to avoid known exploits or issues
          // We also want to timeout bots quickly to avoid flooding
          pipeline.replace(
            READ_TIMEOUT,
            READ_TIMEOUT,
            new FallbackTimeoutHandler(Sonar.get().getConfig().getVerification().getReadTimeout(),
              TimeUnit.MILLISECONDS)
          );

          // We need to determine if the player is premium before we handle the connection,
          // so we can create a ConnectedPlayer instance without having to spoof this
          final boolean onlineMode = !event.getResult().isForceOfflineMode()
            && (SonarVelocity.INSTANCE.getPlugin().getServer().getConfiguration().isOnlineMode()
            || event.getResult().isOnlineModeAllowed());

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
          final FallbackUserWrapper fallbackPlayer = new FallbackUserWrapper(
            fallback, connectedPlayer, mcConnection, mcConnection.getChannel(),
            mcConnection.getChannel().pipeline(), inetAddress,
            ProtocolVersion.fromId(connectedPlayer.getProtocolVersion().getProtocol())
          );

          // Disconnect if the protocol version could not be resolved
          if (fallbackPlayer.getProtocolVersion().isUnknown()) {
            fallbackPlayer.disconnect(Sonar.get().getConfig().getVerification().getInvalidProtocol());
            return;
          }

          // Check if the player is already connected to the proxy but still tries to verify
          if (!mcConnection.server.canRegisterConnection(connectedPlayer)) {
            fallbackPlayer.disconnect(Sonar.get().getConfig().getVerification().getAlreadyConnected());
            return;
          }

          // The player joined the verification
          Statistics.REAL_TRAFFIC.increment();

          if (Sonar.get().getConfig().getVerification().isLogConnections()) {
            // Only log the processing message if the server isn't under attack.
            // We let the user override this through the configuration.
            if (!fallback.isPotentiallyUnderAttack() || Sonar.get().getConfig().getVerification().isLogDuringAttack()) {
              fallback.getLogger().info(Sonar.get().getConfig().getVerification().getConnectLog()
                .replace("%name%", connectedPlayer.getUsername())
                .replace("%ip%", Sonar.get().getConfig().formatAddress(fallbackPlayer.getInetAddress()))
                .replace("%protocol%", String.valueOf(fallbackPlayer.getProtocolVersion().getProtocol())));
            }
          }

          // Call the VerifyJoinEvent for external API usage
          Sonar.get().getEventManager().publish(new UserVerifyJoinEvent(gameProfile.getName(), fallbackPlayer));

          // Mark the player as connected → verifying players
          fallback.getConnected().put(gameProfile.getName(), inetAddress);

          // This sometimes happens when the channel hangs, but the player is still connecting
          // This also fixes a unique issue with TCPShield and other reverse proxies
          if (fallbackPlayer.getPipeline().get(MINECRAFT_ENCODER) == null
            || fallbackPlayer.getPipeline().get(MINECRAFT_DECODER) == null) {
            mcConnection.close(true);
            return;
          }

          // Replace normal encoder to allow custom packets
          final FallbackPacketEncoder encoder = new FallbackPacketEncoder(fallbackPlayer.getProtocolVersion());
          fallbackPlayer.getPipeline().replace(MINECRAFT_ENCODER, FALLBACK_PACKET_ENCODER, encoder);

          // Send LoginSuccess packet to make the client think they are joining the server
          fallbackPlayer.write(new LoginSuccess(gameProfile.getName(), gameProfile.getId()));

          // The LoginSuccess packet has been sent, now we can change the registry state
          encoder.updateRegistry(fallbackPlayer.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0
            ? FallbackPacketRegistry.CONFIG : FallbackPacketRegistry.GAME);

          // Replace normal decoder to allow custom packets
          fallbackPlayer.getPipeline().replace(
            MINECRAFT_DECODER, FALLBACK_PACKET_DECODER, new FallbackPacketDecoder(fallbackPlayer,
              new FallbackVerificationHandler(fallbackPlayer, gameProfile.getName(), connectedPlayer.getUniqueId())
            ));
        }));
      } catch (Throwable throwable) {
        throw new ReflectiveOperationException(throwable);
      }
    });
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull LoginEvent event) {
    val connectedPlayer = (ConnectedPlayer) event.getPlayer();

    if (Sonar.get().getConfig().getLockdown().isEnabled()
      && !event.getPlayer().hasPermission(Sonar.get().getConfig().getLockdown().getBypassPermission())) {
      connectedPlayer.getConnection().closeWith(Disconnect.create(
        Sonar.get().getConfig().getLockdown().getDisconnect(), connectedPlayer.getProtocolVersion()));

      if (Sonar.get().getConfig().getLockdown().isLogAttempts()) {
        Sonar.get().getLogger().info(Sonar.get().getConfig().getLockdown().getConsoleLog()
          .replace("%player%", event.getPlayer().getUsername())
          .replace("%ip%", Sonar.get().getConfig()
            .formatAddress(event.getPlayer().getRemoteAddress().getAddress()))
          .replace("%protocol%",
            String.valueOf(event.getPlayer().getProtocolVersion().getProtocol())));
      }
      return;
    }

    final InetAddress inetAddress = event.getPlayer().getRemoteAddress().getAddress();

    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();

    if (maxOnlinePerIp > 0) {
      final long onlinePerIp = SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
        .filter(player -> Objects.equals(player.getRemoteAddress().getAddress(), inetAddress))
        .count();

      // We use '>=' because the player connecting to the server hasn't joined yet
      if (onlinePerIp >= maxOnlinePerIp) {
        connectedPlayer.getConnection().closeWith(Disconnect.create(
          Sonar.get().getConfig().getTooManyOnlinePerIp(), connectedPlayer.getProtocolVersion()
        ));
      }
    }
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(final @NotNull PostLoginEvent event) {
    if (Sonar.get().getConfig().getLockdown().isEnabled() && Sonar.get().getConfig().getLockdown().isNotifyAdmins()) {
      if (event.getPlayer().hasPermission(Sonar.get().getConfig().getLockdown().getBypassPermission())) {
        final String notification = Sonar.get().getConfig().getLockdown().getNotification();
        final Component deserialized = MiniMessage.miniMessage().deserialize(notification);
        event.getPlayer().sendMessage(deserialized);
      }
    }
  }
}
