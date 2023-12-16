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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.cappuccino.Cappuccino;
import xyz.jonesdev.cappuccino.ExpiringCache;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;

import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Fallback {
  public static final Fallback INSTANCE = new Fallback();
  private static final @NotNull Sonar SONAR = Objects.requireNonNull(Sonar.get());

  private final Map<String, InetAddress> connected = new ConcurrentHashMap<>(64, 0.75f);
  // Only block the player for a few minutes to avoid issues
  private final ExpiringCache<InetAddress> blacklisted = Cappuccino.buildExpiring(
    10L, TimeUnit.MINUTES, 5000L);
  private final @NotNull FallbackQueue queue = FallbackQueue.INSTANCE;
  private final @NotNull FallbackRatelimiter ratelimiter = FallbackRatelimiter.INSTANCE;

  private final LoggerWrapper logger = new LoggerWrapper() {

    @Override
    public void info(final String message, final Object... args) {
      SONAR.getLogger().info("[fallback] " + message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      SONAR.getLogger().warn("[fallback] " + message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      SONAR.getLogger().error("[fallback] " + message, args);
    }
  };

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean shouldVerifyNewPlayers() {
    return performTimingCheck(Sonar.get().getConfig().getVerification().getTiming());
  }

  public boolean shouldDoMapCaptcha() {
    return performTimingCheck(Sonar.get().getConfig().getVerification().getMap().getTiming());
  }

  private boolean performTimingCheck(final SonarConfiguration.Verification.Timing timing) {
    return timing == SonarConfiguration.Verification.Timing.ALWAYS
      || (timing == SonarConfiguration.Verification.Timing.DURING_ATTACK
      && Sonar.get().getAttackTracker().isCurrentlyUnderAttack());
  }
}
