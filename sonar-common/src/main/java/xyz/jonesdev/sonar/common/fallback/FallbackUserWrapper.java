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
import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyJoinEvent;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackTailExceptionsHandler;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackVarInt21FrameDecoder;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackVarIntLengthEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.DisconnectPacket;
import xyz.jonesdev.sonar.common.fallback.verification.FallbackPreJoinHandler;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.net.InetAddress;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_20_2;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.CONFIG;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry.GAME;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.loginSuccess;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.closeWith;

@Getter
@ToString(of = {"protocolVersion", "inetAddress", "geyser"})
public final class FallbackUserWrapper implements FallbackUser {
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final ProtocolVersion protocolVersion;
  private final String fingerprint;
  private final String username;
  private final boolean geyser;
  private final SystemTimer loginTimer;
  @Setter
  private boolean forceCaptcha;

  public FallbackUserWrapper(final @NotNull Channel channel,
                             final @NotNull InetAddress inetAddress,
                             final @NotNull ProtocolVersion protocolVersion,
                             final @NotNull String username,
                             final @NotNull String fingerprint,
                             final boolean geyser) {
    this.channel = channel;
    this.pipeline = channel.pipeline();
    this.inetAddress = inetAddress;
    this.protocolVersion = protocolVersion;
    this.username = username;
    this.fingerprint = fingerprint;
    this.geyser = geyser;
    this.loginTimer = new SystemTimer();

    GlobalSonarStatistics.totalAttemptedVerifications++;

    if (Sonar.get().getConfig().getVerification().isLogConnections()
      && (Sonar.get().getAttackTracker().getCurrentAttack() == null
      || Sonar.get().getConfig().getVerification().isLogDuringAttack())) {
      Sonar.get().getLogger().info(
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
      Sonar.get().getFallback().getConnected().compute(inetAddress, (__, v) -> (byte) 0);

      // Replace normal encoder to allow custom packets
      final FallbackPacketEncoder newEncoder = new FallbackPacketEncoder(protocolVersion);
      pipeline.addFirst(FALLBACK_FRAME_ENCODER, FallbackVarIntLengthEncoder.INSTANCE);
      pipeline.addLast(FALLBACK_PACKET_ENCODER, newEncoder);

      // Send LoginSuccess packet to make the client think they are joining the server
      write(loginSuccess);

      // The LoginSuccess packet has been sent, now we can change the registry state
      newEncoder.updateRegistry(protocolVersion.compareTo(MINECRAFT_1_20_2) >= 0 ? CONFIG : GAME);

      // Replace normal decoder to allow custom packets
      final FallbackPacketDecoder newDecoder = new FallbackPacketDecoder(protocolVersion);
      pipeline.addFirst(FALLBACK_FRAME_DECODER, new FallbackVarInt21FrameDecoder());
      pipeline.addLast(FALLBACK_PACKET_DECODER, newDecoder);
      // Listen for all incoming packets by setting the packet listener
      newDecoder.setListener(new FallbackPreJoinHandler(this));

      // Make sure to catch all exceptions during the verification
      pipeline.addLast(FALLBACK_TAIL_EXCEPTIONS, FallbackTailExceptionsHandler.INSTANCE);
    });
  }

  @Override
  public void disconnect(final @NotNull Component reason) {
    final FallbackPacketEncoder encoder = pipeline.get(FallbackPacketEncoder.class);
    final boolean duringLogin = encoder != null && encoder.getPacketRegistry() != GAME;
    closeWith(channel, protocolVersion, DisconnectPacket.create(reason, duringLogin));
  }
}
