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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;

import java.net.InetAddress;

import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.customDisconnect;
import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.deject;

@RequiredArgsConstructor
public class FallbackChannelHandlerAdapter extends ChannelInboundHandlerAdapter {
  protected final Channel channel;
  protected @Nullable String username;
  protected InetAddress inetAddress;
  protected ProtocolVersion protocolVersion;
  protected @Nullable FallbackUser user;

  protected static final Fallback FALLBACK = Sonar.get().getFallback();

  @Override
  public final void channelActive(final @NotNull ChannelHandlerContext ctx) {
    // Increase connections per second for the action bar verbose
    GlobalSonarStatistics.countConnection();
    // Make sure to let the server handle the rest
    ctx.fireChannelActive();
  }

  @Override
  public final void channelInactive(final @NotNull ChannelHandlerContext ctx) {
    // The player can disconnect without sending the login packet first
    if (username != null) {
      // Remove the username from the connected players
      FALLBACK.getConnected().remove(username);
    }
    // The player cannot be in the queue if the IP address is invalid
    if (inetAddress != null) {
      // Remove the IP address from the queue
      FALLBACK.getQueue().remove(inetAddress);
    }
    // The player cannot be in the verification if the user object doesn't exist,
    // or if the user failed/succeeded the bot verification process.
    if (user != null && user.getState().canReceivePackets()) {
      // Mark the verification attempt as failed
      user.fail("disconnected during verification");
    }
    // Make sure to let the server handle the rest
    ctx.fireChannelInactive();
  }

  // We can override the default exceptionCaught method since this handler
  // will run before the connection knows that there has been an error.
  // Additionally, this will also run after our custom decoder.
  @Override
  public final void exceptionCaught(final @NotNull ChannelHandlerContext ctx,
                                    final @NotNull Throwable cause) throws Exception {
    // Simply close the channel if we encounter any errors.
    ctx.close();
  }

  /**
   * Executes the maximum accounts per IP limit check before letting the player join
   *
   * @param ctx    Forwarded channel handler context
   * @param packet Login packet sent by the client
   */
  protected void initialLogin(final @NotNull ChannelHandlerContext ctx,
                              final @NotNull Object packet,
                              final @NotNull String encoder,
                              final @NotNull String boss) {
    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();
    // Skip the maximum online per IP check if it's disabled in the configuration
    if (maxOnlinePerIp > 0) {
      // Check if the number of online players using the same IP address as
      // the connecting player is greater than the configured amount
      if (Sonar.get().hasTooManyAccounts(inetAddress, maxOnlinePerIp)) {
        customDisconnect(channel, protocolVersion, FallbackPreparer.tooManyOnlinePerIP, encoder, boss);
        return;
      }
    }

    ctx.fireChannelRead(packet);
    // Deject the channel since we don't need it anymore
    deject(channel.pipeline());
  }
}
