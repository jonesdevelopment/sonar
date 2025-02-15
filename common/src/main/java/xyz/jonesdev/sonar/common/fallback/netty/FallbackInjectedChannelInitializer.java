/*
 * Copyright (C) 2025 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.common.fallback.FallbackInboundHandler;
import xyz.jonesdev.sonar.common.util.FakeChannelUtil;
import xyz.jonesdev.sonar.common.util.exception.ReflectiveOperationException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_INBOUND_HANDLER;

@RequiredArgsConstructor
public final class FallbackInjectedChannelInitializer extends ChannelInitializer<Channel> {
  private static final MethodHandle INIT_CHANNEL_METHOD;

  static {
    try {
      INIT_CHANNEL_METHOD = MethodHandles.privateLookupIn(ChannelInitializer.class, MethodHandles.lookup())
        .findVirtual(ChannelInitializer.class, "initChannel", MethodType.methodType(void.class, Channel.class));
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }
  }

  private final @NotNull ChannelInitializer<Channel> originalChannelInitializer;
  private final @NotNull Consumer<ChannelPipeline> sonarPipelineInjector;

  @Override
  protected void initChannel(final Channel channel) throws Exception {
    // Invoke the original method
    try {
      INIT_CHANNEL_METHOD.invokeExact(originalChannelInitializer, channel);
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }

    inject(channel, sonarPipelineInjector);
  }

  public static void inject(final @NotNull Channel channel, final Consumer<ChannelPipeline> sonarPipelineInjector) {
    // Inject Sonar's channel handler into the pipeline;
    // Also make sure the player is not a fake player to avoid compatibility issues
    if (channel.isActive() && !FakeChannelUtil.isFakeChannel(channel)) {
      final FallbackInboundHandler inboundHandler = new FallbackInboundHandler(sonarPipelineInjector);
      // We need to be careful on Bukkit, as the encoder can be different
      if (Sonar.get0().getPlatform() == SonarPlatform.BUKKIT) {
        final String encoder = Sonar.get0().getPlatform().getEncoder().apply(channel.pipeline());
        channel.pipeline().addBefore(encoder, FALLBACK_INBOUND_HANDLER, inboundHandler);
      } else {
        channel.pipeline().addFirst(FALLBACK_INBOUND_HANDLER, inboundHandler);
      }
    }
  }
}
