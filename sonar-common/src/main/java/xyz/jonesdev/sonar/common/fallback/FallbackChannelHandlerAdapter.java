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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_BANDWIDTH;
import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.customDisconnect;
import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.deject;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

@RequiredArgsConstructor
public class FallbackChannelHandlerAdapter extends ChannelInboundHandlerAdapter {
  protected final Channel channel;
  protected @Nullable String username;
  protected InetAddress inetAddress;
  protected ProtocolVersion protocolVersion;
  protected @Nullable FallbackUser user;

  protected static final Fallback FALLBACK = Sonar.get().getFallback();

  @Override
  public final void channelActive(final @NotNull ChannelHandlerContext ctx) {
    // Increase connections per second for the action bar verbose
    GlobalSonarStatistics.countConnection();
    // Make sure to let the server handle the rest
    ctx.fireChannelActive();
  }

  @Override
  public final void channelInactive(final @NotNull ChannelHandlerContext ctx) {
    // The player can disconnect without sending the login packet first
    if (username != null) {
      // Remove the username from the connected players
      FALLBACK.getConnected().remove(username);
    }
    // The player cannot be in the queue if the IP address is invalid
    if (inetAddress != null) {
      // Remove the IP address from the queue
      FALLBACK.getQueue().getQueuedPlayers().remove(inetAddress);
    }
    // The player cannot be in the verification if the user object doesn't exist,
    // or if the user failed/succeeded the bot verification process.
    if (user != null && user.getState().canReceivePackets()) {
      // Mark the verification attempt as failed
      user.fail("disconnected during verification");
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelInactive();
  }

  // We can override the default exceptionCaught method since this handler
  // will run before the connection knows that there has been an error.
  // Additionally, this will also run after our custom decoder.
  @Override
  public final void exceptionCaught(final @NotNull ChannelHandlerContext ctx,
                                    final @NotNull Throwable cause) throws Exception {
    // Simply close the channel if we encounter any errors.
    ctx.close();
  }

  /**
   * Validates and handles incoming handshake packets
   *
   * @param hostname Hostname (server address) sent by the client
   * @param protocol Protocol version number sent by the client
   */
  protected final void handleHandshake(final @NotNull String hostname, final int protocol) throws Exception {
    // Check if the player has already sent a handshake packet
    if (protocolVersion != null) {
      throw new CorruptedFrameException("Already sent handshake");
    }
    // Check if the hostname is invalid
    if (hostname.isEmpty()) {
      throw new CorruptedFrameException("Hostname is empty");
    }
    // Store the protocol version
    protocolVersion = ProtocolVersion.fromId(protocol);
    // Connections from unknown protocol versions will be discarded
    // as this is the safest way of handling unwanted connections
    if (protocolVersion.isUnknown()) {
      // Sonar does NOT support snapshots or unknown versions;
      // I'll try my best to stay up-to-date!
      throw new CorruptedFrameException("Unknown protocol version " + protocol);
    }
    // Hook the traffic listener
    // TODO: Can we implement this in channelActive?
    channel.pipeline().addFirst(FALLBACK_BANDWIDTH, FallbackBandwidthHandler.INSTANCE);
  }

  /**
   * Validates and handles incoming login packets
   *
   * @param ctx           Forwarded channel handler context
   * @param loginPacket   Login packet sent by the client
   * @param username      Username sent by the client
   * @param socketAddress Socket address of the client
   */
  protected final void handleLogin(final @NotNull ChannelHandlerContext ctx,
                                   final @NotNull Object loginPacket,
                                   final @NotNull String username,
                                   final @NotNull InetSocketAddress socketAddress,
                                   final @NotNull String encoder,
                                   final @NotNull String decoder,
                                   final @NotNull String timeout,
                                   final @NotNull String handler) throws Exception {
    // Check if the player has already sent a login packet
    if (this.username != null) {
      throw new CorruptedFrameException("Already sent login");
    }
    // Increase joins per second for the action bar verbose
    GlobalSonarStatistics.countLogin();
    // Store the username and IP address
    this.username = username;
    this.inetAddress = socketAddress.getAddress();

    // Check the blacklist here since we cannot let the player "ghost join"
    if (FALLBACK.getBlacklist().asMap().containsKey(inetAddress)) {
      customDisconnect(channel, protocolVersion, blacklisted, encoder, handler);
      return;
    }

    // Don't continue the verification process if the verification is disabled
    if (!Sonar.get().getFallback().shouldVerifyNewPlayers()) {
      initialLogin(ctx, loginPacket, encoder, handler);
      return;
    }

    // Completely skip Geyser connections
    if (GeyserUtil.isGeyserConnection(channel, socketAddress)) {
      initialLogin(ctx, loginPacket, encoder, handler);
      return;
    }

    // Check if the player is already queued since we don't want bots to flood the queue
    if (FALLBACK.getQueue().getQueuedPlayers().containsKey(inetAddress)) {
      customDisconnect(channel, protocolVersion, alreadyQueued, encoder, handler);
      return;
    }

    // Check if Fallback is already verifying a player
    // → is another player with the same IP address connected to Fallback?
    if (FALLBACK.getConnected().containsKey(username)
      || FALLBACK.getConnected().containsValue(inetAddress)) {
      customDisconnect(channel, protocolVersion, alreadyVerifying, encoder, handler);
      return;
    }

    // Check if the protocol ID of the player is not allowed to enter the server
    if (Sonar.get().getConfig().getVerification().getBlacklistedProtocols()
      .contains(protocolVersion.getProtocol())) {
      customDisconnect(channel, protocolVersion, protocolBlacklisted, encoder, handler);
      return;
    }

    // Make sure we actually have to verify the player
    final String offlineUUIDString = "OfflinePlayer:" + username;
    final UUID offlineUUID = UUID.nameUUIDFromBytes(offlineUUIDString.getBytes(StandardCharsets.UTF_8));
    if (Sonar.get().getVerifiedPlayerController().has(inetAddress, offlineUUID)) {
      initialLogin(ctx, loginPacket, encoder, handler);
      return;
    }

    // Check if the IP address is currently being rate-limited
    if (!FALLBACK.getRatelimiter().attempt(inetAddress)) {
      customDisconnect(channel, protocolVersion, reconnectedTooFast, encoder, handler);
      return;
    }

    // Check if the protocol ID of the player is allowed to bypass verification
    if (Sonar.get().getConfig().getVerification().getWhitelistedProtocols()
      .contains(protocolVersion.getProtocol())) {
      initialLogin(ctx, loginPacket, encoder, handler);
      return;
    }

    // Queue the connection for further processing
    FALLBACK.getQueue().getQueuedPlayers().put(inetAddress, () -> {
      // Check if the username matches the valid name regex to prevent
      // UTF-16 names or other types of exploits
      if (!Sonar.get().getConfig().getVerification().getValidNameRegex()
        .matcher(username).matches()) {
        customDisconnect(channel, protocolVersion, invalidUsername, encoder, handler);
        return;
      }

      // Create an instance for the Fallback connection
      user = new FallbackUserWrapper(channel, inetAddress, protocolVersion);
      // Let the verification handler take over the channel
      user.hijack(username, offlineUUID, encoder, decoder, timeout, handler);
    });
  }

  /**
   * Executes the maximum accounts per IP limit check before letting the player join
   *
   * @param ctx         Forwarded channel handler context
   * @param loginPacket Login packet sent by the client
   */
  protected final void initialLogin(final @NotNull ChannelHandlerContext ctx,
                                    final @NotNull Object loginPacket,
                                    final @NotNull String encoder,
                                    final @NotNull String handler) throws Exception {
    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();
    // Skip the maximum online per IP check if it's disabled in the configuration
    if (maxOnlinePerIp > 0) {
      // Check if the number of online players using the same IP address as
      // the connecting player is greater than the configured amount
      if (Sonar.get().hasTooManyAccounts(inetAddress, maxOnlinePerIp)) {
        customDisconnect(channel, protocolVersion, FallbackPreparer.tooManyOnlinePerIP, encoder, handler);
        return;
      }
    }

    ctx.fireChannelRead(loginPacket);
    // Deject the channel since we don't need it anymore
    deject(channel.pipeline());
  }
}
