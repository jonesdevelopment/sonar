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

package xyz.jonesdev.sonar.common.fallback.verification;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.database.model.VerifiedPlayer;
import xyz.jonesdev.sonar.api.event.impl.UserBlacklistedEvent;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyFailedEvent;
import xyz.jonesdev.sonar.api.event.impl.UserVerifySuccessEvent;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackVarInt21FrameDecoder;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackVarIntLengthEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketListener;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import java.util.Random;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.transferToOrigin;

@RequiredArgsConstructor
public abstract class FallbackVerificationHandler implements FallbackPacketListener {
  protected final FallbackUser user;

  protected static final Random RANDOM = new Random();

  protected final void finishVerification() {
    GlobalSonarStatistics.totalSuccessfulVerifications++;

    // Add verified player to the database
    Sonar.get0().getVerifiedPlayerController().add(new VerifiedPlayer(
      user.getFingerprint(), user.getLoginTimer().getStart()));

    // Call the VerifySuccessEvent for external API usage
    Sonar.get0().getEventManager().publish(new UserVerifySuccessEvent(user));

    // If enabled, transfer the player back to the origin server.
    // This feature was introduced by Mojang in Minecraft version 1.20.5.
    if (transferToOrigin != null && user.getProtocolVersion().greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20_5)) {
      // Send the transfer packet to the player (and close the channel if on Java Edition)
      if (user.isGeyser()) {
        user.write(transferToOrigin);
        // Make sure we cannot receive any more packets from the player
        user.channel().pipeline().remove(FallbackPacketDecoder.class);
        user.channel().pipeline().remove(FallbackPacketEncoder.class);
        user.channel().pipeline().remove(FallbackVarInt21FrameDecoder.class);
        user.channel().pipeline().remove(FallbackVarIntLengthEncoder.class);
      } else {
        ProtocolUtil.closeWith(user.channel(), user.getProtocolVersion(), transferToOrigin);
      }
    } else {
      // Disconnect player with the verification success message
      user.disconnect(Sonar.get0().getConfig().getVerification().getVerificationSuccess());
    }

    Sonar.get0().getLogger().info(
      Sonar.get0().getConfig().getMessagesConfig().getString("verification.logs.successful")
        .replace("<username>", user.getUsername())
        .replace("<time-taken>", user.getLoginTimer().toString()));
  }

  protected final void fail(final @NotNull String reason) {
    GlobalSonarStatistics.totalFailedVerifications++;

    user.disconnect(Sonar.get0().getConfig().getVerification().getVerificationFailed());

    final boolean shouldLog = Sonar.get0().getAttackTracker().getCurrentAttack() == null
      || Sonar.get0().getConfig().getVerification().isLogDuringAttack();

    if (shouldLog) {
      Sonar.get0().getLogger().info(
        Sonar.get0().getConfig().getMessagesConfig().getString("verification.logs.failed")
          .replace("<username>", user.getUsername())
          .replace("<ip>", Sonar.get0().getConfig().formatAddress(user.getInetAddress()))
          .replace("<protocol>", user.getProtocolVersion().getName())
          .replace("<reason>", reason));
    }

    // Call the VerifyFailedEvent for external API usage
    Sonar.get0().getEventManager().publish(new UserVerifyFailedEvent(user, reason));

    // Use a label, so we can easily add more code beneath this method in the future
    blacklist: {
      final String hostAddress = user.getInetAddress().getHostAddress();
      final int score = Sonar.get0().getFallback().getBlacklist().get(hostAddress, __ -> 0);
      final int newScore = score + 1;

      Sonar.get0().getFallback().getBlacklist().put(hostAddress, newScore);

      // The user is allowed to disable the blacklist entirely by setting the threshold to 0
      final int limit = Sonar.get0().getConfig().getVerification().getBlacklistThreshold();
      // The player hasn't been blacklisted yet, so skip this iteration
      if (newScore < limit) break blacklist;

      GlobalSonarStatistics.totalBlacklistedPlayers++;

      // Call the BotBlacklistedEvent for external API usage
      Sonar.get0().getEventManager().publish(new UserBlacklistedEvent(user));

      if (shouldLog) {
        Sonar.get0().getLogger().info(
          Sonar.get0().getConfig().getMessagesConfig().getString("verification.logs.blacklisted")
            .replace("<username>", user.getUsername())
            .replace("<ip>", Sonar.get0().getConfig().formatAddress(user.getInetAddress()))
            .replace("<protocol>", user.getProtocolVersion().getName()));
      }
    }

    // Throw an exception to avoid further code execution
    throw QuietDecoderException.INSTANCE;
  }

  protected final void checkState(final boolean state, final String failReason) {
    // Fails the verification if the condition is not met
    if (!state) {
      fail(failReason);
    }
  }
}
