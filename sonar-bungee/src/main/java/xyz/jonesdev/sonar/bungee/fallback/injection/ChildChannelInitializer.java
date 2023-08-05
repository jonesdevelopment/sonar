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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.event.ClientConnectEvent;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.*;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.common.exception.ReflectionException;

import java.lang.reflect.Field;
import java.net.SocketAddress;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChildChannelInitializer extends ChannelInitializer<Channel> {
  public static final ChildChannelInitializer INSTANCE = new ChildChannelInitializer();

  private static final BaseChannelInitializer BASE = BaseChannelInitializer.INSTANCE;

  private static final KickStringWriter LEGACY_KICKER;

  static {
    try {
      final Field kickField = PipelineUtils.class.getDeclaredField("legacyKicker");
      kickField.setAccessible(true);
      LEGACY_KICKER = (KickStringWriter) kickField.get(null);
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
    }
  }

  // Mostly taken from Waterfall
  @Override
  protected void initChannel(final @NotNull Channel channel) throws Exception {
    final SocketAddress remoteAddress = channel.remoteAddress() == null ? channel.parent().localAddress() :
      channel.remoteAddress();

    if (BungeeCord.getInstance().getConnectionThrottle() != null && BungeeCord.getInstance().getConnectionThrottle().throttle(remoteAddress)) {
      channel.close();
    } else {
      final ListenerInfo listener = channel.attr(PipelineUtils.LISTENER).get();

      if (BungeeCord.getInstance().getPluginManager().callEvent(new ClientConnectEvent(remoteAddress, listener)).isCancelled()) {
        channel.close();
      } else {
        try {
          BASE.initChannel(channel);
        } catch (Exception exception) {
          exception.printStackTrace();
          channel.close();
          return;
        }

        channel.pipeline().addBefore("frame-decoder", "legacy-decoder", new LegacyDecoder());
        channel.pipeline().addAfter("frame-decoder", "packet-decoder", new MinecraftDecoder(Protocol.HANDSHAKE, true,
          ProxyServer.getInstance().getProtocolVersion()));
        channel.pipeline().addAfter("frame-prepender", "packet-encoder", new MinecraftEncoder(Protocol.HANDSHAKE,
          true, ProxyServer.getInstance().getProtocolVersion()));
        channel.pipeline().addBefore("frame-prepender", "legacy-kick", LEGACY_KICKER);
        channel.pipeline().get(HandlerBoss.class).setHandler(new InitialHandler(BungeeCord.getInstance(), listener));

        if (listener.isProxyProtocol()) {
          channel.pipeline().addFirst(new HAProxyMessageDecoder());
        }
      }
    }
  }
}
