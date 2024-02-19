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

package xyz.jonesdev.sonar.api.fallback;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserBlacklistedEvent;
import xyz.jonesdev.sonar.api.event.impl.UserVerifyFailedEvent;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.api.statistics.Statistics;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public interface FallbackUser {
  @NotNull Channel getChannel();

  @NotNull InetAddress getInetAddress();

  @NotNull ProtocolVersion getProtocolVersion();

  /**
   * Disconnect the player during/after verification
   * using our custom Disconnect packet.
   *
   * @param reason Disconnect message component
   */
  void disconnect(final @NotNull Component reason);

  /**
   * Takes over the channel and begins the verification process
   *
   * @param username Username of the player
   * @param uuid     UUID of the player
   * @param encoder  Name of the encoder pipeline
   * @param decoder  Name of the decoder pipeline
   * @param timeout  Name of the read timeout pipeline
   * @param boss     Name of the main pipeline
   */
  void hijack(final @NotNull String username, final @NotNull UUID uuid,
              final @NotNull String encoder, final @NotNull String decoder,
              final @NotNull String timeout, final @NotNull String boss);

  /**
   * Sends a packet/message to the player
   *
   * @param msg Message to send to the player
   */
  default void write(final @NotNull Object msg) {
    if (getChannel().isActive()) {
      getChannel().writeAndFlush(msg, getChannel().voidPromise());
    } else {
      ReferenceCountUtil.release(msg);
    }
  }

  /**
   * Queues a buffered message that will be
   * sent once all messages are flushed.
   */
  default void delayedWrite(final @NotNull Object msg) {
    if (getChannel().isActive()) {
      getChannel().write(msg, getChannel().voidPromise());
    } else {
      ReferenceCountUtil.release(msg);
    }
  }

  Cache<InetAddress, Integer> GLOBAL_FAIL_COUNT = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(5)) // expire after 5 minutes
    .build();

  /**
   * Increments the number of times this user (by IP address) has failed the verification
   */
  default void incrementFails() {
    final InetAddress inetAddress = Objects.requireNonNull(getInetAddress());
    final int failCount = getFailedCount();
    // Make sure to remove the old values from the cache
    if (failCount > 0) {
      GLOBAL_FAIL_COUNT.invalidate(inetAddress);
    }
    GLOBAL_FAIL_COUNT.put(inetAddress, failCount + 1);
  }

  /**
   * @return The number of times this user (by IP address) has failed the verification
   * @apiNote Returns 0 if no key/value is present in the cache
   */
  default int getFailedCount() {
    // Make sure to clean up the cache before we try to get cached values
    GLOBAL_FAIL_COUNT.cleanUp();
    return GLOBAL_FAIL_COUNT.asMap().getOrDefault(Objects.requireNonNull(getInetAddress()), 0);
  }

  /**
   * Removes all previously failed verifications
   */
  default void invalidateFails() {
    GLOBAL_FAIL_COUNT.invalidate(Objects.requireNonNull(getInetAddress()));
  }

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
    }

    Statistics.FAILED_VERIFICATIONS.increment();

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
}
