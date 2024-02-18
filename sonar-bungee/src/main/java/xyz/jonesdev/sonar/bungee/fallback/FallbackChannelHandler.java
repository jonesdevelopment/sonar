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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.Kick;
import net.md_5.bungee.protocol.packet.LoginRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.statistics.Counters;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.common.fallback.FallbackBandwidthHandler;
import xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper;
import xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static net.md_5.bungee.netty.PipelineUtils.*;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_BANDWIDTH;
import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.closeWith;

@RequiredArgsConstructor
public final class FallbackChannelHandler extends ChannelInboundHandlerAdapter {
  private final Channel channel;
  private @Nullable String username;
  private ProtocolVersion protocolVersion;
  private InetAddress inetAddress;

  private static final Fallback FALLBACK = Sonar.get().getFallback();
  private static final Cache<Component, Kick> CACHED_KICK_PACKETS = Caffeine.newBuilder().build();

  private static final Field CHANNEL_WRAPPER_FIELD;

  static {
    try {
      CHANNEL_WRAPPER_FIELD = HandlerBoss.class.getDeclaredField("channel");
      CHANNEL_WRAPPER_FIELD.setAccessible(true);
    } catch (Exception exception) {
      throw new ReflectiveOperationException(exception);
    }
  }

  public static @NotNull Kick getCachedKickPacket(final Component component) {
    Kick packet = CACHED_KICK_PACKETS.asMap().get(component);
    if (packet == null) {
      final String serialized = JSONComponentSerializer.json().serialize(component);
      final BaseComponent baseComponent = ComponentSerializer.deserialize(serialized);
      packet = new Kick(baseComponent);
      CACHED_KICK_PACKETS.put(component, packet);
    }
    return packet;
  }

  @Override
  public void channelActive(final @NotNull ChannelHandlerContext ctx) throws Exception {
    // Increase connections per second for the action bar verbose
    Counters.CONNECTIONS_PER_SECOND.put(System.nanoTime(), (byte) 0);
    // Make sure to let the server handle the rest
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(final @NotNull ChannelHandlerContext ctx) throws Exception {
    // The player can disconnect without sending the login packet first
    if (username != null) {
      // Remove the username from the connected players
      FALLBACK.getConnected().remove(username);
    }
    // The player cannot be in the queue if the IP address is invalid
    if (inetAddress != null) {
      // Remove the IP address from the queue
      FALLBACK.getQueue().remove(inetAddress);
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx, final Object msg) throws Exception {
    // Intercept any packets processed by BungeeCord
    if (msg instanceof PacketWrapper) {
      final PacketWrapper packetWrapper = (PacketWrapper) msg;
      final DefinedPacket wrappedPacket = packetWrapper.packet;
      // Don't handle any invalid packets
      if (wrappedPacket != null) {
        // Intercept any handshake packet by the client
        if (wrappedPacket instanceof Handshake) {
          handleHandshake((Handshake) wrappedPacket);
        }
        // Intercept any server login packet by the client
        if (wrappedPacket instanceof LoginRequest) {
          handleLogin(ctx, (LoginRequest) wrappedPacket, msg);
          return;
        }
      }
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelRead(msg);
  }

  // We can override the default exceptionCaught method since this handler
  // will run before the MinecraftConnection knows that there has been an error.
  // Additionally, this will also run after our custom decoder.
  @Override
  public void exceptionCaught(final @NotNull ChannelHandlerContext ctx,
                              final @NotNull Throwable cause) throws Exception {
    // Sonar sometimes uses exceptions to quickly close
    // the channel and interrupt any other ongoing process.
    // Simply close the channel if we encounter any errors.
    ctx.close();
  }

  private void handleHandshake(final @NotNull Handshake handshake) throws Exception {
    // Check if the player has already sent a handshake packet
    if (protocolVersion != null) {
      throw new CorruptedFrameException("Already sent handshake");
    }
    // Check if the hostname is invalid
    if (handshake.getHost().isEmpty()) {
      throw new CorruptedFrameException("Hostname is empty");
    }
    // Store the protocol version
    protocolVersion = ProtocolVersion.fromId(handshake.getProtocolVersion());
    // Hook the traffic listener
    // TODO: Can we implement this in channelActive?
    channel.pipeline().addFirst(FALLBACK_BANDWIDTH, FallbackBandwidthHandler.INSTANCE);
  }

  private void handleLogin(final @NotNull ChannelHandlerContext ctx,
                           final @NotNull LoginRequest loginRequest,
                           final @NotNull Object wrappedMessage) throws Exception {
    // Check if the player has already sent a login packet
    if (username != null) {
      throw new CorruptedFrameException("Already logged on");
    }
    // Increase joins per second for the action bar verbose
    Counters.LOGINS_PER_SECOND.put(System.nanoTime(), (byte) 0);
    // Increase total traffic statistic
    Statistics.TOTAL_TRAFFIC.increment();
    // Store the username
    username = loginRequest.getData();
    // Make sure to use the potentially modified, real IP
    final HandlerBoss handlerBoss = channel.pipeline().get(HandlerBoss.class);
    final ChannelWrapper channelWrapper = (ChannelWrapper) CHANNEL_WRAPPER_FIELD.get(handlerBoss);
    final InetSocketAddress socketAddress = (InetSocketAddress) channelWrapper.getRemoteAddress();
    inetAddress = socketAddress.getAddress();

    // Check the blacklist here since we cannot let the player "ghost join"
    if (FALLBACK.getBlacklist().asMap().containsKey(inetAddress)) {
      closeWith(channel, protocolVersion,
        getCachedKickPacket(Sonar.get().getConfig().getVerification().getBlacklisted()));
      return;
    }

    // Don't continue the verification process if the verification is disabled
    if (!Sonar.get().getFallback().shouldVerifyNewPlayers()) {
      ctx.fireChannelRead(wrappedMessage);
      return;
    }

    // Completely skip Geyser connections
    if (GeyserUtil.isGeyserConnection(channel, socketAddress)) {
      ctx.fireChannelRead(wrappedMessage);
      return;
    }

    // Check if the player is already queued since we don't want bots to flood the queue
    if (FALLBACK.getQueue().getQueuedPlayers().containsKey(inetAddress)) {
      closeWith(channel, protocolVersion,
        getCachedKickPacket(Sonar.get().getConfig().getVerification().getAlreadyQueued()));
      return;
    }

    // Check if Fallback is already verifying a player
    // → is another player with the same IP address connected to Fallback?
    if (FALLBACK.getConnected().containsKey(loginRequest.getData())
      || FALLBACK.getConnected().containsValue(inetAddress)) {
      closeWith(channel, protocolVersion,
        getCachedKickPacket(Sonar.get().getConfig().getVerification().getAlreadyVerifying()));
      return;
    }

    // Check if the protocol ID of the player is not allowed to enter the server
    if (Sonar.get().getConfig().getVerification().getBlacklistedProtocols()
      .contains(protocolVersion.getProtocol())) {
      closeWith(channel, protocolVersion,
        getCachedKickPacket(Sonar.get().getConfig().getVerification().getProtocolBlacklisted()));
      return;
    }

    // Make sure we actually have to verify the player
    final String offlineUUIDString = "OfflinePlayer:" + loginRequest.getData();
    final UUID offlineUUID = UUID.nameUUIDFromBytes(offlineUUIDString.getBytes(StandardCharsets.UTF_8));
    if (Sonar.get().getVerifiedPlayerController().has(inetAddress, offlineUUID)) {
      ctx.fireChannelRead(wrappedMessage);
      return;
    }

    // Check if the IP address is currently being rate-limited
    if (!FALLBACK.getRatelimiter().attempt(inetAddress)) {
      closeWith(channel, protocolVersion,
        getCachedKickPacket(Sonar.get().getConfig().getVerification().getTooFastReconnect()));
      return;
    }

    // Check if the protocol ID of the player is allowed to bypass verification
    if (Sonar.get().getConfig().getVerification().getWhitelistedProtocols()
      .contains(protocolVersion.getProtocol())) {
      ctx.fireChannelRead(wrappedMessage);
      return;
    }

    // Queue the connection for further processing
    FALLBACK.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {
      // Check if the username matches the valid name regex to prevent
      // UTF-16 names or other types of exploits
      if (!Sonar.get().getConfig().getVerification().getValidNameRegex()
        .matcher(loginRequest.getData()).matches()) {
        closeWith(channel, protocolVersion,
          Sonar.get().getConfig().getVerification().getInvalidUsername());
        return;
      }

      // Create an instance for the Fallback connection
      final FallbackUser user = new FallbackUserWrapper(channel, inetAddress, protocolVersion);
      // Let the verification handler take over the channel
      user.hijack(loginRequest.getData(), offlineUUID, PACKET_ENCODER, PACKET_DECODER, TIMEOUT_HANDLER, BOSS_HANDLER);
    }));
  }
}
