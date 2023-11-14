/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.api.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.cappuccino.Cappuccino;
import xyz.jonesdev.cappuccino.ExpiringCache;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserBlacklistedEvent;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyFailedEvent;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.statistics.Statistics;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public interface FallbackUser<X, Y> {
  @NotNull Fallback getFallback();

  @NotNull X getPlayer();

  @NotNull Y getConnection();

  @NotNull Channel getChannel();

  @NotNull ChannelPipeline getPipeline();

  @NotNull InetAddress getInetAddress();

  @NotNull ProtocolVersion getProtocolVersion();

  /**
   * Kicks the player from the server with
   * the given disconnect message.
   *
   * @param reason Legacy disconnect message string
   * @see #disconnect(Component)
   */
  default void disconnect(final @NotNull String reason) {
    disconnect(Component.text(reason));
  }

  /**
   * Kicks the player from the server with
   * the given disconnect message.
   *
   * @param reason Disconnect message component
   */
  void disconnect(final @NotNull Component reason);

  /**
   * Sends a packet/message to the player
   *
   * @param msg Message to send to the player
   */
  void write(final @NotNull Object msg);

  /**
   * Queues a buffered message that will be
   * sent once all messages are flushed.
   */
  void delayedWrite(final @NotNull Object msg);

  ExpiringCache<String> PREVIOUS_FAILS = Cappuccino.buildExpiring(3L, TimeUnit.MINUTES);

  /**
   * Disconnects the player who failed the verification
   * and caches them in FAILED_VERIFICATIONS.
   * If the player fails the verification twice,
   * the player will be temporarily denied from verifying.
   *
   * @param reason Reason for failing the verification
   */
  default void fail(final @Nullable String reason) {
    if (getChannel().isActive()) {
      disconnect(Sonar.get().getConfig().getVerification().getVerificationFailed());

      if (reason != null) {
        getFallback().getLogger().info(Sonar.get().getConfig().getVerification().getFailedLog()
          .replace("%ip%", Sonar.get().getConfig().formatAddress(getInetAddress()))
          .replace("%protocol%", String.valueOf(getProtocolVersion().getProtocol()))
          .replace("%reason%", reason));
      }
    }

    Statistics.FAILED_VERIFICATIONS.increment();

    // Call the VerifyFailedEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifyFailedEvent(this, reason));

    // Make sure old entries are removed
    PREVIOUS_FAILS.cleanUp();

    // Check if the player has too many failed attempts
    if (PREVIOUS_FAILS.has(getInetAddress().toString())) {
      // Call the BotBlacklistedEvent for external API usage
      Sonar.get().getEventManager().publish(new UserBlacklistedEvent(this));

      getFallback().getBlacklisted().put(getInetAddress());
      getFallback().getLogger().info(Sonar.get().getConfig().getVerification().getBlacklistLog()
        .replace("%ip%", Sonar.get().getConfig().formatAddress(getInetAddress()))
        .replace("%protocol%", String.valueOf(getProtocolVersion().getProtocol())));
    } else {
      // Cache the InetAddress for 3 minutes
      PREVIOUS_FAILS.put(getInetAddress().toString());
    }
  }
}
