/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.RequiredArgsConstructor;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;

import java.lang.reflect.Method;

import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_HANDLER;

@RequiredArgsConstructor
public final class FallbackChannelInitializer extends ChannelInitializer<Channel> {
  private static final Method INIT_CHANNEL_METHOD;

  static {
    try {
      INIT_CHANNEL_METHOD = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
      INIT_CHANNEL_METHOD.setAccessible(true);
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }
  }

  private final ChannelInitializer<Channel> originalChannelInitializer;

  @Override
  protected void initChannel(final Channel channel) throws Exception {
    // Invoke the original method
    try {
      INIT_CHANNEL_METHOD.invoke(originalChannelInitializer, channel);
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }

    channel.pipeline().addAfter(MINECRAFT_DECODER, FALLBACK_HANDLER, new FallbackChannelHandler(channel));
  }
}
