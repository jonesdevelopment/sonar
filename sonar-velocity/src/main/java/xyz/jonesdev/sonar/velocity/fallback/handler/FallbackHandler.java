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

package xyz.jonesdev.sonar.velocity.fallback.handler;

import com.velocitypowered.proxy.protocol.packet.Disconnect;
import io.netty.handler.codec.CorruptedFrameException;
import xyz.jonesdev.sonar.api.fallback.FallbackConnection;
import xyz.jonesdev.sonar.velocity.fallback.FallbackListener;
import xyz.jonesdev.sonar.velocity.fallback.FallbackPlayer;

public interface FallbackHandler {
  FallbackPlayer getPlayer();

  default void checkFrame(final boolean condition, final String message) {
    if (!condition) {
      getPlayer().getConnection().closeWith(
        Disconnect.create(FallbackListener.CachedMessages.VERIFICATION_FAILED, getPlayer().getPlayer().getProtocolVersion()
      ));

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
