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

package xyz.jonesdev.sonar.bungee.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import lombok.Getter;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.EncryptionUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.UpstreamBridge;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.PlayerPublicKey;
import net.md_5.bungee.protocol.packet.Handshake;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.md_5.bungee.netty.PipelineUtils.*;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_19_1;
import static xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil.isGeyserConnection;

public final class FallbackHandlerBoss extends HandlerBoss {
  private static final BungeeCord bungee = BungeeCord.getInstance();
  private ChannelWrapper channelWrapper;
  private PacketHandler handler;

  private Channel channel;

  @Getter
  private InetSocketAddress address;

  public FallbackHandlerBoss(final Channel channel) {
    this.channel=channel;
    this.address = (InetSocketAddress) channel.remoteAddress();
  }

  private static final @NotNull Fallback FALLBACK = Objects.requireNonNull(Sonar.get().getFallback());

  @Override
  public void setHandler(final @NotNull PacketHandler handler) {
    this.handler = handler;
    super.setHandler(handler);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      channelWrapper = new ChannelWrapper(ctx);
      super.channelActive(ctx);

      // At: InitialHandler#connected

      // Increase connections per second for the action bar verbose
      Counters.CONNECTIONS_PER_SECOND.put(System.nanoTime());
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      channelWrapper.markClosed();
      super.channelInactive(ctx);
    }
  }

  @Override
  public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      handler.writabilityChanged(channelWrapper);
      super.channelWritabilityChanged(ctx);
    }
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof HAProxyMessage) {
      final HAProxyMessage haproxyMessage = (HAProxyMessage) msg;
      this.address = InetSocketAddress.createUnresolved(
        haproxyMessage.sourceAddress(),
        haproxyMessage.sourcePort()
      );
      super.channelRead(ctx, msg);
    }
    if (msg instanceof PacketWrapper) {
      final PacketWrapper wrapper = (PacketWrapper) msg;
      final DefinedPacket packet = wrapper.packet;
      if (packet != null) {
        if (packet instanceof Handshake)
          this.handle((Handshake) packet);
        else if (packet instanceof StatusRequest)
          this.handle((StatusRequest) packet);
        else if (packet instanceof LoginRequest) {
          this.handle((LoginRequest) packet, ctx, wrapper);
          return;
        }
      }
      super.channelRead(ctx, msg);
    }
  }

  @Override
  public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final Throwable throwable) {
    if (ctx.channel().isActive()) {
      if (handler instanceof DownstreamBridge || handler instanceof UpstreamBridge) {
        try {
          handler.exception(throwable);
        } catch (Exception exception) {
          ProxyServer.getInstance().getLogger().severe(handler + " - exception processing exception: " + exception);
        }
      }

      channelWrapper.markClosed();
      ctx.close();
    }
  }

  @Getter
  private Handshake handshake;
  public void handle(final @NotNull Handshake handshake) {
    if (handshake.getHost().isEmpty()) throw new CorruptedFrameException("Hostname is empty!");
    this.handshake=handshake;
  }

  private boolean receivedStatusPacket;
  @SuppressWarnings("unused")
  public void handle(final @NotNull StatusRequest statusRequest) {
    if (receivedStatusPacket)
      throw new CorruptedFrameException("Duplicated status packet");
    receivedStatusPacket=true;
  }

  private boolean receivedLoginPacket;
  private @Nullable FallbackUserWrapper user;
  private ProtocolVersion protocolVersion;
  public void handle(
    final @NotNull LoginRequest loginRequest,
    final @NotNull ChannelHandlerContext ctx,
    final @NotNull PacketWrapper wrapper
  ) {
    // Increase joins per second for the action bar verbose
    Counters.LOGINS_PER_SECOND.put(System.nanoTime());

    // Fix login packet spam exploit
    if (receivedLoginPacket || user != null) {
      throw new CorruptedFrameException("Duplicate login packet");
    }
    receivedLoginPacket = true;

    // Cache protocol version so other handlers don't throw NPEs
    final int protocolId = getHandshake().getProtocolVersion();
    protocolVersion = ProtocolVersion.fromId(protocolId);
    this.channel = this.channelWrapper.getHandle();

    // Run in the channel's event loop
    channel.eventLoop().execute(() -> {
      // Do not continue if the connection is closed or marked as disconnected
      if (channelWrapper.isClosed() || channelWrapper.isClosing()) return;
      try {

        // Hook the custom traffic pipeline, so we can count the incoming and outgoing traffic
        final ChannelPipeline pipeline = channel.pipeline();
        TrafficChannelHooker.hook(pipeline, PACKET_DECODER, PACKET_ENCODER);

        // Increase total traffic statistic
        Statistics.TOTAL_TRAFFIC.increment();

        final InetAddress inetAddress = getAddress().getAddress();
        // Check the blacklist here since we cannot let the player "ghost join"
        if (FALLBACK.getBlacklisted().has(inetAddress)) {
          closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getBlacklisted()));
          return;
        }

        // Check if the verification is enabled
        if (!Sonar.get().getFallback().shouldVerifyNewPlayers()) {
          super.channelRead(ctx, wrapper);
          return;
        }

        // Completely skip Geyser connections
        if (isGeyserConnection(channel)) {
          FALLBACK.getLogger().info("Skipping Geyser player: {}{}",
            loginRequest.getData(), Sonar.get().getConfig().formatAddress(inetAddress));
          super.channelRead(ctx, wrapper);
          return;
        }

        // Check if the protocol ID of the player is not allowed to enter the server
        if (Sonar.get().getConfig().getVerification().getBlacklistedProtocols().contains(protocolId)) {
          closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getProtocolBlacklisted()));
          return;
        }

        val uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + loginRequest.getData()).getBytes(StandardCharsets.UTF_8));
        // Check if the player is already verified
        if (Sonar.get().getVerifiedPlayerController().has(inetAddress, uuid)) {
          super.channelRead(ctx, wrapper);
          return;
        }

        // Check if the protocol ID of the player is allowed to bypass verification
        if (Sonar.get().getConfig().getVerification().getWhitelistedProtocols().contains(protocolId)) {
          super.channelRead(ctx, wrapper);
          return;
        }

        // Create wrapped Fallback user
        user = new FallbackUserWrapper(
          FALLBACK, (InitialHandler) this.handler, channel, channel.pipeline(),
          inetAddress, protocolVersion, this
        );

        if (bungee.config.isEnforceSecureProfile() &&
          user.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) < 0) {
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

        // Check if the IP address is currently being rate-limited
        if (!FALLBACK.getRatelimiter().attempt(inetAddress)) {
          closeWith(getKickPacket(Sonar.get().getConfig().getVerification().getTooFastReconnect()));
          return;
        }

        // We have to add this pipeline to monitor whenever the client disconnects
        // to remove them from the list of connected and queued players
        pipeline.addFirst(FALLBACK_HANDLER, new FallbackChannelHandler(loginRequest.getData(), inetAddress));

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
          final int readTimeout = Sonar.get().getConfig().getVerification().getReadTimeout();
          pipeline.replace(TIMEOUT_HANDLER, TIMEOUT_HANDLER,
            new FallbackTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));

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

            // Removed onlineMode check here. Because we are not in InitialHandler.
          } else if (bungee.getPlayer(loginRequest.getData()) != null) {
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
          user.getPipeline().replace(PACKET_DECODER, FALLBACK_PACKET_DECODER,
            new FallbackPacketDecoder(user, new FallbackVerificationHandler(user, loginRequest.getData(), uuid)));
        }));
      } catch (final Throwable throwable) {
        throw new ReflectiveOperationException(throwable);
      }
    });
  }

  // Status is unchecked. Because we only send kick packets at the login stage.
  private void disconnect(final String message) {
    this.channelWrapper.delayedClose(new Kick(TextComponent.fromLegacy(message)));
  }

  public void closeWith(final Object msg) { closeWith(this.channelWrapper, this.protocolVersion, msg); }

  public static void closeWith(
    final ChannelWrapper wrapper,
    final ProtocolVersion version,
    final Object msg
  ) {
    if (wrapper.getHandle().isActive()) {
      boolean is17 = version.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0
        && version.compareTo(ProtocolVersion.MINECRAFT_1_7_2) >= 0;
      if (is17) {
        wrapper.getHandle().eventLoop().execute(() -> {
          wrapper.getHandle().config().setAutoRead(false);
          wrapper.getHandle().eventLoop().schedule(() -> {
            wrapper.markClosed();
            wrapper.getHandle().writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
          }, 250L, TimeUnit.MILLISECONDS);
        });
      } else {
        wrapper.markClosed();
        wrapper.getHandle().writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  private static final Map<Component, Kick> CACHED_KICK_PACKETS = new ConcurrentHashMap<>(10);

  // Use computeIfAbsent to ensure thread safety.
  public static @NotNull Kick getKickPacket(final @NotNull Component component) {
    return CACHED_KICK_PACKETS.computeIfAbsent(component, key -> {
      final String serialized = JSONComponentSerializer.json().serialize(key);
      return new Kick(ComponentSerializer.deserialize(serialized));
    });
  }
}
