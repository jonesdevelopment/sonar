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

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import jones.sonar.api.Sonar;

import java.net.InetAddress;

public interface FallbackConnection<Player, Connection> {
  Fallback getFallback();

  Player getPlayer();

  Connection getConnection();

  Channel getChannel();

  ChannelPipeline getPipeline();

  InetAddress getInetAddress();

  int getProtocolVersion();

  default void fail(final String reason) {
    if (getChannel().isActive()) {
      getChannel().close();
    }

    getFallback().getBlacklisted().add(getInetAddress());

    Sonar.get().getLogger().info("[Fallback] {} ({}) has failed the bot check for: {}",
      getInetAddress(), getProtocolVersion(), reason);
  }
}
