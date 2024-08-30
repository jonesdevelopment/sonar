/*
 * Copyright (C) 2024 Sonar Contributors
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
import xyz.jonesdev.sonar.common.fallback.netty.FallbackTailExceptionsHandler;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackVarInt21FrameDecoder;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackVarInt21FrameEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.DisconnectPacket;
import xyz.jonesdev.sonar.common.fallback.session.FallbackLoginSessionHandler;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.CONFIG;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.GAME;

@Getter
@ToString(of = {"protocolVersion", "inetAddress", "geyser"})
public final class FallbackUserWrapper implements FallbackUser {
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final ProtocolVersion protocolVersion;
  private final UUID offlineUuid;
  @Setter
  private boolean receivedClientSettings;
  @Setter
  private boolean receivedPluginMessage;
  private final boolean geyser;
  private final SystemTimer loginTimer = new SystemTimer();

  public FallbackUserWrapper(final @NotNull Channel channel,
                             final @NotNull InetAddress inetAddress,
                             final @NotNull ProtocolVersion protocolVersion,
                             final @NotNull UUID offlineUuid,
                             final boolean geyser) {
    this.channel = channel;
    this.pipeline = channel.pipeline();
    this.inetAddress = inetAddress;
    this.protocolVersion = protocolVersion;
    this.offlineUuid = offlineUuid;
    this.geyser = geyser;
  }

  @Override
  public void disconnect(final @NotNull Component reason) {
    final FallbackPacketEncoder encoder = pipeline.get(FallbackPacketEncoder.class);
    final boolean duringLogin = encoder != null && encoder.getPacketRegistry() != GAME;
    closeWith(channel, protocolVersion, DisconnectPacket.create(reason, duringLogin));
  }

  @Override
  public void hijack(final @NotNull String username, final @NotNull UUID offlineUuid) {
    GlobalSonarStatistics.totalAttemptedVerifications++;

    if (Sonar.get().getConfig().getVerification().isLogConnections()
      && (Sonar.get().getAttackTracker().getCurrentAttack() == null
      || Sonar.get().getConfig().getVerification().isLogDuringAttack())) {
      Sonar.get().getFallback().getLogger().info(
        Sonar.get().getConfig().getMessagesConfig().getString("verification.logs.connection")
          .replace("<username>", username)
          .replace("<ip>", Sonar.get().getConfig().formatAddress(inetAddress))
          .replace("<protocol>", String.valueOf(protocolVersion.getProtocol())));
    }

    // Call the VerifyJoinEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifyJoinEvent(username, this));

    // Run this in the channel's event loop to avoid issues
    channel.eventLoop().execute(() -> {
      // Make sure the channel is still active
      if (!channel.isActive()) {
        return;
      }

      // Mark the player as connected by caching them in a map of verifying players
      Sonar.get().getFallback().getConnected().compute(inetAddress, (_k, _v) -> (byte) 0);

      // Replace normal encoder to allow custom packets
      final FallbackPacketEncoder newEncoder = new FallbackPacketEncoder(protocolVersion);
      pipeline.addFirst(FALLBACK_FRAME_ENCODER, FallbackVarInt21FrameEncoder.INSTANCE);
      pipeline.addLast(FALLBACK_PACKET_ENCODER, newEncoder);

      // Send LoginSuccess packet to make the client think they are joining the server
      write(FallbackPreparer.loginSuccess);

      // The LoginSuccess packet has been sent, now we can change the registry state
      newEncoder.updateRegistry(protocolVersion.compareTo(MINECRAFT_1_20_2) >= 0 ? CONFIG : GAME);

      // Replace normal decoder to allow custom packets
      final FallbackPacketDecoder fallbackPacketDecoder = new FallbackPacketDecoder(protocolVersion);
      pipeline.addFirst(FALLBACK_FRAME_DECODER, new FallbackVarInt21FrameDecoder());
      pipeline.addLast(FALLBACK_PACKET_DECODER, fallbackPacketDecoder);
      // Listen for all incoming packets by setting the packet listener
      fallbackPacketDecoder.setListener(new FallbackLoginSessionHandler(this, username));

      // Make sure to catch all exceptions during the verification
      pipeline.addLast(FALLBACK_TAIL_EXCEPTIONS, FallbackTailExceptionsHandler.INSTANCE);
    });
  }

  @Override
  public void fail(final @NotNull String reason) {
    GlobalSonarStatistics.totalFailedVerifications++;

    disconnect(Sonar.get().getConfig().getVerification().getVerificationFailed());

    final boolean shouldLog = Sonar.get().getAttackTracker().getCurrentAttack() == null
      || Sonar.get().getConfig().getVerification().isLogDuringAttack();

    if (shouldLog) {
      Sonar.get().getFallback().getLogger().info(
        Sonar.get().getConfig().getMessagesConfig().getString("verification.logs.failed")
          .replace("<ip>", Sonar.get().getConfig().formatAddress(getInetAddress()))
          .replace("<protocol>", String.valueOf(getProtocolVersion().getProtocol()))
          .replace("<reason>", reason));
    }

    // Call the VerifyFailedEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifyFailedEvent(this, reason));

    // Use a label, so we can easily add more code beneath this method in the future
    blacklist: {
      // Check if the player has too many failed attempts
      final int limit = Sonar.get().getConfig().getVerification().getBlacklistThreshold();
      // The user is allowed to disable the blacklist entirely by setting the threshold to 0
      if (limit <= 0) break blacklist;

      final String hostAddress = inetAddress.getHostAddress();
      final int score = Sonar.get().getFallback().getBlacklist().get(hostAddress, __ -> 0);

      if (score < limit) {
        Sonar.get().getFallback().getBlacklist().put(hostAddress, score + 1);
        break blacklist;
      }

      GlobalSonarStatistics.totalBlacklistedPlayers++;

      // Call the BotBlacklistedEvent for external API usage
      Sonar.get().getEventManager().publish(new UserBlacklistedEvent(this));

      if (shouldLog) {
        Sonar.get().getFallback().getLogger().info(
          Sonar.get().getConfig().getMessagesConfig().getString("verification.logs.blacklisted")
            .replace("<ip>", Sonar.get().getConfig().formatAddress(getInetAddress()))
            .replace("<protocol>", String.valueOf(getProtocolVersion().getProtocol())));
      }
    }

    // Throw an exception to avoid further code execution
    throw new CorruptedFrameException("Failed the bot verification");
  }

  /**
   * @param channel         Channel to close
   * @param protocolVersion Protocol version
   * @param msg             Packet to close the channel with
   */
  public static void closeWith(final @NotNull Channel channel,
                               final @NotNull ProtocolVersion protocolVersion,
                               final @NotNull Object msg) {
    if (protocolVersion.compareTo(MINECRAFT_1_8) < 0
      && protocolVersion.compareTo(MINECRAFT_1_7_2) >= 0) {
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
