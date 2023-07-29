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

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.cappuchino.ExpiringCache;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.logger.Logger;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;

public interface Fallback {
  @NotNull Map<String, InetAddress> getConnected();

  @NotNull Collection<String> getVerified();

  @NotNull ExpiringCache<String> getBlacklisted();

  @NotNull FallbackQueue getQueue();

  @NotNull FallbackFilter getAttemptLimiter();

  @NotNull Sonar getSonar();

  @NotNull Logger getLogger();

  void setAttemptLimiter(final @NotNull FallbackFilter limiter);

  default boolean isUnderAttack() {
    return getConnected().size() > Sonar.get().getConfig().MINIMUM_PLAYERS_FOR_ATTACK
      || getQueue().getQueuedPlayers().size() > Sonar.get().getConfig().MINIMUM_PLAYERS_FOR_ATTACK;
  }
}
