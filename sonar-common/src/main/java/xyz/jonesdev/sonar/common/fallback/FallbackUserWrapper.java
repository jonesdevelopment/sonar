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
import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyJoinEvent;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.statistics.Statistics;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginSuccess;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.Disconnect;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_ENCODER;

@Getter
@ToString(of = {"protocolVersion", "inetAddress"})
public final class FallbackUserWrapper implements FallbackUser {
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final ProtocolVersion protocolVersion;

  public FallbackUserWrapper(final @NotNull Channel channel,
                             final @NotNull InetAddress inetAddress,
                             final @NotNull ProtocolVersion protocolVersion) {
    this.channel = channel;
    this.pipeline = channel.pipeline();
    this.inetAddress = inetAddress;
    this.protocolVersion = protocolVersion;
  }

  /**
   * Disconnect the player during/after verification
   * using our custom {@link Disconnect} packet.
   *
   * @param reason Disconnect message component
   */
  @Override
  public void disconnect(final @NotNull Component reason) {
    closeWith(channel, protocolVersion, Disconnect.create(reason));
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
   * Takes over the channel and begins the verification process
   *
   * @param username Username of the player
   * @param uuid     UUID of the player
   * @param encoder  Name of the encoder pipeline
   * @param decoder  Name of the decoder pipeline
   * @param timeout  Name of the read timeout pipeline
   */
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
    Statistics.REAL_TRAFFIC.increment();

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
    write(new LoginSuccess(username, uuid));

    // The LoginSuccess packet has been sent, now we can change the registry state
    newEncoder.updateRegistry(protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0
      ? FallbackPacketRegistry.CONFIG : FallbackPacketRegistry.GAME);

    // Replace normal decoder to allow custom packets
    pipeline.replace(decoder, FALLBACK_PACKET_DECODER,
      new FallbackPacketDecoder(this, new FallbackVerificationHandler(this, username, uuid)));
  }
}
