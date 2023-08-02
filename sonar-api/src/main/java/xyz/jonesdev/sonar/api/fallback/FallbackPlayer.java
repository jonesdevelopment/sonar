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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.cappuchino.Cappuchino;
import xyz.jonesdev.cappuchino.ExpiringCache;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.statistics.Statistics;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public interface FallbackPlayer<X, Y> {
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
   * @param reason Disconnect message
   */
  void disconnect(final @NotNull String reason);

  /**
   * Sends a packet/message to the player
   *
   * @param msg Message to send to the player
   */
  void write(final @NotNull Object msg);

  ExpiringCache<String> PREVIOUS_FAILS = Cappuchino.buildExpiring(3L, TimeUnit.MINUTES);

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
      disconnect(getFallback().getSonar().getConfig().VERIFICATION_FAILED);

      if (reason != null) {
        getFallback().getLogger().info("{} ({}) has failed the bot check for: {}",
          getInetAddress(), getProtocolVersion().getProtocol(), reason);
      }
    }

    Statistics.FAILED_VERIFICATIONS.increment();

    // Make sure old entries are removed
    PREVIOUS_FAILS.cleanUp();

    // Check if the player has too many failed attempts
    if (PREVIOUS_FAILS.has(getInetAddress().toString())) {
      getFallback().getBlacklisted().put(getInetAddress().toString());
      getFallback().getLogger().info("{} ({}) was blacklisted for too many failed attempts",
        getInetAddress(), getProtocolVersion().getProtocol());
    } else {
      // Cache the InetAddress for 3 minutes
      PREVIOUS_FAILS.put(getInetAddress().toString());
    }
  }
}
