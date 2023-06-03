/*
 * Copyright (C) 2023, jones
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

package jones.sonar.api.fallback;

import jones.sonar.api.Sonar;
import jones.sonar.api.logger.Logger;

import java.net.InetAddress;
import java.util.List;

public interface Fallback {
  List<InetAddress> getConnected();

  List<InetAddress> getVerified();

  List<InetAddress> getBlacklisted();

  FallbackQueue getQueue();

  FallbackFilter getAttemptLimiter();

  Sonar getSonar();

  Logger getLogger();

  void setAttemptLimiter(final FallbackFilter limiter);

  default boolean isUnderAttack() {
    return getConnected().size() > Sonar.get().getConfig().MINIMUM_PLAYERS_FOR_ATTACK
      || getQueue().getQueuedPlayers().size() > Sonar.get().getConfig().MINIMUM_PLAYERS_FOR_ATTACK;
  }
}
