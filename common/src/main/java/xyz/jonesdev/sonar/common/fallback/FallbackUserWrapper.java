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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
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
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.DisconnectPacket;
import xyz.jonesdev.sonar.common.fallback.verification.FallbackPreJoinHandler;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.net.InetAddress;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.*;

@Getter
@ToString(of = {"protocolVersion", "inetAddress", "geyser"})
public final class FallbackUserWrapper implements FallbackUser {
  @Accessors(fluent = true)
  private final Channel channel;
  private final InetAddress inetAddress;
  private final ProtocolVersion protocolVersion;
  private final String fingerprint;
  private final String username;
  private final boolean geyser;
  private final SystemTimer loginTimer;
  @Setter
  private boolean forceCaptcha;

  public FallbackUserWrapper(final @NotNull ChannelHandlerContext ctx,
                             final @NotNull InetAddress inetAddress,
                             final @NotNull ProtocolVersion protocolVersion,
                             final @NotNull String username,
                             final @NotNull String fingerprint,
                             final boolean geyser) {
    this.channel = ctx.channel();
    this.inetAddress = inetAddress;
    this.protocolVersion = protocolVersion;
    this.username = username;
    this.fingerprint = fingerprint;
    this.geyser = geyser;
    this.loginTimer = new SystemTimer();

    GlobalSonarStatistics.totalAttemptedVerifications++;

    if (Sonar.get0().getConfig().getVerification().isLogConnections()
      && (Sonar.get0().getAttackTracker().getCurrentAttack() == null
      || Sonar.get0().getConfig().getVerification().isLogDuringAttack())) {
      Sonar.get0().getLogger().info(
        Sonar.get0().getConfig().getMessagesConfig().getString("verification.logs.connection")
          .replace("<username>", username)
          .replace("<ip>", Sonar.get0().getConfig().formatAddress(inetAddress))
          .replace("<protocol>", protocolVersion.getName()));
    }

    // Call the VerifyJoinEvent for external API usage
    Sonar.get0().getEventManager().publish(new UserVerifyJoinEvent(this));

    // Run this in the channel's event loop to avoid issues
    channel.eventLoop().execute(() -> {
      // Make sure the channel is still active
      if (!channel.isActive()) {
        return;
      }

      // Mark the player as connected by caching them in a map of verifying players
      Sonar.get0().getFallback().getConnected().compute(inetAddress, (__, v) -> true);

      // Replace normal encoder to allow custom packets
      final FallbackPacketEncoder newEncoder = new FallbackPacketEncoder(protocolVersion);
      channel.pipeline().addFirst(FALLBACK_FRAME_ENCODER, FallbackVarIntLengthEncoder.INSTANCE);
      channel.pipeline().addLast(FALLBACK_PACKET_ENCODER, newEncoder);

      // Replace normal decoder to allow custom packets
      final FallbackPacketDecoder newDecoder = new FallbackPacketDecoder(protocolVersion);
      channel.pipeline().addFirst(FALLBACK_FRAME_DECODER, new FallbackVarInt21FrameDecoder());
      channel.pipeline().addLast(FALLBACK_PACKET_DECODER, newDecoder);

      // We're sending the LoginSuccess packet now
      newDecoder.updateRegistry(FallbackPacketRegistry.LOGIN);
      newEncoder.updateRegistry(FallbackPacketRegistry.LOGIN);
      // Send LoginSuccess packet to make the client think they are joining the server
      write(FallbackPreparer.loginSuccess);

      // pre-1.20.2 clients do not have the configuration stage
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        newDecoder.updateRegistry(FallbackPacketRegistry.GAME);
        newEncoder.updateRegistry(FallbackPacketRegistry.GAME);
      }

      // Listen for all incoming packets by setting the packet listener
      newDecoder.setListener(new FallbackPreJoinHandler(this));

      // Make sure to catch all exceptions during the verification
      channel.pipeline().addLast(FALLBACK_TAIL_EXCEPTIONS, FallbackTailExceptionsHandler.INSTANCE);
    });
  }

  @Override
  public void disconnect(final @NotNull Component reason) {
    final FallbackPacketEncoder encoder = channel.pipeline().get(FallbackPacketEncoder.class);
    final boolean duringLogin = encoder != null && encoder.getPacketRegistry() == FallbackPacketRegistry.LOGIN;
    ProtocolUtil.closeWith(channel, protocolVersion, new DisconnectPacket(reason, duringLogin));
  }
}
