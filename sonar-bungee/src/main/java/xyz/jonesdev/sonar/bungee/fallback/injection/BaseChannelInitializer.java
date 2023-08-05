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

package xyz.jonesdev.sonar.bungee.fallback.injection;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.Varint21LengthFieldPrepender;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.bungee.fallback.varint.Varint21FrameDecoder;
import xyz.jonesdev.sonar.common.exception.ReflectionException;
import xyz.jonesdev.sonar.common.fallback.FallbackTimeoutHandler;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BaseChannelInitializer extends ChannelInitializer<Channel> {
  public static final BaseChannelInitializer INSTANCE = new BaseChannelInitializer();

  private static final WriteBufferWaterMark MARK;
  private static final Varint21LengthFieldPrepender FRAME_ENCODER;

  static {
    try {
      final Field markField = PipelineUtils.class.getDeclaredField("MARK");
      markField.setAccessible(true);
      MARK = (WriteBufferWaterMark) markField.get(null);

      final Field frameEncoder = PipelineUtils.class.getDeclaredField("framePrepender");
      frameEncoder.setAccessible(true);
      FRAME_ENCODER = (Varint21LengthFieldPrepender) frameEncoder.get(null);
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
    }
  }

  // Mostly taken from Waterfall
  @Override
  protected void initChannel(final @NotNull Channel channel) throws Exception {
    try {
      channel.config().setOption(ChannelOption.IP_TOS, 0x18);
    } catch (ChannelException exception) {
    }

    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
    channel.config().setAllocator(PooledByteBufAllocator.DEFAULT);
    channel.config().setWriteBufferWaterMark(MARK);

    channel.pipeline().addLast("frame-decoder", new Varint21FrameDecoder());
    channel.pipeline().addLast("timeout", new FallbackTimeoutHandler(
      BungeeCord.getInstance().config.getTimeout(), TimeUnit.MILLISECONDS
    ));
    channel.pipeline().addLast("frame-prepender", FRAME_ENCODER);
    channel.pipeline().addLast("inbound-boss", new HandlerBoss());
  }
}
