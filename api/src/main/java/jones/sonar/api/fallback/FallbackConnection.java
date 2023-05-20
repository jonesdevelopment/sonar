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
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;

@Getter
@RequiredArgsConstructor
public final class FallbackConnection<Player, Connection> {
  private final Fallback fallback;
  private final Player player;
  private final Connection connection;
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final int protocolVersion;
  private final long loginTimestamp = System.currentTimeMillis();

  public void fail(final String reason) {
    channel.close();

    fallback.getBlacklisted().add(inetAddress);

    Sonar.get().getLogger().info("[Fallback] {} ({}) has failed the bot check for: {}",
      inetAddress, protocolVersion, reason);
  }
}
