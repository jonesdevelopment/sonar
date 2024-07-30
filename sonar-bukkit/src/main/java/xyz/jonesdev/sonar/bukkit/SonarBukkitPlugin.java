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

package xyz.jonesdev.sonar.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.jonesdev.sonar.bukkit.fallback.FallbackBukkitInjector;

import java.lang.reflect.Method;
import java.util.logging.Level;

public final class SonarBukkitPlugin extends JavaPlugin {
  private SonarBukkit bootstrap;

  public static boolean lateBind = false;

  @Override
  public void onLoad() {
    lateBind: {
      try {
        try {
          Class.forName("org.bukkit.event.server.ServerLoadEvent");
          // lateBind is removed in this spigot version.
          break lateBind;
        } catch (ClassNotFoundException ignore) {
        }
        final Class<?> spigotConfiguration = Class.forName("org.spigotmc.SpigotConfig");
        final Method initFieldMethod = spigotConfiguration.getDeclaredMethod("lateBind");
        initFieldMethod.setAccessible(true);
        initFieldMethod.invoke(null);
        if ((boolean) spigotConfiguration.getField("lateBind").get(null)) {
          getLogger().log(Level.WARNING, "Late-bind is enabled on this server.");
          lateBind = true;
        }
      } catch (final java.lang.ReflectiveOperationException ignore) {
      }
    }
    if (!lateBind) {
      FallbackBukkitInjector.inject();
    }
  }

  @Override
  public void onEnable() {
    bootstrap = new SonarBukkit(this);
    bootstrap.initialize();
  }

  @Override
  public void onDisable() {
    bootstrap.shutdown();
  }
}
