/*
 * Copyright (C) 2024 Sonar Contributors
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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

@ChannelHandler.Sharable
public final class FallbackTailExceptionsHandler extends ChannelDuplexHandler {
  public static final FallbackTailExceptionsHandler INSTANCE = new FallbackTailExceptionsHandler();

  // We can override the default exceptionCaught method since the server
  // does not have any other pipelines that could handle this error.
  // Additionally, this will also run after our custom decoder.
  @Override
  public void exceptionCaught(final @NotNull ChannelHandlerContext ctx,
                              final Throwable cause) throws Exception {
    // Close the channel if we encounter any errors.
    ctx.close();
  }
}
