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

package xyz.jonesdev.sonar.bungee.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.EncryptionUtil;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.PlayerPublicKey;
import net.md_5.bungee.protocol.packet.Kick;
import net.md_5.bungee.protocol.packet.LoginRequest;
import net.md_5.bungee.protocol.packet.StatusRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.md_5.bungee.netty.PipelineUtils.*;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_19_1;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_19_3;
import static xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil.isGeyserConnection;

public final class FallbackInitialHandler extends InitialHandler {
  public FallbackInitialHandler(final @NotNull BungeeCord bungee, final ListenerInfo listener) {
    super(bungee, listener);
    this.bungee = bungee;
  }
  private static final @NotNull Fallback FALLBACK = Objects.requireNonNull(Sonar.get().getFallback());
  @Getter
  private ChannelWrapper channelWrapper;
  private @NotNull final BungeeCord bungee;
  private @Nullable FallbackUserWrapper user;
  @Getter
  private ProtocolVersion protocolVersion;
  private boolean receivedLoginPacket;
  private boolean receivedStatusPacket;

  @Override
  public void connected(final ChannelWrapper channelWrapper) throws Exception {
    this.channelWrapper = channelWrapper;
    super.connected(channelWrapper);
  }

  @Override
  public void handle(final StatusRequest statusRequest) throws Exception {
    // Fix status packet spam exploit
    if (receivedStatusPacket) {
      throw new ConditionFailedException("Duplicate status packet");
    }
    receivedStatusPacket = true;
    // Run the rest of the method asynchronously
    CompletableFuture.runAsync(() -> {
      // The channel always stays open because the client sends
      // a StatusRequest and a Ping packet after one another
      if (!isConnected()) {
        throw new ConditionFailedException("Not connected anymore");
      }
      try {
        super.handle(statusRequest);
      } catch (Exception exception) {
        throw new DecoderException(exception);
      }
    });
  }

  @Override
  public void handle(final LoginRequest loginRequest) throws Exception {
    // Increase joins per second for the action bar verbose
    Sonar.get().getVerboseHandler().getLoginsPerSecond().put(System.nanoTime());

    // Fix login packet spam exploit
    if (receivedLoginPacket || user != null) {
      throw new ConditionFailedException("Duplicate login packet");
    }
    receivedLoginPacket = true;
    // Cache protocol version so other handlers don't throw NPEs
    protocolVersion = ProtocolVersion.fromId(getHandshake().getProtocolVersion());
    final Channel channel = channelWrapper.getHandle();

    // Run in the channel's event loop
    channel.eventLoop().execute(() -> {
      try {
        final ChannelPipeline pipeline = channel.pipeline();
        TrafficChannelHooker.hook(pipeline, PACKET_DECODER, PACKET_ENCODER);

        // Increase total traffic statistic
        Statistics.TOTAL_TRAFFIC.increment();

        // Check if the verification is enabled
        if (!Sonar.get().getFallback().shouldVerifyNewPlayers()) {
          super.handle(loginRequest);
          return;
        }

        final InetAddress inetAddress = getAddress().getAddress();
        val uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + loginRequest.getData()).getBytes(StandardCharsets.UTF_8));
        // Check if the player is already verified
        if (Sonar.get().getVerifiedPlayerController().has(inetAddress, uuid)) {
          super.handle(loginRequest);
          return;
        }

        // Check the blacklist here since we cannot let the player "ghost join"
        if (FALLBACK.getBlacklisted().has(inetAddress.toString())) {
          closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getBlacklisted()));
          return;
        }

        // Completely skip Geyser connections
        if (isGeyserConnection(channel)) {
          FALLBACK.getLogger().info("Skipping Geyser player: {}{}",
            loginRequest.getData(), Sonar.get().getConfig().formatAddress(inetAddress));
          super.handle(loginRequest);
          return;
        }

        // Create wrapped Fallback user
        user = new FallbackUserWrapper(
          FALLBACK, channelWrapper, this,
          channel, channel.pipeline(), inetAddress, protocolVersion
        );

        // Perform default BungeeCord checks
        if (bungee.config.isEnforceSecureProfile()
          && user.getProtocolVersion().compareTo(MINECRAFT_1_19_3) < 0) {
          final PlayerPublicKey publicKey = loginRequest.getPublicKey();
          if (publicKey == null) {
            disconnect(bungee.getTranslation("secure_profile_required"));
            return;
          }
          if (Instant.ofEpochMilli(publicKey.getExpiry()).isBefore(Instant.now())) {
            disconnect(bungee.getTranslation("secure_profile_expired"));
            return;
          }
          if (user.getProtocolVersion().compareTo(MINECRAFT_1_19_1) < 0
            && !EncryptionUtil.check(publicKey, null)) {
            disconnect(bungee.getTranslation("secure_profile_invalid"));
            return;
          }
        }

        // Check if the player is already queued since we don't want bots to flood the queue
        if (FALLBACK.getQueue().getQueuedPlayers().containsKey(inetAddress)) {
          closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getAlreadyQueued()));
          return;
        }

        // Check if Fallback is already verifying a player
        // → is another player with the same IP address connected to Fallback?
        if (FALLBACK.getConnected().containsKey(loginRequest.getData())
          || FALLBACK.getConnected().containsValue(inetAddress)) {
          closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getAlreadyVerifying()));
          return;
        }

        // We cannot allow too many players on our Fallback server
        // There's technically no reason for limiting this, but we'll better stay safe.
        if (FALLBACK.getConnected().size() > Sonar.get().getConfig().getVerification().getMaxVerifyingPlayers()) {
          closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getTooManyPlayers()));
          return;
        }

        // Check if the IP address is currently being rate-limited
        if (!FALLBACK.getRatelimiter().attempt(inetAddress)) {
          closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getTooFastReconnect()));
          return;
        }

        // We have to add this pipeline to monitor whenever the client disconnects
        // to remove them from the list of connected and queued players
        pipeline.addFirst(FALLBACK_HANDLER, new FallbackChannelHandler(loginRequest.getData()));

        // Queue the connection for further processing
        FALLBACK.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {

          // Do not continue if the connection is closed or marked as disconnected
          if (channelWrapper.isClosed() || channelWrapper.isClosing()) return;

          // Check if the username matches the valid name regex to prevent
          // UTF-16 names or other types of exploits
          if (!Sonar.get().getConfig().getVerification().getValidNameRegex().matcher(loginRequest.getData()).matches()) {
            closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getInvalidUsername()));
            return;
          }

          // Add better timeout handler to avoid known exploits or issues
          // We also want to timeout bots quickly to avoid flooding
          pipeline.replace(
            TIMEOUT_HANDLER,
            TIMEOUT_HANDLER,
            new FallbackTimeoutHandler(Sonar.get().getConfig().getVerification().getReadTimeout(),
              TimeUnit.MILLISECONDS)
          );

          // Disconnect if the protocol version could not be resolved
          if (user.getProtocolVersion().isUnknown()) {
            closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getInvalidProtocol()));
            return;
          }

          // Check if the player is already connected to the proxy but still tries to verify
          final int limit = bungee.config.getPlayerLimit();
          if (limit > 0 && bungee.getOnlineCount() >= limit) {
            disconnect(bungee.getTranslation("proxy_full"));
            return;
          } else if (!isOnlineMode() && bungee.getPlayer(loginRequest.getData()) != null) {
            closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getAlreadyConnected()));
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
                .replace("%name%", loginRequest.getData())
                .replace("%ip%", Sonar.get().getConfig().formatAddress(inetAddress))
                .replace("%protocol%", String.valueOf(user.getProtocolVersion().getProtocol())));
            }
          }

          // Call the VerifyJoinEvent for external API usage
          Sonar.get().getEventManager().publish(new UserVerifyJoinEvent(loginRequest.getData(), user));

          // Mark the player as connected → verifying players
          FALLBACK.getConnected().put(loginRequest.getData(), inetAddress);

          // This sometimes happens when the channel hangs, but the player is still connecting
          // This also fixes a unique issue with TCPShield and other reverse proxies
          if (user.getPipeline().get(PACKET_ENCODER) == null
            || user.getPipeline().get(PACKET_DECODER) == null) {
            channelWrapper.close();
            return;
          }

          // Replace normal encoder to allow custom packets
          final FallbackPacketEncoder encoder = new FallbackPacketEncoder(user.getProtocolVersion());
          user.getPipeline().replace(PACKET_ENCODER, FALLBACK_PACKET_ENCODER, encoder);

          // Send LoginSuccess packet to make the client think they are joining the server
          user.write(new LoginSuccess(loginRequest.getData(), uuid));

          // The LoginSuccess packet has been sent, now we can change the registry state
          encoder.updateRegistry(user.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0
            ? FallbackPacketRegistry.CONFIG : FallbackPacketRegistry.GAME);

          // Replace normal decoder to allow custom packets
          user.getPipeline().replace(
            PACKET_DECODER, FALLBACK_PACKET_DECODER, new FallbackPacketDecoder(user,
              new FallbackVerificationHandler(user, loginRequest.getData(), uuid)
            ));
        }));
      } catch (Throwable throwable) {
        throw new ReflectiveOperationException(throwable);
      }
    });
  }

  private static final Map<Component, Kick> CACHED_KICK_PACKETS = new ConcurrentHashMap<>(16);

  public static @NotNull Kick getKickPacket(final @NotNull Component component) {
    Kick cachedKickPacket = CACHED_KICK_PACKETS.get(component);
    if (cachedKickPacket == null) {
      final String serialized = JSONComponentSerializer.json().serialize(component);
      cachedKickPacket = new Kick(ComponentSerializer.deserialize(serialized));
      CACHED_KICK_PACKETS.put(component, cachedKickPacket);
    }
    return cachedKickPacket;
  }

  // Mostly taken from Velocity
  public void closeWith(final Object msg) {
    if (channelWrapper.getHandle().isActive()) {
      boolean is17 = protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0
        && protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_7_2) >= 0;
      if (is17) {
        channelWrapper.getHandle().eventLoop().execute(() -> {
          channelWrapper.getHandle().config().setAutoRead(false);
          channelWrapper.getHandle().eventLoop().schedule(() -> {
            channelWrapper.markClosed();
            channelWrapper.getHandle().writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
          }, 250L, TimeUnit.MILLISECONDS);
        });
      } else {
        channelWrapper.markClosed();
        channelWrapper.getHandle().writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
      }
    }
  }
}
