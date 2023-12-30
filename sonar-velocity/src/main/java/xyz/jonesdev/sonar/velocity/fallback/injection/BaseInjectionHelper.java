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

package xyz.jonesdev.sonar.velocity.fallback.injection;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;

import java.lang.reflect.Field;

@UtilityClass
public class BaseInjectionHelper {

  @SuppressWarnings("deprecation")
  public void inject(final VelocityServer server, final ChannelInitializer<Channel> serverInitializer) {
    try {
      final Field field = VelocityServer.class.getDeclaredField("cm");
      field.setAccessible(true);

      final ConnectionManager cm = (ConnectionManager) field.get(server);

      cm.serverChannelInitializer.set(serverInitializer);
    } catch (Throwable throwable) {
      throw new ReflectiveOperationException(throwable);
    }
  }
}
