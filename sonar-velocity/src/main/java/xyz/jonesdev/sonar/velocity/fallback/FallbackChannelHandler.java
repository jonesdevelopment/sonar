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

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.FallbackBandwidthHandler;
import xyz.jonesdev.sonar.common.fallback.FallbackChannelHandlerAdapter;
import xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.utility.geyser.GeyserUtil;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.velocitypowered.proxy.network.Connections.*;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_BANDWIDTH;
import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.customDisconnect;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackChannelHandler extends FallbackChannelHandlerAdapter {

  public FallbackChannelHandler(final @NotNull Channel channel) {
    super(channel);
  }

  private @ApiStatus.Internal HandshakePacket handshakePacket;

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx, final Object msg) throws Exception {
    // Intercept any handshake packet by the client
    if (msg instanceof HandshakePacket handshake) {
      handleHandshake(handshake);
    }
    // Intercept any server login packet by the client
    if (msg instanceof ServerLoginPacket serverLogin) {
      handleLogin(ctx, serverLogin);
      return;
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelRead(msg);
  }

  private void handleHandshake(final @NotNull HandshakePacket handshake) throws Exception {
    // Check if the player has already sent a handshake packet
    if (handshakePacket != null) {
      throw new CorruptedFrameException("Already sent handshake");
    }
    // Check if the hostname is invalid
    if (handshake.getServerAddress().isEmpty()) {
      throw new CorruptedFrameException("Hostname is empty");
    }
    // Store the protocol version
    protocolVersion = ProtocolVersion.fromId(handshake.getProtocolVersion().getProtocol());
    // Connections from unknown protocol versions will be discarded
    // as this is the safest way of handling unwanted connections
    if (protocolVersion.isUnknown()) {
      // Sonar does NOT support snapshots or unknown versions;
      // I'll try my best to stay up-to-date!
      throw new CorruptedFrameException("Unknown protocol");
    }
    // Store the handshake packet
    handshakePacket = handshake;
    // Hook the traffic listener
    // TODO: Can we implement this in channelActive?
    channel.pipeline().addFirst(FALLBACK_BANDWIDTH, FallbackBandwidthHandler.INSTANCE);
  }

  private void handleLogin(final @NotNull ChannelHandlerContext ctx,
                           final @NotNull ServerLoginPacket serverLogin) throws Exception {
    // Check if the player has already sent a login packet
    if (username != null) {
      throw new CorruptedFrameException("Already logged on");
    }
    // Increase joins per second for the action bar verbose
    GlobalSonarStatistics.countLogin();
    // Store the username
    username = serverLogin.getUsername();
    // Make sure to use the potentially modified, original IP
    final MinecraftConnection minecraftConnection = (MinecraftConnection) channel.pipeline().get(HANDLER);
    final InetSocketAddress socketAddress = (InetSocketAddress) minecraftConnection.getRemoteAddress();
    inetAddress = socketAddress.getAddress();

    // Check the blacklist here since we cannot let the player "ghost join"
    if (FALLBACK.getBlacklist().asMap().containsKey(inetAddress)) {
      customDisconnect(channel, protocolVersion, blacklisted, MINECRAFT_DECODER, HANDLER);
      return;
    }

    // Don't continue the verification process if the verification is disabled
    if (!Sonar.get().getFallback().shouldVerifyNewPlayers()) {
      ctx.fireChannelRead(serverLogin);
      return;
    }

    // Completely skip Geyser connections
    if (GeyserUtil.isGeyserConnection(channel, socketAddress)) {
      ctx.fireChannelRead(serverLogin);
      return;
    }

    // Check if the player is already queued since we don't want bots to flood the queue
    if (FALLBACK.getQueue().getQueuedPlayers().containsKey(inetAddress)) {
      customDisconnect(channel, protocolVersion, alreadyQueued, MINECRAFT_DECODER, HANDLER);
      return;
    }

    // Check if Fallback is already verifying a player
    // → is another player with the same IP address connected to Fallback?
    if (FALLBACK.getConnected().containsKey(serverLogin.getUsername())
      || FALLBACK.getConnected().containsValue(inetAddress)) {
      customDisconnect(channel, protocolVersion, alreadyVerifying, MINECRAFT_DECODER, HANDLER);
      return;
    }

    // Check if the protocol ID of the player is not allowed to enter the server
    if (Sonar.get().getConfig().getVerification().getBlacklistedProtocols()
      .contains(protocolVersion.getProtocol())) {
      customDisconnect(channel, protocolVersion, protocolBlacklisted, MINECRAFT_DECODER, HANDLER);
      return;
    }

    // Make sure we actually have to verify the player
    final String offlineUUIDString = "OfflinePlayer:" + serverLogin.getUsername();
    final UUID offlineUUID = UUID.nameUUIDFromBytes(offlineUUIDString.getBytes(StandardCharsets.UTF_8));
    if (Sonar.get().getVerifiedPlayerController().has(inetAddress, offlineUUID)) {
      ctx.fireChannelRead(serverLogin);
      return;
    }

    // Check if the IP address is currently being rate-limited
    if (!FALLBACK.getRatelimiter().attempt(inetAddress)) {
      customDisconnect(channel, protocolVersion, reconnectedTooFast, MINECRAFT_DECODER, HANDLER);
      return;
    }

    // Check if the protocol ID of the player is allowed to bypass verification
    if (Sonar.get().getConfig().getVerification().getWhitelistedProtocols()
      .contains(protocolVersion.getProtocol())) {
      ctx.fireChannelRead(serverLogin);
      return;
    }

    // Queue the connection for further processing
    FALLBACK.getQueue().queue(inetAddress, () -> channel.eventLoop().execute(() -> {
      // Check if the username matches the valid name regex to prevent
      // UTF-16 names or other types of exploits
      if (!Sonar.get().getConfig().getVerification().getValidNameRegex()
        .matcher(serverLogin.getUsername()).matches()) {
        customDisconnect(channel, protocolVersion, invalidUsername, MINECRAFT_DECODER, HANDLER);
        return;
      }

      // Create an instance for the Fallback connection
      final FallbackUser user = new FallbackUserWrapper(channel, inetAddress, protocolVersion);
      // Let the verification handler take over the channel
      user.hijack(serverLogin.getUsername(), offlineUUID, MINECRAFT_ENCODER, MINECRAFT_DECODER, READ_TIMEOUT, HANDLER);
    }));
  }
}
