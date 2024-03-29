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

package xyz.jonesdev.sonar.bungee.fallback;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.packet.Kick;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.bungee.SonarBungee;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Objects;

import static xyz.jonesdev.sonar.common.fallback.FallbackUserWrapper.closeWith;

@RequiredArgsConstructor
public final class FallbackLoginListener implements Listener {

  private static final Field CHANNEL_FIELD;

  static {
    try {
      CHANNEL_FIELD = InitialHandler.class.getDeclaredField("ch");
      CHANNEL_FIELD.setAccessible(true);
    } catch (NoSuchFieldException exception) {
      throw new ReflectiveOperationException(exception);
    }
  }

  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(final @NotNull PreLoginEvent event) throws IllegalAccessException {
    // Check if the number of online players using the same IP address as
    // the connecting player is greater than the configured amount
    final int maxOnlinePerIp = Sonar.get().getConfig().getMaxOnlinePerIp();
    // Don't do anything if the setting is disabled
    if (maxOnlinePerIp <= 0) return;

    final InetAddress inetAddress = event.getConnection().getAddress().getAddress();

    final long onlinePerIp = SonarBungee.INSTANCE.getPlugin().getServer().getPlayers().stream()
      .filter(player -> Objects.equals(player.getAddress().getAddress(), inetAddress))
      .count()
      + 1 /* add 1 because the player hasn't been added to the list of online players yet */;

    // We use '>=' because the player connecting to the server hasn't joined yet
    if (onlinePerIp >= maxOnlinePerIp) {
      final Component component = Sonar.get().getConfig().getTooManyOnlinePerIp();
      final String serialized = GsonComponentSerializer.gson().serialize(component);
      final PendingConnection connection = event.getConnection();
      closeWith(((ChannelWrapper) CHANNEL_FIELD.get(connection)).getHandle(),
        ProtocolVersion.fromId(connection.getVersion()),
        new Kick(ComponentSerializer.deserialize(serialized)));
    }
  }
}
