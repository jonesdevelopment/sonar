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

package xyz.jonesdev.sonar.common.fallback;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.net.InetAddress;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class FallbackInboundHandler extends ChannelInboundHandlerAdapter {
  private final Consumer<ChannelPipeline> sonarPipelineInjector;
  @Setter
  @Getter
  private InetAddress inetAddress;
  @Getter
  private String handler, encoder;

  @Override
  public void channelActive(final @NotNull ChannelHandlerContext ctx) throws Exception {
    // Increase connections per second for the action bar verbose
    GlobalSonarStatistics.countConnection();
    // Make sure to let the server handle the rest
    ctx.fireChannelActive();
    // Add the packet handler pipeline
    sonarPipelineInjector.accept(ctx.pipeline());
    // Set the handler and encoder pipeline names
    handler = Sonar.get().getPlatform().getHandlerFunction().apply(ctx.pipeline());
    encoder = Sonar.get().getPlatform().getEncoderFunction().apply(ctx.pipeline());
  }

  @Override
  public void channelInactive(final @NotNull ChannelHandlerContext ctx) throws Exception {
    // The player can disconnect without sending the login packet first
    // Account for this by checking if the inetAddress has been set yet
    if (inetAddress != null) {
      // Remove the IP address from the connected players
      Sonar.get().getFallback().getConnected().compute(inetAddress, (k, v) -> null);
      // Remove the IP address from the queue
      Sonar.get().getFallback().getQueue().getPlayers().compute(inetAddress, (k, v) -> null);
      // Remove this account from the online players or decrement the number of accounts with the same IP
      Sonar.get().getFallback().getOnline().compute(inetAddress, (k, v) -> v == null || v <= 1 ? null : v - 1);
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelInactive();
  }

  // We can override the default exceptionCaught method since this handler
  // will run before the connection knows that there has been an error.
  @Override
  public void exceptionCaught(final @NotNull ChannelHandlerContext ctx,
                              final @NotNull Throwable cause) throws Exception {
    // Close the channel if we encounter any errors.
    ctx.close();
  }
}
