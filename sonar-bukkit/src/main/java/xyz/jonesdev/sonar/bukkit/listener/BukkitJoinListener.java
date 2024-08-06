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

package xyz.jonesdev.sonar.bukkit.listener;

import io.netty.channel.Channel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.bukkit.fallback.FallbackBukkitInjector;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_BANDWIDTH;

// Taken from
// https://github.com/ViaVersion/ViaVersion/blob/master/bukkit/src/main/java/com/viaversion/viaversion/bukkit/listeners/JoinListener.java
public final class BukkitJoinListener implements Listener {
  // ServerGamePacketListenerImpl aka. PlayerConnection
  private static boolean initialized;

  private static MethodHandle handleMethod;
  private static MethodHandle packetListenerField;
  private static MethodHandle managerField;
  private static MethodHandle channelField;

  static {
    try {
      final Method handleMethod = FallbackBukkitInjector.getOBCClass("entity.CraftPlayer")
        .getDeclaredMethod("getHandle");
      final Field listenerField = FallbackBukkitInjector.findField(false, handleMethod.getReturnType(),
        "PlayerConnection", "ServerGamePacketListenerImpl");
      final Field managerField = FallbackBukkitInjector.findField(true, listenerField.getType(),
        "NetworkManager", "Connection");
      final Field channelField = FallbackBukkitInjector.getFieldAt(managerField.getType(), Channel.class, 0);

      BukkitJoinListener.handleMethod = MethodHandles.lookup().unreflect(handleMethod);
      packetListenerField = MethodHandles.lookup().unreflectGetter(listenerField);
      BukkitJoinListener.managerField = MethodHandles.lookup().unreflectGetter(managerField);
      BukkitJoinListener.channelField = MethodHandles.lookup().unreflectGetter(channelField);
      initialized = true;
    } catch (Exception exception) {
      initialized = false;
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handle(final @NotNull PlayerJoinEvent event) {
    if (!initialized || event.getPlayer().hasMetadata("NPC")) {
      return;
    }

    try {
      final Object handle = handleMethod.invoke(event.getPlayer());
      final Object listener = packetListenerField.invoke(handle);
      final Object networkManager = managerField.invoke(listener);
      final Channel channel = (Channel) channelField.invoke(networkManager);

      // Close the channel if Sonar's handlers are not found when the PlayerJoinEvent is called
      if (channel != null && channel.isActive() && channel.pipeline().context(FALLBACK_BANDWIDTH) == null) {
        channel.close();
      }
    } catch (Throwable throwable) {
      Sonar.get().getLogger().warn("Failed to the channel for {}: {}", event.getPlayer().getName(), throwable);
    }
  }
}
