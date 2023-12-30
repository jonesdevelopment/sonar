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
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.CorruptedFrameException;
import net.kyori.adventure.text.Component;
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_3;
import static com.velocitypowered.proxy.network.Connections.*;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil.isGeyserConnection;

public final class FallbackMinecraftConnection extends MinecraftConnection {
  public FallbackMinecraftConnection(final Channel channel, final VelocityServer server) {
    super(channel, server);
    this.channel = channel;
  }

  private final Channel channel;
  private FallbackUserWrapper user;
  private boolean receivedLoginPacket;

  private static final @NotNull Fallback FALLBACK = Objects.requireNonNull(Sonar.get().getFallback());
  private static final boolean FORCE_SECURE_PROFILES;

  static {
    FORCE_SECURE_PROFILES = Boolean.getBoolean("auth.forceSecureProfiles");
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    // Increase connections per second for the action bar verbose
    Counters.CONNECTIONS_PER_SECOND.put(System.nanoTime());
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof Handshake handshake) {
      // Check if the host is invalid
      if (handshake.getServerAddress().isEmpty()) {
        throw new CorruptedFrameException("Hostname is empty");
      }
    }
    if (msg instanceof ServerLogin login) {
      // Increase joins per second for the action bar verbose
      Counters.LOGINS_PER_SECOND.put(System.nanoTime());

      // Fix login packet spam exploit
      if (receivedLoginPacket || user != null) {
        throw new CorruptedFrameException("Duplicate login packet");
      }
      receivedLoginPacket = true;

      // Run in the channel's event loop
      channel.eventLoop().execute(() -> {

        // Do not continue if the connection is closed or marked as disconnected
        if (isClosed() || isKnownDisconnect()) return;

        // Hook the custom traffic pipeline, so we can count the incoming and outgoing traffic
        final ChannelPipeline pipeline = channel.pipeline();
        TrafficChannelHooker.hook(pipeline, MINECRAFT_DECODER, MINECRAFT_ENCODER);

        final InetAddress inetAddress = ((InetSocketAddress) getRemoteAddress()).getAddress();

        // Increase total traffic statistic
        Statistics.TOTAL_TRAFFIC.increment();

        try {

          // Check the blacklist here since we cannot let the player "ghost join"
          if (FALLBACK.getBlacklisted().has(inetAddress)) {
            // Mark the connection as dead to avoid unnecessary console logs
            closeWith(Disconnect.create(Sonar.get().getConfig().getVerification().getBlacklisted(),
              getProtocolVersion(), false));
            return;
          }

          // Don't continue the verification process if the verification is disabled
          if (!Sonar.get().getFallback().shouldVerifyNewPlayers()) {
            super.channelRead(ctx, msg);
            return;
          }

          // Completely skip Geyser connections
          if (isGeyserConnection(channel)) {
            FALLBACK.getLogger().info("Skipping Geyser player: {}{}",
              login.getUsername(), Sonar.get().getConfig().formatAddress(inetAddress));
            return;
          }

          // Check if the protocol ID of the player is not allowed to enter the server
          final int protocolId = getProtocolVersion().getProtocol();
          if (Sonar.get().getConfig().getVerification().getBlacklistedProtocols().contains(protocolId)) {
            // Mark the connection as dead to avoid unnecessary console logs
            //markConnectionAsDead(activeSessionHandler);
            closeWith(Disconnect.create(Sonar.get().getConfig().getVerification().getProtocolBlacklisted(),
              getProtocolVersion(), false));
            return;
          }

          // Check if the player is already verified.
          // No one wants to be verified over and over again.
          final GameProfile gameProfile = GameProfile.forOfflinePlayer(login.getUsername());
          if (Sonar.get().getVerifiedPlayerController().has(inetAddress, gameProfile.getId())) {
            super.channelRead(ctx, msg);
            return;
          }

          // Check if the protocol ID of the player is allowed to bypass verification
          if (Sonar.get().getConfig().getVerification().getWhitelistedProtocols().contains(protocolId)) {
            super.channelRead(ctx, msg);
            return;
          }

          // Don't allow exceptions or disconnect messages
          setAssociation(null);

          // Check if the player is already queued since we don't want bots to flood the queue
          if (FALLBACK.getQueue().getQueuedPlayers().containsKey(inetAddress)) {
            closeWith(Disconnect.create(Sonar.get().getConfig().getVerification().getAlreadyQueued(),
              getProtocolVersion(), false));
            return;
          }

          // Check if Fallback is already verifying a player
          // → is another player with the same IP address connected to Fallback?
          if (FALLBACK.getConnected().containsKey(login.getUsername())
            || FALLBACK.getConnected().containsValue(inetAddress)) {
            closeWith(Disconnect.create(Sonar.get().getConfig().getVerification().getAlreadyVerifying(),
              getProtocolVersion(), false));
            return;
          }

          // Check if the IP address is currently being rate-limited
          if (!FALLBACK.getRatelimiter().attempt(inetAddress)) {
            closeWith(Disconnect.create(Sonar.get().getConfig().getVerification().getTooFastReconnect(),
              getProtocolVersion(), false));
            return;
          }

          // We have to add this pipeline to monitor whenever the client disconnects
          // to remove them from the list of connected and queued players
          pipeline.addFirst(FALLBACK_HANDLER, new FallbackChannelHandler(login.getUsername(), inetAddress));

          // Queue the connection for further processing
          FALLBACK.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {

            // Do not continue if the connection is closed or marked as disconnected
            if (isClosed() || isKnownDisconnect()) return;

            // Check if the username matches the valid name regex to prevent
            // UTF-16 names or other types of exploits
            if (!Sonar.get().getConfig().getVerification().getValidNameRegex().matcher(login.getUsername()).matches()) {
              closeWith(Disconnect.create(Sonar.get().getConfig().getVerification().getInvalidUsername(),
                getProtocolVersion(), false));
              return;
            }

            // Add better timeout handler to avoid known exploits or issues
            // We also want to timeout bots quickly to avoid flooding
            final int readTimeout = Sonar.get().getConfig().getVerification().getReadTimeout();
            pipeline.replace(READ_TIMEOUT, READ_TIMEOUT, new FallbackTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));

            // Create an instance for the Fallback connection
            user = new FallbackUserWrapper(FALLBACK, this, channel, channel.pipeline(),
              inetAddress, ProtocolVersion.fromId(protocolId));

            // Perform default Velocity checks
            final IdentifiedKey playerKey = login.getPlayerKey();
            if (playerKey != null) {
              if (playerKey.hasExpired()) {
                closeWith(Disconnect.create(
                  Component.translatable("multiplayer.disconnect.invalid_public_key_signature"),
                  getProtocolVersion(), false));
                return;
              }

              boolean isKeyValid;
              if (playerKey.getKeyRevision() == IdentifiedKey.Revision.LINKED_V2 && playerKey instanceof IdentifiedKeyImpl keyImpl) {
                isKeyValid = keyImpl.internalAddHolder(login.getHolderUuid());
              } else {
                isKeyValid = playerKey.isSignatureValid();
              }

              if (!isKeyValid) {
                closeWith(Disconnect.create(
                  Component.translatable("multiplayer.disconnect.invalid_public_key"),
                  getProtocolVersion(), false));
                return;
              }
            } else if ((FORCE_SECURE_PROFILES || server.getConfiguration().isForceKeyAuthentication())
              && getProtocolVersion().compareTo(MINECRAFT_1_19) >= 0
              && getProtocolVersion().compareTo(MINECRAFT_1_19_3) < 0) {
              closeWith(Disconnect.create(
                Component.translatable("multiplayer.disconnect.missing_public_key"),
                getProtocolVersion(), false));
              return;
            }

            // Disconnect if the protocol version could not be resolved
            if (user.getProtocolVersion().isUnknown()) {
              user.disconnect(Sonar.get().getConfig().getVerification().getInvalidProtocol());
              return;
            }

            // Check if the player is already connected to the proxy but still tries to verify
            if (server.getPlayer(gameProfile.getId()).isPresent()
              || server.getPlayer(gameProfile.getName()).isPresent()) {
              user.disconnect(Sonar.get().getConfig().getVerification().getAlreadyConnected());
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
                  .replace("%name%", login.getUsername())
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
              close(true);
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
      return;
    }
    super.channelRead(ctx, msg);
  }
}
