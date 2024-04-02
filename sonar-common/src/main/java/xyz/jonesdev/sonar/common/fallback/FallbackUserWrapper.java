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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserBlacklistedEvent;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyFailedEvent;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyJoinEvent;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginSuccessPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.DisconnectPacket;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;

@Getter
@ToString(of = {"protocolVersion", "inetAddress"})
public final class FallbackUserWrapper implements FallbackUser {
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final ProtocolVersion protocolVersion;
  @Setter
  private boolean verified;

  public FallbackUserWrapper(final @NotNull Channel channel,
                             final @NotNull InetAddress inetAddress,
                             final @NotNull ProtocolVersion protocolVersion) {
    this.channel = channel;
    this.pipeline = channel.pipeline();
    this.inetAddress = inetAddress;
    this.protocolVersion = protocolVersion;
  }

  @Override
  public void disconnect(final @NotNull Component reason) {
    closeWith(channel, protocolVersion, DisconnectPacket.create(reason, false));
  }

  @Override
  public void hijack(final @NotNull String username, final @NotNull UUID uuid,
                     final @NotNull String encoder, final @NotNull String decoder,
                     final @NotNull String timeout, final @NotNull String boss) {
    // This rarely happens when the channel hangs, but the player is still connecting
    // This also fixes a unique issue with TCPShield and other reverse proxies
    if (pipeline.get(encoder) == null || pipeline.get(decoder) == null) {
      channel.close();
      return;
    }

    // Remove main pipeline to completely take over the channel
    if (pipeline.get(boss) != null) {
      pipeline.remove(boss);
    }

    // The player has joined the verification
    GlobalSonarStatistics.totalAttemptedVerifications++;

    if (Sonar.get().getConfig().getVerification().isLogConnections()) {
      // Only log the processing message if the server isn't under attack.
      // We let the user override this through the configuration.
      if (!Sonar.get().getAttackTracker().isCurrentlyUnderAttack()
        || Sonar.get().getConfig().getVerification().isLogDuringAttack()) {
        Sonar.get().getFallback().getLogger().info(Sonar.get().getConfig().getVerification().getConnectLog()
          .replace("%name%", username)
          .replace("%ip%", Sonar.get().getConfig().formatAddress(inetAddress))
          .replace("%protocol%", String.valueOf(protocolVersion.getProtocol())));
      }
    }

    // Call the VerifyJoinEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifyJoinEvent(username, this));

    // Mark the player as connected → verifying players
    Sonar.get().getFallback().getConnected().put(username, inetAddress);

    // Add better timeout handler to avoid known exploits or issues
    // We also want to timeout bots quickly to avoid flooding
    final int readTimeout = Sonar.get().getConfig().getVerification().getReadTimeout();
    pipeline.replace(timeout, timeout, new FallbackTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));

    // Replace normal encoder to allow custom packets
    final FallbackPacketEncoder newEncoder = new FallbackPacketEncoder(protocolVersion);
    pipeline.replace(encoder, FALLBACK_PACKET_ENCODER, newEncoder);

    // Send LoginSuccess packet to make the client think they are joining the server
    write(new LoginSuccessPacket(username, uuid));

    // The LoginSuccess packet has been sent, now we can change the registry state
    newEncoder.updateRegistry(protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0
      ? FallbackPacketRegistry.CONFIG : FallbackPacketRegistry.GAME);

    // Replace normal decoder to allow custom packets
    pipeline.replace(decoder, FALLBACK_PACKET_DECODER,
      new FallbackPacketDecoder(this, new FallbackVerificationHandler(this, username, uuid)));
  }

  @Override
  public void fail(final @NotNull String reason) {
    if (channel.isActive()) {
      // Disconnect the player if the channel is open
      disconnect(Sonar.get().getConfig().getVerification().getVerificationFailed());

      // Only log the failed message if the server isn't under attack.
      // We let the user override this through the configuration.
      if (!Sonar.get().getAttackTracker().isCurrentlyUnderAttack()
        || Sonar.get().getConfig().getVerification().isLogDuringAttack()) {
        Sonar.get().getFallback().getLogger().info(Sonar.get().getConfig().getVerification().getFailedLog()
          .replace("%ip%", Sonar.get().getConfig().formatAddress(getInetAddress()))
          .replace("%protocol%", String.valueOf(getProtocolVersion().getProtocol()))
          .replace("%reason%", reason));
      }
    }

    // Increment amount of total failed verifications
    GlobalSonarStatistics.totalFailedVerifications++;

    // Call the VerifyFailedEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifyFailedEvent(this, reason));

    // Use a label, so we can easily add more code beneath this method
    blacklist: {
      // Check if the player has too many failed attempts
      final int blacklistThreshold = Sonar.get().getConfig().getVerification().getBlacklistThreshold();
      // The user is allowed to disable the blacklist entirely by setting the threshold to 0
      if (blacklistThreshold <= 0) break blacklist;

      // Add 1 to the amount of fails since we haven't updated the cached entries yet (for performance)
      final int failCount = getFailedCount() + 1;
      // Now we simply need to check if the threshold is reached
      if (failCount >= blacklistThreshold) {
        // Call the BotBlacklistedEvent for external API usage
        Sonar.get().getEventManager().publish(new UserBlacklistedEvent(this));

        // Increment amount of total blacklisted players
        GlobalSonarStatistics.totalBlacklistedPlayers++;

        Sonar.get().getFallback().getBlacklist().put(getInetAddress(), (byte) 0);
        Sonar.get().getFallback().getLogger().info(Sonar.get().getConfig().getVerification().getBlacklistLog()
          .replace("%ip%", Sonar.get().getConfig().formatAddress(getInetAddress()))
          .replace("%protocol%", String.valueOf(getProtocolVersion().getProtocol())));

        // Invalidate the cached entry to ensure memory safety
        invalidateFails();
        break blacklist;
      }

      // Make sure we increment the number of fails
      incrementFails();
    }
  }

  /**
   * @param channel         Channel to close
   * @param protocolVersion Protocol version
   * @param msg             Packet to close the channel with
   */
  public static void closeWith(final @NotNull Channel channel,
                               final @NotNull ProtocolVersion protocolVersion,
                               final @NotNull Object msg) {
    if (channel.isActive()) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0
        && protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_7_2) >= 0) {
        channel.eventLoop().execute(() -> {
          channel.config().setAutoRead(false);
          channel.eventLoop().schedule(() -> {
            channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
          }, 250L, TimeUnit.MILLISECONDS);
        });
      } else {
        channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  /**
   * Disconnect the player before verification (during login)
   * by replacing the encoder before running the method.
   *
   * @param packet          Disconnect packet
   * @param encoder         Encoder to replace
   * @param boss            Main pipeline to remove
   * @param channel         Channel of the player
   * @param protocolVersion Protocol version of the player
   */
  public static void customDisconnect(final @NotNull Channel channel,
                                      final @NotNull ProtocolVersion protocolVersion,
                                      final @NotNull FallbackPacket packet,
                                      final @NotNull String encoder,
                                      final @NotNull String boss) {
    // Remove main pipeline to completely take over the channel
    if (channel.pipeline().get(boss) != null) {
      channel.pipeline().remove(boss);
    }
    final ChannelHandler currentEncoder = channel.pipeline().get(encoder);
    // Simply close the channel if no decoder exists
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

  /**
   * Removes Sonar's pipelines from a channel to save
   * unnecessary RAM and CPU usage
   *
   * @param pipeline Pipeline object of the channel
   */
  public static void deject(final @NotNull ChannelPipeline pipeline) {
    // Remove Sonar's pipelines if they exist
    if (pipeline.get(FALLBACK_HANDLER) != null) {
      pipeline.remove(FALLBACK_HANDLER);
    }
  }
}
