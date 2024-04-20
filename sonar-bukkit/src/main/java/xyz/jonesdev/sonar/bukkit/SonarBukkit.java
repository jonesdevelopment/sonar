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

import com.alessiodp.libby.BukkitLibraryManager;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.bukkit.command.BukkitSonarCommand;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;
import xyz.jonesdev.sonar.common.statistics.CachedBandwidthStatistics;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class SonarBukkit extends SonarBootstrap<SonarBukkitPlugin> {
  public static SonarBukkit INSTANCE;

  public SonarBukkit(final @NotNull SonarBukkitPlugin plugin) {
    super(plugin, new BukkitLibraryManager(
        plugin, plugin.getDataFolder().getName()),
      plugin.getDataFolder(), SonarPlatform.BUKKIT);
    INSTANCE = this;
  }

  /**
   * Wrapper for Bukkit audiences
   */
  private final BukkitAudiences bukkitAudiences = BukkitAudiences.create(getPlugin());

  @Override
  public @Nullable Audience audience(final @Nullable UUID uniqueId) {
    if (uniqueId == null) {
      return null;
    }
    return bukkitAudiences.player(uniqueId);
  }

  @Override
  public boolean hasTooManyAccounts(final @NotNull InetAddress inetAddress, final int limit) {
    int count = 1;
    for (final Player player : getPlugin().getServer().getOnlinePlayers()) {
      // I don't know why, but Bukkit is *special*
      if (Objects.isNull(player.getAddress())) continue;
      // Check if the IP address of the player is equal to the IP trying to connect
      if (!Objects.equals(player.getAddress().getAddress(), inetAddress)) continue;
      // Increment count of duplicated accounts
      if (++count >= limit) return true;
    }
    return false;
  }

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

  private Metrics metrics;

  @Override
  public void enable() {
    // Initialize bStats.org metrics
    metrics = new Metrics(getPlugin(), getPlatform().getMetricsId());

    // Add charts for some configuration options
    metrics.addCustomChart(new SimplePie("verification",
      () -> getConfig().getVerification().getTiming().getDisplayName()));
    metrics.addCustomChart(new SimplePie("captcha",
      () -> getConfig().getVerification().getMap().getTiming().getDisplayName()));

    // Register Sonar command
    Objects.requireNonNull(getPlugin().getCommand("sonar")).setExecutor(new BukkitSonarCommand());

    // Register queue and traffic service
    getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(getPlugin(), () -> {
        CachedBandwidthStatistics.reset();
        getFallback().getQueue().poll();
      },
      20L, 20L);

    // Register verbose service
    getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(getPlugin(),
      Sonar.get().getVerboseHandler()::update, 5L, 5L);
  }

  @Override
  public void disable() {
    // Make sure to properly shutdown bStats metrics
    metrics.shutdown();
  }
}
