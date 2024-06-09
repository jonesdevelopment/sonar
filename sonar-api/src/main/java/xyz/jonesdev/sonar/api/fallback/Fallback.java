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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Fallback {
  public static final Fallback INSTANCE = new Fallback();

  // Map of all players connected to the server in general
  private final ConcurrentMap<InetAddress, Integer> online = new ConcurrentHashMap<>(128);
  // Map of all connected IP addresses (used for fast checking)
  private final ConcurrentMap<InetAddress, Byte> connected = new ConcurrentHashMap<>(128);
  // Cache of all blacklisted IP addresses to ensure each entry can expire after the given time
  @Setter
  private Cache<InetAddress, Byte> blacklist;
  @Setter
  private long blacklistTime;

  private final @NotNull FallbackQueue queue = new FallbackQueue();
  private final @NotNull FallbackRatelimiter ratelimiter = new FallbackRatelimiter();

  private final LoggerWrapper logger = new LoggerWrapper() {

    @Override
    public void info(final String message, final Object... args) {
      Sonar.get().getLogger().info("[fallback] " + message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      Sonar.get().getLogger().warn("[fallback] " + message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      Sonar.get().getLogger().error("[fallback] " + message, args);
    }
  };

  public boolean shouldVerifyNewPlayers() {
    return shouldPerform(Sonar.get().getConfig().getVerification().getTiming());
  }

  public boolean shouldPerformCaptcha() {
    return shouldPerform(Sonar.get().getConfig().getVerification().getMap().getTiming());
  }

  public boolean shouldPerformVehicleCheck() {
    return shouldPerform(Sonar.get().getConfig().getVerification().getVehicle().getTiming());
  }

  private static boolean shouldPerform(final SonarConfiguration.Verification.Timing timing) {
    return timing == SonarConfiguration.Verification.Timing.ALWAYS
      || (timing == SonarConfiguration.Verification.Timing.DURING_ATTACK
      && Sonar.get().getAttackTracker().getCurrentAttack() != null);
  }
}
