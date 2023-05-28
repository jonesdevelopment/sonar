/*
 * Copyright (C) 2023 jones
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

package jones.sonar.velocity.fallback;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.api.fallback.FallbackConnection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;

@Getter
@RequiredArgsConstructor
public final class FallbackPlayer implements FallbackConnection<ConnectedPlayer, MinecraftConnection> {
  private final Fallback fallback;
  private final ConnectedPlayer player;
  private final MinecraftConnection connection;
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final int protocolVersion;
}
