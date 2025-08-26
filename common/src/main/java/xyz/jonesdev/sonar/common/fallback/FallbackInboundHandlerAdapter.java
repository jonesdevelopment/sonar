/*
 * Copyright (C) 2025 Sonar Contributors
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

import io.netty.channel.*;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.fingerprint.FingerprintingUtil;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackTimeoutHandler;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.util.GeyserUtil;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

@AllArgsConstructor
@RequiredArgsConstructor
public abstract class FallbackInboundHandlerAdapter extends ChannelInboundHandlerAdapter {
  protected @Nullable String username;
  protected ProtocolVersion protocolVersion;
  protected @Nullable RemovalListener channelRemovalListener;

  /**
   * Validates and handles incoming handshake packets
   */
  protected final void handleHandshake(final @NotNull ChannelHandlerContext ctx,
                                       final @NotNull String hostname,
                                       final int protocol) throws Exception {
    // Check if the hostname is invalid
    if (hostname.isEmpty()) {
      throw QuietDecoderException.INSTANCE;
    }
    // Check if the player has already sent a handshake packet
    if (protocolVersion != null) {
      throw QuietDecoderException.INSTANCE;
    }
    protocolVersion = ProtocolVersion.fromId(protocol);
    ctx.pipeline().addFirst(FALLBACK_BANDWIDTH, FallbackBandwidthHandler.INSTANCE);
  }

  /**
   * Validates and handles incoming login packets
   */
  protected final void handleLogin(final @NotNull ChannelHandlerContext ctx,
                                   final @NotNull Runnable initialLoginAction,
                                   final @NotNull String username,
                                   final @NotNull InetSocketAddress socketAddress) throws Exception {
    // Count every single attempted login
    GlobalSonarStatistics.countLogin();
    // Ensure that the player sent a handshake packet
    if (protocolVersion == null) {
      throw QuietDecoderException.INSTANCE;
    }
    // Connections from unknown protocol versions will be discarded
    // as this is the safest way of handling unwanted connections.
    // Sonar does not support snapshots or Minecraft versions older than 1.7.2
    if (protocolVersion.isUnknown()) {
      customDisconnect(ctx.channel(), unsupportedVersion, ProtocolVersion.MINECRAFT_1_7_2);
      return;
    }
    // Ensure that the player has not sent a login packet yet
    if (this.username != null) {
      throw QuietDecoderException.INSTANCE;
    }
    this.username = username;

    final InetAddress inetAddress = socketAddress.getAddress();
    ctx.pipeline().get(FallbackInboundHandler.class).setInetAddress(inetAddress);

    // Check if Fallback is already verifying a player with the same IP address
    if (Sonar.get0().getFallback().getConnected().containsKey(inetAddress)) {
      customDisconnect(ctx.channel(), alreadyVerifying, protocolVersion);
      return;
    }

    // Check if the protocol ID of the player is not allowed to enter the server
    if (Sonar.get0().getConfig().getVerification().getBlacklistedProtocols().contains(protocolVersion.getProtocol())) {
      customDisconnect(ctx.channel(), protocolBlacklisted, protocolVersion);
      return;
    }

    final String hostAddress = inetAddress.getHostAddress();
    // Check if the player failed the verification too many times
    final int limit = Sonar.get0().getConfig().getVerification().getBlacklistThreshold();
    if (limit > 0) {
      final int score = Sonar.get0().getFallback().getBlacklist().asMap().getOrDefault(hostAddress, 0);
      if (score >= limit) {
        customDisconnect(ctx.channel(), blacklisted, protocolVersion);
        return;
      }
    }

    // Don't continue the verification process if the verification is disabled
    if (!Sonar.get0().getFallback().shouldVerifyNewPlayers()) {
      initialLogin(ctx.channel(), inetAddress, initialLoginAction);
      return;
    }

    // Completely skip Geyser connections if configured
    final boolean geyser = GeyserUtil.isGeyserConnection(ctx.channel(), socketAddress);
    if (geyser && !Sonar.get0().getConfig().getVerification().isCheckGeyser()) {
      initialLogin(ctx.channel(), inetAddress, initialLoginAction);
      return;
    }

    // Make sure we actually have to verify the player
    final String fingerprint = FingerprintingUtil.getFingerprint(username, hostAddress);
    if (Sonar.get0().getVerifiedPlayerController().getCache().contains(fingerprint)) {
      initialLogin(ctx.channel(), inetAddress, initialLoginAction);
      return;
    }

    // Check if the IP address is currently being rate-limited
    if (!Sonar.get0().getFallback().getRatelimiter().attempt(inetAddress)) {
      customDisconnect(ctx.channel(), reconnectedTooFast, protocolVersion);
      return;
    }

    // Remove all other pipelines that could still mess up something
    rewriteProtocol(ctx, channelRemovalListener);

    // Queue the connection for further processing
    Sonar.get0().getFallback().getQueue().getPlayers().compute(inetAddress, (__, runnable) -> {
      // Check if the player is already queued since we don't want bots to flood the queue
      if (runnable != null) {
        customDisconnect(ctx.channel(), alreadyQueued, protocolVersion);
        // Remove other instances of this IP address from the queue
        return null;
      }

      // Create an instance for the user and let the verification handler take over the channel
      return () -> new FallbackUserWrapper(ctx, inetAddress, protocolVersion, username, fingerprint, geyser);
    });
  }

  /**
   * Executes the maximum accounts per IP limit check before letting the player join
   */
  protected final void initialLogin(final @NotNull Channel channel,
                                    final @NotNull InetAddress inetAddress,
                                    final @NotNull Runnable loginPacket) throws Exception {
    final int maxOnlinePerIp = Sonar.get0().getConfig().getMaxOnlinePerIp();
    final int newCount = Sonar.get0().getFallback().getOnline().compute(inetAddress,
      (__, count) -> count == null ? 1 : count + 1);
    if (newCount > maxOnlinePerIp) {
      customDisconnect(channel, tooManyOnlinePerIP, protocolVersion);
      return;
    }
    loginPacket.run();
  }

  /**
   * Removes all pipelines and rewrites them using our custom handlers
   */
  private static void rewriteProtocol(final @NotNull ChannelHandlerContext ctx,
                                      final @Nullable RemovalListener removalListener) {
    for (final Map.Entry<String, ChannelHandler> entry : ctx.pipeline()) {
      // Don't accidentally remove Sonar's handlers
      if (entry.getKey().startsWith("sonar")
        // Don't remove floodgate's pipelines
        || entry.getKey().startsWith("geyser")
        || entry.getKey().startsWith("floodgate")
        || entry.getKey().equals("DefaultChannelPipeline$TailContext#0")) {
        continue;
      }
      ctx.pipeline().remove(entry.getValue());
      if (removalListener != null) {
        removalListener.accept(ctx.pipeline(), entry.getKey(), entry.getValue());
      }
    }
    // Add our read/write timeout handler
    ctx.pipeline().addFirst(FALLBACK_TIMEOUT, new FallbackTimeoutHandler(
      Sonar.get0().getConfig().getVerification().getReadTimeout(),
      Sonar.get0().getConfig().getVerification().getWriteTimeout(),
      TimeUnit.MILLISECONDS));
  }

  private static void customDisconnect(final @NotNull Channel channel,
                                       final @NotNull FallbackPacket packet,
                                       final @NotNull ProtocolVersion protocolVersion) {
    if (channel.isActive()) {
      if (channel.eventLoop().inEventLoop()) {
        _customDisconnect(channel, packet, protocolVersion);
      } else {
        channel.eventLoop().execute(() -> _customDisconnect(channel, packet, protocolVersion));
      }
    }
  }

  /**
   * Disconnect the player before verification (during login)
   */
  private static void _customDisconnect(final @NotNull Channel channel,
                                        final @NotNull FallbackPacket packet,
                                        final @NotNull ProtocolVersion protocolVersion) {
    // TODO: recode this
    // Remove the connection handler pipeline to completely take over the channel
    final String handler = Sonar.get0().getPlatform().getConnectionHandler();
    if (channel.pipeline().context(handler) != null) {
      channel.pipeline().remove(handler);
    }
    final String encoder = Sonar.get0().getPlatform().getEncoder().apply(channel.pipeline());
    final ChannelHandler currentEncoder = channel.pipeline().get(encoder);
    // Close the channel if no encoder exists
    if (currentEncoder != null) {
      // We don't need to update the encoder if it has already been replaced by Sonar
      if (!(currentEncoder instanceof FallbackPacketEncoder)) {
        final FallbackPacketEncoder newEncoder = new FallbackPacketEncoder(protocolVersion);
        newEncoder.updateRegistry(FallbackPacketRegistry.LOGIN);
        channel.pipeline().replace(encoder, FALLBACK_PACKET_ENCODER, newEncoder);
      }
      ProtocolUtil.closeWith(channel, protocolVersion, packet);
    } else {
      channel.close();
    }
  }

  @FunctionalInterface
  public interface RemovalListener {
    void accept(final @NotNull ChannelPipeline pipeline,
                final @NotNull String name,
                final @NotNull ChannelHandler handler);
  }
}
