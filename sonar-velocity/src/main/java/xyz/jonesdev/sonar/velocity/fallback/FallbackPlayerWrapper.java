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

package xyz.jonesdev.sonar.velocity.fallback;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.FallbackPlayer;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.Disconnect;

import java.net.InetAddress;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class FallbackPlayerWrapper implements FallbackPlayer<ConnectedPlayer, MinecraftConnection> {
  private final Fallback fallback;
  private final ConnectedPlayer player;
  private final MinecraftConnection connection;
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final ProtocolVersion protocolVersion;

  @Override
  public void disconnect(final @NotNull String reason) {
    final String serialized = ProtocolUtils.getJsonChatSerializer(connection.getProtocolVersion())
      .serialize(Component.text(reason));
    connection.closeWith(Disconnect.create(serialized));
  }

  @Override
  public void write(final @NotNull Object packet) {
    connection.write(packet);
  }
}
