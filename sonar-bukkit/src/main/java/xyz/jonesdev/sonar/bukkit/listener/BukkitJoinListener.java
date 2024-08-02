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
import xyz.jonesdev.sonar.api.fallback.FallbackPipelines;
import xyz.jonesdev.sonar.bukkit.fallback.FallbackBukkitInjector;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Credits: https://github.com/ViaVersion/ViaVersion/blob/master/bukkit/src/main/java/com/viaversion/viaversion/bukkit/listeners/JoinListener.java
public final class BukkitJoinListener implements Listener {
  // ServerGamePacketListenerImpl aka. PlayerConnection
  private boolean initialized = false;
  private MethodHandle handleMethod, packetListenerField, managerField, channelField;

  public BukkitJoinListener() {
    try {
      final Method handleMethod = FallbackBukkitInjector.getOBCClass("entity.CraftPlayer").getDeclaredMethod("getHandle");
      final Field listenerField = findField(false, handleMethod.getReturnType(), "PlayerConnection", "ServerGamePacketListenerImpl");
      final Field managerField = findField(true, listenerField.getType(), "NetworkManager", "Connection");
      final Field channelField = FallbackBukkitInjector.getFieldAt(managerField.getType(), Channel.class, 0);
      this.handleMethod = MethodHandles.lookup().unreflect(handleMethod);
      this.packetListenerField = MethodHandles.lookup().unreflectGetter(listenerField);
      this.managerField = MethodHandles.lookup().unreflectGetter(managerField);
      this.channelField = MethodHandles.lookup().unreflectGetter(channelField);
      initialized = true;
    } catch (java.lang.ReflectiveOperationException exception) {
      initialized = false;
    }
  }

  private static Field findField(boolean checkSuperClass, Class<?> clazz, String... types) throws NoSuchFieldException {
    for (Field field : clazz.getDeclaredFields()) {
      String fieldTypeName = field.getType().getSimpleName();
      for (String type : types) {
        if (fieldTypeName.equals(type)) {
          if (!Modifier.isPublic(field.getModifiers())) {
            field.setAccessible(true);
          }
          return field;
        }
      }
    }
    if (checkSuperClass && clazz != Object.class && clazz.getSuperclass() != null) {
      return findField(true, clazz.getSuperclass(), types);
    }
    throw new NoSuchFieldException(types[0]);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerSpawn(final @NotNull PlayerJoinEvent event) {
    if (!initialized || event.getPlayer().hasMetadata("NPC"))
      return;
    try {
      final Object handle = handleMethod.invoke(event.getPlayer());
      final Object listener = packetListenerField.invoke(handle);
      final Object networkManager = managerField.invoke(listener);
      final Channel channel = (Channel) channelField.invoke(networkManager);
      if (channel != null && channel.isActive() && channel.pipeline().get(FallbackPipelines.FALLBACK_BANDWIDTH) == null) {
        channel.close(); // Traditional disconnections can be canceled via ServerKickEvent.
      }
    } catch (final Throwable t) {
      Sonar.get().getLogger().warn("Failed to get player channel. {}", t);
    }
  }
}
