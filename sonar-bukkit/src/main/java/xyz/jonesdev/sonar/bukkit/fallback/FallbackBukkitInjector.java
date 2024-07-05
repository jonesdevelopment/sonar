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

package xyz.jonesdev.sonar.bukkit.fallback;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.ReflectiveOperationException;
import xyz.jonesdev.sonar.api.Sonar;

import java.lang.reflect.Field;

// NMS/OBC stuff mostly taken from
// https://github.com/retrooper/packetevents/blob/2.0/spigot/src/main/java/io/github/retrooper/packetevents/util/SpigotReflectionUtil.java
@UtilityClass
public class FallbackBukkitInjector {
  private final String MODIFIED_PACKAGE_NAME;
  private final String LEGACY_NMS_PACKAGE;
  private final String OBC_PACKAGE;

  private final BukkitServerVersion SERVER_VERSION;
  private final boolean USES_LEGACY_PACKAGING, OBFUSCATED;

  private final Class<?> MINECRAFT_SERVER_CLASS;
  private final Class<?> CRAFTBUKKIT_SERVER_CLASS;
  private final Class<?> SERVER_CONNECTION_CLASS;

  private Object MINECRAFT_SERVER_INSTANCE;
  private final Object MINECRAFT_SERVER_CONNECTION_INSTANCE;

  static {
    final String bukkitPackage = Bukkit.getServer().getClass().getPackage().getName();

    MODIFIED_PACKAGE_NAME = bukkitPackage.split("\\.")[3];
    LEGACY_NMS_PACKAGE = "net.minecraft.server." + MODIFIED_PACKAGE_NAME + ".";
    OBC_PACKAGE = bukkitPackage + ".";

    SERVER_VERSION = resolveServerVersion();
    USES_LEGACY_PACKAGING = SERVER_VERSION.compareTo(BukkitServerVersion.MINECRAFT_1_17) < 0;

    try {
      OBFUSCATED = isObfuscated();
      // Minecraft classes
      MINECRAFT_SERVER_CLASS = getNMSClass("server.MinecraftServer", "MinecraftServer");
      SERVER_CONNECTION_CLASS = getNMSClass(OBFUSCATED ? "server.network.ServerConnection" : "server.network.ServerConnectionListener", "ServerConnection");
      // CraftBukkit classes
      CRAFTBUKKIT_SERVER_CLASS = getOBCClass("CraftServer");

      try {
        // 1.20.5+
        MINECRAFT_SERVER_INSTANCE = getFieldAt(MINECRAFT_SERVER_CLASS, MINECRAFT_SERVER_CLASS, 0).get(null);
      } catch (Exception exception) {
        MINECRAFT_SERVER_INSTANCE = getFieldAt(CRAFTBUKKIT_SERVER_CLASS, MINECRAFT_SERVER_CLASS, 0).get(Bukkit.getServer());
      }

      MINECRAFT_SERVER_CONNECTION_INSTANCE = getFieldAt(MINECRAFT_SERVER_CLASS, SERVER_CONNECTION_CLASS, 0).get(MINECRAFT_SERVER_INSTANCE);
      System.out.println(MINECRAFT_SERVER_CONNECTION_INSTANCE);
    } catch (Exception exception) {
      throw new ReflectiveOperationException(exception);
    }
  }

  private @NotNull Field getFieldAt(final @NotNull Class<?> clazz, final @NotNull Class<?> type, final int index) {
    int currentIndex = 0;
    for (final Field field : clazz.getDeclaredFields()) {
      field.setAccessible(true);

      if (type.isAssignableFrom(field.getType())) {
        if (currentIndex++ == index) {
          return field;
        }
      }
    }

    if (clazz.getSuperclass() != null) {
      return getFieldAt(clazz.getSuperclass(), type, index);
    }
    throw new IllegalStateException("Could not find field #" + index + " in " + clazz.getName());
  }

  private @NotNull BukkitServerVersion resolveServerVersion() {
    final String bukkitVersion = Bukkit.getBukkitVersion();

    for (final BukkitServerVersion serverVersion : BukkitServerVersion.REVERSED_VALUES) {
      if (bukkitVersion.contains(serverVersion.getRelease())) {
        Sonar.get().getLogger().info("Detected Minecraft {} on Bukkit {}",
          serverVersion.getRelease(), bukkitVersion);
        return serverVersion;
      }
    }

    // Throw an error if we cannot find the server version
    throw new IllegalStateException("Could not find server version " + bukkitVersion);
  }

  private boolean isObfuscated() {
    try {
      Class.forName("net.minecraft.server.network.PlayerConnection");
      return true;
    } catch (ClassNotFoundException exception) {
      return false;
    }
  }

  private @NotNull Class<?> getNMSClass(final String modern, final String legacy) throws ClassNotFoundException {
    return Class.forName(USES_LEGACY_PACKAGING ? LEGACY_NMS_PACKAGE + legacy : "net.minecraft." + modern);
  }

  private @NotNull Class<?> getOBCClass(final String clazz) throws ClassNotFoundException {
    return Class.forName(OBC_PACKAGE + clazz);
  }

  public void inject() {
  }
}
