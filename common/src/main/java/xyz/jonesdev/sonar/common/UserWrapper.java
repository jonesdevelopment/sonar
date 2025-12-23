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

package xyz.jonesdev.sonar.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.antibot.SonarUser;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyJoinEvent;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.common.netty.MinecraftVarInt21FrameDecoder;
import xyz.jonesdev.sonar.common.netty.MinecraftVarIntLengthEncoder;
import xyz.jonesdev.sonar.common.netty.TailExceptionsHandler;
import xyz.jonesdev.sonar.common.protocol.SonarPacketDecoder;
import xyz.jonesdev.sonar.common.protocol.SonarPacketEncoder;
import xyz.jonesdev.sonar.common.protocol.SonarPacketPreparer;
import xyz.jonesdev.sonar.common.protocol.SonarPacketRegistry;
import xyz.jonesdev.sonar.common.protocol.packets.play.DisconnectPacket;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;
import xyz.jonesdev.sonar.common.verification.LoginHandler;

import java.net.InetAddress;

import static xyz.jonesdev.sonar.api.antibot.ChannelPipelines.*;

@Getter
@ToString(of = {"protocolVersion", "inetAddress", "geyser"})
public final class UserWrapper implements SonarUser {
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

  public UserWrapper(final @NotNull ChannelHandlerContext ctx,
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

      // How? Is there some kind of de-sync or race condition?
      if (channel.pipeline().context(SONAR_FRAME_ENCODER) != null) {
        channel.close(); // Nope ¯\_(ツ)_/¯
        return;
      }

      // Mark the player as connected by caching them in a map of verifying players
      Sonar.get0().getAntiBot().getConnected().compute(inetAddress, (__, v) -> true);

      // Replace normal encoder to allow custom packets
      final SonarPacketEncoder newEncoder = new SonarPacketEncoder(protocolVersion);
      channel.pipeline().addFirst(SONAR_FRAME_ENCODER, MinecraftVarIntLengthEncoder.INSTANCE);
      channel.pipeline().addLast(SONAR_PACKET_ENCODER, newEncoder);

      // Replace normal decoder to allow custom packets
      final SonarPacketDecoder newDecoder = new SonarPacketDecoder(protocolVersion);
      channel.pipeline().addFirst(SONAR_FRAME_DECODER, new MinecraftVarInt21FrameDecoder());
      channel.pipeline().addLast(SONAR_PACKET_DECODER, newDecoder);

      // We're sending the LoginSuccess packet now
      newDecoder.updateRegistry(SonarPacketRegistry.LOGIN);
      newEncoder.updateRegistry(SonarPacketRegistry.LOGIN);
      // Send LoginSuccess packet to make the client think they are joining the server
      write(SonarPacketPreparer.loginSuccess);

      // pre-1.20.2 clients do not have the configuration stage
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        newDecoder.updateRegistry(SonarPacketRegistry.GAME);
        newEncoder.updateRegistry(SonarPacketRegistry.GAME);
      }

      // Listen for all incoming packets by setting the packet listener
      newDecoder.setListener(new LoginHandler(this));

      // Make sure to catch all exceptions during the verification
      channel.pipeline().addLast(SONAR_TAIL_EXCEPTIONS, TailExceptionsHandler.INSTANCE);
    });
  }

  @Override
  public void disconnect(final @NotNull Component reason) {
    final SonarPacketEncoder encoder = channel.pipeline().get(SonarPacketEncoder.class);
    final boolean duringLogin = encoder != null && encoder.getPacketRegistry() == SonarPacketRegistry.LOGIN;
    ProtocolUtil.closeWith(channel, protocolVersion, new DisconnectPacket(reason, duringLogin));
  }
}
