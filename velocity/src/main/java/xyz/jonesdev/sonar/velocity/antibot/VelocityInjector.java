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

package xyz.jonesdev.sonar.velocity.antibot;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.ServerChannelInitializerHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.common.netty.SonarInjectedChannelInitializer;
import xyz.jonesdev.sonar.common.util.exception.ReflectiveOperationException;

import java.lang.reflect.Field;

import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static xyz.jonesdev.sonar.api.antibot.ChannelPipelines.SONAR_PACKET_HANDLER;

@UtilityClass
public class VelocityInjector {
  private final Field CONNECTION_MANAGER_FIELD;
  private final Field SERVER_CHANNEL_INITIALIZER_FIELD;

  static {
    try {
      CONNECTION_MANAGER_FIELD = VelocityServer.class.getDeclaredField("cm");
      CONNECTION_MANAGER_FIELD.setAccessible(true);

      SERVER_CHANNEL_INITIALIZER_FIELD = ServerChannelInitializerHolder.class.getDeclaredField("initializer");
      SERVER_CHANNEL_INITIALIZER_FIELD.setAccessible(true);
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }
  }

  public void inject(final Object proxyServer) {
    try {
      final ConnectionManager connectionManager = (ConnectionManager) CONNECTION_MANAGER_FIELD.get(proxyServer);

      // Make sure to store the original channel initializer
      final ChannelInitializer<Channel> originalInitializer = connectionManager.serverChannelInitializer.get();
      final ChannelInitializer<Channel> injectedInitializer = new SonarInjectedChannelInitializer(
        originalInitializer, pipeline -> pipeline.addAfter(MINECRAFT_DECODER, SONAR_PACKET_HANDLER,
        new VelocityInboundHandler()));

      // Replace the original channel initializer
      SERVER_CHANNEL_INITIALIZER_FIELD.set(connectionManager.getServerChannelInitializer(), injectedInitializer);
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }
  }
}
