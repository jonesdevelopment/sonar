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

package xyz.jonesdev.sonar.common.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackVarInt21FrameDecoder;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackVarInt21FrameEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.util.GeyserUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.closeWith;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

@RequiredArgsConstructor
public abstract class FallbackPacketDecoderAdapter extends MessageToMessageDecoder<Object> {
  protected final String encoder, handler;
  protected @Nullable String username;
  protected ProtocolVersion protocolVersion;

  protected static final Fallback FALLBACK = Sonar.get().getFallback();

  /**
   * Validates and handles incoming handshake packets
   *
   * @param hostname Hostname (server address) sent by the client
   * @param protocol Protocol version number sent by the client
   */
  protected final void handleHandshake(final @NotNull Channel channel,
                                       final @NotNull String hostname,
                                       final int protocol) throws Exception {
    // Check if the hostname is invalid
    if (hostname.isEmpty()) {
      throw new CorruptedFrameException("Hostname is empty");
    }
    // Check if the player has already sent a handshake packet
    if (protocolVersion != null) {
      throw new CorruptedFrameException("Already sent handshake");
    }
    // Store the protocol version
    protocolVersion = ProtocolVersion.fromId(protocol);
    // Hook the traffic listener
    channel.pipeline().addFirst(FALLBACK_BANDWIDTH, FallbackBandwidthHandler.INSTANCE);
  }

  /**
   * Validates and handles incoming login packets
   *
   * @param channel       Forwarded client channel
   * @param loginPacket   Login packet sent by the client
   * @param username      Username sent by the client
   * @param socketAddress Socket address of the client
   */
  protected final void handleLogin(final @NotNull Channel channel,
                                   final @NotNull ChannelHandlerContext ctx,
                                   final @NotNull Runnable loginPacket,
                                   final @NotNull String username,
                                   final @NotNull InetSocketAddress socketAddress) throws Exception {
    // Check if the player has already sent a login packet
    if (this.username != null) {
      throw new CorruptedFrameException("Already sent login");
    }
    // Connections from unknown protocol versions will be discarded
    // as this is the safest way of handling unwanted connections
    if (protocolVersion.isUnknown()) {
      // Sonar does not support snapshots or Minecraft versions older than 1.7.2
      throw new CorruptedFrameException("Unknown protocol version");
    }
    // Increase joins per second for the action bar verbose
    GlobalSonarStatistics.countLogin();
    // Store the username and IP address
    this.username = username;
    final FallbackInboundHandler inboundHandler = channel.pipeline().get(FallbackInboundHandler.class);
    Objects.requireNonNull(inboundHandler).setInetAddress(socketAddress.getAddress());

    // Check the blacklist here since we cannot let the player "ghost join"
    if (FALLBACK.getBlacklist().asMap().containsKey(inboundHandler.getInetAddress())) {
      customDisconnect(channel, protocolVersion, blacklisted);
      return;
    }

    // Check if the player is already queued since we don't want bots to flood the queue
    if (FALLBACK.getQueue().getPlayers().containsKey(inboundHandler.getInetAddress())) {
      customDisconnect(channel, protocolVersion, alreadyQueued);
      return;
    }

    // Check if Fallback is already verifying a player with the same IP address
    if (FALLBACK.getConnected().containsKey(inboundHandler.getInetAddress())) {
      customDisconnect(channel, protocolVersion, alreadyVerifying);
      return;
    }

    // Don't continue the verification process if the verification is disabled
    if (!Sonar.get().getFallback().shouldVerifyNewPlayers()) {
      initialLogin(ctx, inboundHandler.getInetAddress(), loginPacket);
      return;
    }

    // Completely skip Geyser connections if configured
    final boolean geyser = GeyserUtil.isGeyserConnection(channel, socketAddress);
    if (geyser && !Sonar.get().getConfig().getVerification().isCheckGeyser()) {
      initialLogin(ctx, inboundHandler.getInetAddress(), loginPacket);
      return;
    }

    // Check if the protocol ID of the player is not allowed to enter the server
    if (Sonar.get().getConfig().getVerification().getBlacklistedProtocols()
      .contains(protocolVersion.getProtocol())) {
      customDisconnect(channel, protocolVersion, protocolBlacklisted);
      return;
    }

    // Make sure we actually have to verify the player
    final String offlineUUIDString = "OfflinePlayer:" + username;
    final UUID offlineUUID = UUID.nameUUIDFromBytes(offlineUUIDString.getBytes(StandardCharsets.UTF_8));
    if (Sonar.get().getVerifiedPlayerController().has(inboundHandler.getInetAddress().toString(), offlineUUID)) {
      initialLogin(ctx, inboundHandler.getInetAddress(), loginPacket);
      return;
    }

    // Check if the IP address is currently being rate-limited
    if (!FALLBACK.getRatelimiter().attempt(inboundHandler.getInetAddress())) {
      customDisconnect(channel, protocolVersion, reconnectedTooFast);
      return;
    }

    // Check if the protocol ID of the player is allowed to bypass verification
    if (Sonar.get().getConfig().getVerification().getWhitelistedProtocols()
      .contains(protocolVersion.getProtocol())) {
      initialLogin(ctx, inboundHandler.getInetAddress(), loginPacket);
      return;
    }

    // Remove all other pipelines that could still mess up something
    rewriteProtocol(ctx);

    // Queue the connection for further processing
    FALLBACK.getQueue().getPlayers().compute(inboundHandler.getInetAddress(), (_k, _v) -> () -> {
      // Check if the username matches the valid name regex to prevent
      // UTF-16 names or other types of exploits
      if (!Sonar.get().getConfig().getVerification().getValidNameRegex()
        .matcher(username).matches()) {
        customDisconnect(channel, protocolVersion, invalidUsername);
        return;
      }

      // Create an instance for the Fallback connection
      final FallbackUser user = new FallbackUserWrapper(
        channel, inboundHandler.getInetAddress(), protocolVersion, geyser);
      // Let the verification handler take over the channel
      user.hijack(username, offlineUUID);
    });
  }

  /**
   * Removes all pipelines and rewrites them using our custom handlers
   */
  private static void rewriteProtocol(final @NotNull ChannelHandlerContext ctx) {
    for (final Map.Entry<String, ChannelHandler> entry : ctx.pipeline()) {
      // Don't accidentally remove Sonar's handlers
      if (entry.getKey().startsWith("sonar")
        // Don't remove floodgate's pipelines
        || entry.getKey().startsWith("geyser")
        || entry.getKey().startsWith("floodgate")) {
        continue;
      }
      ctx.pipeline().remove(entry.getValue());
    }
    // Add our custom pipelines
    ctx.pipeline()
      .addFirst(FALLBACK_FRAME_ENCODER, FallbackVarInt21FrameEncoder.INSTANCE)
      .addFirst(FALLBACK_TIMEOUT, new FallbackTimeoutHandler(
        Sonar.get().getConfig().getVerification().getReadTimeout(),
        Sonar.get().getConfig().getVerification().getWriteTimeout(),
        TimeUnit.MILLISECONDS))
      .addFirst(FALLBACK_FRAME_DECODER, new FallbackVarInt21FrameDecoder());
  }

  /**
   * Executes the maximum accounts per IP limit check before letting the player join
   *
   * @param ctx         Forwarded client channel context
   * @param inetAddress Resolved client IP address
   * @param loginPacket Login packet sent by the client
   */
  protected final void initialLogin(final @NotNull ChannelHandlerContext ctx,
                                    final @NotNull InetAddress inetAddress,
                                    final @NotNull Runnable loginPacket) throws Exception {
    // Increment the number of accounts with the same IP
    FALLBACK.getOnline().compute(inetAddress, (k, v) -> v == null ? 1 : v + 1);

    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();
    // Skip the maximum online per IP check if it's disabled in the configuration
    if (maxOnlinePerIp > 0) {
      // Check if the number of online players using the same IP address as
      // the connecting player is greater than the configured amount
      final int onlinePerIp = FALLBACK.getOnline().getOrDefault(inetAddress, 0);
      if (onlinePerIp >= maxOnlinePerIp) {
        customDisconnect(ctx.channel(), protocolVersion, tooManyOnlinePerIP);
        return;
      }
    }

    // Let the server know about the login packet
    loginPacket.run();
  }


  /**
   * Disconnect the player before verification (during login)
   * by replacing the encoder before running the method.
   *
   * @param packet          Disconnect packet
   * @param channel         Channel of the player
   * @param protocolVersion Protocol version of the player
   */
  private void customDisconnect(final @NotNull Channel channel,
                                final @NotNull ProtocolVersion protocolVersion,
                                final @NotNull FallbackPacket packet) {
    if (channel.eventLoop().inEventLoop()) {
      _customDisconnect(channel, protocolVersion, packet);
    } else {
      channel.eventLoop().execute(() -> _customDisconnect(channel, protocolVersion, packet));
    }
  }

  private void _customDisconnect(final @NotNull Channel channel,
                                 final @NotNull ProtocolVersion protocolVersion,
                                 final @NotNull FallbackPacket packet) {
    // Remove the main pipeline to completely take over the channel
    if (channel.pipeline().context(handler) != null) {
      channel.pipeline().remove(handler);
    }
    final ChannelHandler currentEncoder = channel.pipeline().get(encoder);
    // Close the channel if no decoder exists
    if (currentEncoder != null) {
      // We don't need to update the encoder if it's already present
      if (!(currentEncoder instanceof FallbackPacketEncoder)) {
        final FallbackPacketEncoder newEncoder = new FallbackPacketEncoder(protocolVersion);
        channel.pipeline().replace(encoder, FALLBACK_PACKET_ENCODER, newEncoder);
      }
      closeWith(channel, protocolVersion, packet);
    } else {
      channel.close();
    }
  }
}
