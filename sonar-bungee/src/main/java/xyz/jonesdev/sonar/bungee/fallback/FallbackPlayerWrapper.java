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

package xyz.jonesdev.sonar.bungee.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.netty.ChannelWrapper;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.FallbackPlayer;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.bungee.fallback.handler.FallbackInitialHandler;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.Disconnect;

import java.net.InetAddress;

@Getter
@RequiredArgsConstructor
public final class FallbackPlayerWrapper implements FallbackPlayer<ChannelWrapper, FallbackInitialHandler> {
  private final Fallback fallback;
  private final ChannelWrapper player;
  private final FallbackInitialHandler connection;
  private final Channel channel;
  private final ChannelPipeline pipeline;
  private final InetAddress inetAddress;
  private final ProtocolVersion protocolVersion;

  @Override
  public void disconnect(final @NotNull String reason) {
    connection.closeWith(Disconnect.create(TextComponent.toLegacyText(new TextComponent(reason))));
  }

  @Override
  public void write(final @NotNull Object packet) {
    if (channel.isActive()) {
      channel.writeAndFlush(packet, channel.voidPromise());
    } else {
      ReferenceCountUtil.release(packet);
    }
  }
}
