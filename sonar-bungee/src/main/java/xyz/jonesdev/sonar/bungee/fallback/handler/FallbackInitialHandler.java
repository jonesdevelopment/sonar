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

package xyz.jonesdev.sonar.bungee.fallback.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.packet.LoginRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.bungee.fallback.FallbackPlayerWrapper;

import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class FallbackInitialHandler extends InitialHandler {
  public FallbackInitialHandler(final BungeeCord bungee, final ListenerInfo listener) {
    super(bungee, listener);
  }

  private static final @NotNull Fallback FALLBACK = Objects.requireNonNull(Sonar.get().getFallback());
  private ChannelWrapper channelWrapper;
  @SuppressWarnings("unused") // TODO: remove later
  private @Nullable FallbackPlayerWrapper player;

  @Override
  public void connected(final ChannelWrapper channelWrapper) throws Exception {
    this.channelWrapper = channelWrapper;
    super.connected(channelWrapper);
  }

  @Override
  public void handle(final LoginRequest loginRequest) throws Exception {
    final InetAddress inetAddress = getAddress().getAddress();

    if (player == null && !FALLBACK.getSonar().getVerifiedPlayerController().has(inetAddress)) {
      final Channel channel = channelWrapper.getHandle();

      player = new FallbackPlayerWrapper(
        FALLBACK, channelWrapper, this,
        channel, channel.pipeline(), inetAddress,
        ProtocolVersion.fromId(getHandshake().getProtocolVersion())
      );
      // TODO: implement Fallback
    }

    super.handle(loginRequest);
  }

  // Mostly taken from Velocity
  public void closeWith(final Object msg) {
    if (player != null && player.getChannel().isActive()) {
      boolean is17 = player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) < 0
        && player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_7_2) >= 0;
      if (is17) {
        player.getChannel().eventLoop().execute(() -> {
          channelWrapper.getHandle().config().setAutoRead(false);
          player.getChannel().eventLoop().schedule(() -> {
            channelWrapper.markClosed();
            player.getChannel().writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
          }, 250L, TimeUnit.MILLISECONDS);
        });
      } else {
        channelWrapper.markClosed();
        player.getChannel().writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
      }
    }
  }
}
