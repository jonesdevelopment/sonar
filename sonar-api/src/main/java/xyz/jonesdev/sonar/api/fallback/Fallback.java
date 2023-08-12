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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.cappuccino.Cappuccino;
import xyz.jonesdev.cappuccino.ExpiringCache;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.logger.Logger;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Getter
public final class Fallback {
  public static final Fallback INSTANCE = new Fallback();
  private final Sonar sonar = Sonar.get();

  private final Map<String, InetAddress> connected = new ConcurrentHashMap<>();
  // Only block the player for a few minutes to avoid issues
  private final ExpiringCache<String> blacklisted = Cappuccino.buildExpiring(
    10L, TimeUnit.MINUTES, 2500L
  );
  private final @NotNull FallbackQueue queue = FallbackQueue.INSTANCE;
  private final @NotNull FallbackRatelimiter ratelimiter = FallbackRatelimiter.INSTANCE;

  private final Logger logger = new Logger() {

    @Override
    public void info(final String message, final Object... args) {
      sonar.getLogger().info("[Fallback] " + message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      sonar.getLogger().warn("[Fallback] " + message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      sonar.getLogger().error("[Fallback] " + message, args);
    }
  };

  public boolean isUnderAttack() {
    return getConnected().size() > Sonar.get().getConfig().MINIMUM_PLAYERS_FOR_ATTACK
      || getQueue().getQueuedPlayers().size() > Sonar.get().getConfig().MINIMUM_PLAYERS_FOR_ATTACK;
  }
}
