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

package xyz.jonesdev.sonar.common.fallback.handler;

import io.netty.handler.codec.CorruptedFrameException;
import xyz.jonesdev.sonar.api.fallback.FallbackConnection;

public interface FallbackHandler {
  FallbackConnection<?, ?> getPlayer();

  default void checkFrame(final boolean condition, final String message) {
    if (!condition) {
      getPlayer().fail(message);
      throw new CorruptedFrameException(message);
    }
  }

  static void checkFrame(final FallbackConnection<?, ?> player,
                         final boolean condition,
                         final String message) {
    if (!condition) {
      player.fail(message);
      throw new CorruptedFrameException(message);
    }
  }
}
