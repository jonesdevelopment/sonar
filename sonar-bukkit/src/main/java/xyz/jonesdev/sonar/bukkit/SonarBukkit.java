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

package xyz.jonesdev.sonar.bukkit;

import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.bukkit.audience.AudienceListener;
import xyz.jonesdev.sonar.bukkit.command.BukkitSonarCommand;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;

import java.util.Objects;

@Getter
public final class SonarBukkit extends SonarBootstrap<SonarBukkitPlugin> {
  public static SonarBukkit INSTANCE;

  public SonarBukkit(final @NotNull SonarBukkitPlugin plugin) {
    super(plugin, plugin.getDataFolder(), SonarPlatform.BUKKIT);
    INSTANCE = this;
  }

  /**
   * Wrapper for Bukkit audiences
   */
  private final BukkitAudiences bukkitAudiences = BukkitAudiences.create(getPlugin());

  /**
   * Create a wrapper for the plugin logger, so we can use it outside
   * the velocity module.
   * <br>
   * We have to do this, so we can access all necessary API functions.
   */
  private final LoggerWrapper logger = new LoggerWrapper() {

    @Override
    public void info(final String message, final Object... args) {
      getPlugin().getLogger().info(buildFullMessage(message, args));
    }

    @Override
    public void warn(final String message, final Object... args) {
      getPlugin().getLogger().warning(buildFullMessage(message, args));
    }

    @Override
    public void error(final String message, final Object... args) {
      getPlugin().getLogger().severe(buildFullMessage(message, args));
    }
  };

  @Override
  public void enable() {

    // Initialize bStats.org metrics
    new Metrics(getPlugin(), getPlatform().getMetricsId());

    // Register Sonar command
    Objects.requireNonNull(getPlugin().getCommand("sonar")).setExecutor(new BukkitSonarCommand());

    // Register audience register listener
    getPlugin().getServer().getPluginManager().registerEvents(new AudienceListener(), getPlugin());

    // Register Fallback queue task
    getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(getPlugin(), getFallback().getQueue()::poll,
      10L, 10L);

    // Register traffic counter reset task
    getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(getPlugin(), TrafficCounter::reset,
      20L, 20L);

    // Register action bar verbose task
    getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(getPlugin(), getVerboseHandler()::update,
      2L, 2L);
  }
}
