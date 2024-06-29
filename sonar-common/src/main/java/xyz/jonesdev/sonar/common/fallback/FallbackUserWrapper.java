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
import io.netty.handler.codec.CorruptedFrameException;
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
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.DisconnectPacket;
import xyz.jonesdev.sonar.common.fallback.session.FallbackLoginSessionHandler;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_ENCODER;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_20_2;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.CONFIG;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.GAME;

@Getter
@ToString(of = {"protocolVersion", "inetAddress", "geyser"})
public final class FallbackUserWrapper implements FallbackUser {
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final ProtocolVersion protocolVersion;
  @Setter
  private boolean receivedClientSettings;
  @Setter
  private boolean receivedPluginMessage;
  private final boolean geyser;
  private final SystemTimer loginTimer = new SystemTimer();

  public FallbackUserWrapper(final @NotNull Channel channel,
                             final @NotNull InetAddress inetAddress,
                             final @NotNull ProtocolVersion protocolVersion,
                             final boolean geyser) {
    this.channel = channel;
    this.pipeline = channel.pipeline();
    this.inetAddress = inetAddress;
    this.protocolVersion = protocolVersion;
    this.geyser = geyser;
  }

  @Override
  public void disconnect(final @NotNull Component reason) {
    final FallbackPacketEncoder encoder = pipeline.get(FallbackPacketEncoder.class);
    final boolean duringLogin = encoder != null && encoder.getPacketRegistry() != GAME;
    closeWith(channel, protocolVersion, DisconnectPacket.create(reason, duringLogin));
  }

  @Override
  public void hijack(final @NotNull String username, final @NotNull UUID uuid,
                     final @NotNull String encoder, final @NotNull String decoder,
                     final @NotNull String timeout, final @NotNull String handler) {
    // The player has joined the verification
    GlobalSonarStatistics.totalAttemptedVerifications++;

    if (Sonar.get().getConfig().getVerification().isLogConnections()) {
      // Only log the processing message if the server isn't under attack.
      // We let the user override this through the configuration.
      if (Sonar.get().getAttackTracker().getCurrentAttack() == null
        || Sonar.get().getConfig().getVerification().isLogDuringAttack()) {
        Sonar.get().getFallback().getLogger().info(
          Sonar.get().getConfig().getMessagesConfig().getString("verification.logs.connection")
            .replace("<username>", username)
            .replace("<ip>", Sonar.get().getConfig().formatAddress(inetAddress))
            .replace("<protocol>", String.valueOf(protocolVersion.getProtocol())));
      }
    }

    // Call the VerifyJoinEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifyJoinEvent(username, this));

    // Mark the player as connected by caching them in a map of verifying players
    Sonar.get().getFallback().getConnected().compute(inetAddress, (key, value) -> (byte) 0);

    channel.eventLoop().execute(() -> {
      // Add better timeout handler to avoid known exploits or issues
      // We also want to timeout bots quickly to avoid flooding
      pipeline.replace(timeout, timeout, new FallbackTimeoutHandler(
        Sonar.get().getConfig().getVerification().getReadTimeout(),
        Sonar.get().getConfig().getVerification().getWriteTimeout(),
        TimeUnit.MILLISECONDS));

      // Replace normal encoder to allow custom packets
      final FallbackPacketEncoder newEncoder = new FallbackPacketEncoder(protocolVersion);
      pipeline.replace(encoder, FALLBACK_PACKET_ENCODER, newEncoder);

      // Remove the main pipeline to completely take over the channel
      if (pipeline.get(handler) != null) {
        pipeline.remove(handler);
      }

      // Send LoginSuccess packet to make the client think they are joining the server
      write(FallbackPreparer.LOGIN_SUCCESS);

      // The LoginSuccess packet has been sent, now we can change the registry state
      newEncoder.updateRegistry(protocolVersion.compareTo(MINECRAFT_1_20_2) >= 0 ? CONFIG : GAME);

      // Replace normal decoder to allow custom packets
      final FallbackPacketDecoder fallbackPacketDecoder = new FallbackPacketDecoder(protocolVersion);
      pipeline.replace(decoder, FALLBACK_PACKET_DECODER, fallbackPacketDecoder);
      // Listen for all incoming packets by setting the packet listener
      fallbackPacketDecoder.setListener(new FallbackLoginSessionHandler(this, username, uuid));
    });
  }

  @Override
  public void fail(final @NotNull String reason) {
    disconnect(Sonar.get().getConfig().getVerification().getVerificationFailed());

    // Only log the failed message if the server isn't currently under attack.
    // However, we let the user override this through the configuration.
    final boolean shouldLog = Sonar.get().getAttackTracker().getCurrentAttack() == null
      || Sonar.get().getConfig().getVerification().isLogDuringAttack();

    if (shouldLog) {
      Sonar.get().getFallback().getLogger().info(
        Sonar.get().getConfig().getMessagesConfig().getString("verification.logs.failed")
          .replace("<ip>", Sonar.get().getConfig().formatAddress(getInetAddress()))
          .replace("<protocol>", String.valueOf(getProtocolVersion().getProtocol()))
          .replace("<reason>", reason));
    }

    // Increment number of total failed verifications
    GlobalSonarStatistics.totalFailedVerifications++;

    // Call the VerifyFailedEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifyFailedEvent(this, reason));

    // Use a label, so we can easily add more code beneath this method in the future
    blacklist: {
      // Check if the player has too many failed attempts
      final int blacklistThreshold = Sonar.get().getConfig().getVerification().getBlacklistThreshold();
      // The user is allowed to disable the blacklist entirely by setting the threshold to 0
      if (blacklistThreshold <= 0) break blacklist;

      // Use 1 as the default amount of fails since we haven't cached anything yet
      final int fails = Sonar.get().getFallback().getRatelimiter().getFailCountCache().get(inetAddress, ignored -> 1);
      // Now we simply need to check if the threshold is reached
      if (fails < blacklistThreshold) {
        // Make sure we increment the number of fails
        Sonar.get().getFallback().getRatelimiter().incrementFails(inetAddress, fails);
        break blacklist;
      }

      // Call the BotBlacklistedEvent for external API usage
      Sonar.get().getEventManager().publish(new UserBlacklistedEvent(this));

      // Increment number of total blacklisted players
      GlobalSonarStatistics.totalBlacklistedPlayers++;

      Sonar.get().getFallback().getBlacklist().put(getInetAddress(), (byte) 0);

      if (shouldLog) {
        Sonar.get().getFallback().getLogger().info(
          Sonar.get().getConfig().getMessagesConfig().getString("verification.logs.blacklisted")
            .replace("<ip>", Sonar.get().getConfig().formatAddress(getInetAddress()))
            .replace("<protocol>", String.valueOf(getProtocolVersion().getProtocol())));
      }

      // Invalidate the cached entry to ensure memory safety
      Sonar.get().getFallback().getRatelimiter().getFailCountCache().invalidate(inetAddress);
    }

    // Throw an exception to avoid further code execution
    throw new CorruptedFrameException();
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
   * @param handler         Main pipeline to remove
   * @param channel         Channel of the player
   * @param protocolVersion Protocol version of the player
   */
  public static void customDisconnect(final @NotNull Channel channel,
                                      final @NotNull ProtocolVersion protocolVersion,
                                      final @NotNull FallbackPacket packet,
                                      final @NotNull String encoder,
                                      final @NotNull String handler) {
    if (channel.eventLoop().inEventLoop()) {
      _customDisconnect(channel, protocolVersion, packet, encoder, handler);
    } else {
      channel.eventLoop().execute(() -> _customDisconnect(channel, protocolVersion, packet, encoder, handler));
    }
  }

  private static void _customDisconnect(final @NotNull Channel channel,
                                        final @NotNull ProtocolVersion protocolVersion,
                                        final @NotNull FallbackPacket packet,
                                        final @NotNull String encoder,
                                        final @NotNull String boss) {
    // Remove the main pipeline to completely take over the channel
    if (channel.pipeline().context(boss) != null) {
      channel.pipeline().remove(boss);
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
