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

package xyz.jonesdev.sonar.velocity;

import com.alessiodp.libby.VelocityLibraryManager;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;
import xyz.jonesdev.sonar.velocity.command.VelocitySonarCommand;
import xyz.jonesdev.sonar.velocity.fallback.FallbackVelocityInjector;

import java.util.UUID;

@Getter
public final class SonarVelocity extends SonarBootstrap<SonarVelocityPlugin> {
  public static SonarVelocity INSTANCE;

  public SonarVelocity(final @NotNull SonarVelocityPlugin plugin) {
    super(plugin, SonarPlatform.VELOCITY, plugin.getDataDirectory().toFile(),
      new VelocityLibraryManager<>(plugin, plugin.getLogger(),
        plugin.getDataDirectory(), plugin.getServer().getPluginManager()));
    INSTANCE = this;
  }

  /**
   * Wrapper for Velocity audiences
   */
  @Override
  public @Nullable Audience audience(final @Nullable UUID uniqueId) {
    if (uniqueId == null) {
      return null;
    }
    return getPlugin().getServer().getPlayer(uniqueId).orElse(null);
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
      getPlugin().getLogger().info(message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      getPlugin().getLogger().warn(message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      getPlugin().getLogger().error(message, args);
    }
  };

  private Metrics metrics;

  @Override
  public void enable() {
    // Initialize bStats.org metrics
    metrics = getPlugin().getMetricsFactory().make(getPlugin(), getPlatform().getMetricsId());

    // Add charts for some configuration options
    metrics.addCustomChart(new SimplePie("verification",
      () -> getConfig().getVerification().getTiming().getDisplayName()));
    metrics.addCustomChart(new SimplePie("captcha",
      () -> getConfig().getVerification().getMap().getTiming().getDisplayName()));

    // Register Sonar command
    getPlugin().getServer().getCommandManager().register("sonar", new VelocitySonarCommand());

    // Make sure to inject into the server's connection handler
    FallbackVelocityInjector.inject(getPlugin().getServer());
  }

  @Override
  public void disable() {
    if (metrics != null) {
      // Make sure to properly shutdown bStats metrics
      metrics.shutdown();
    }
  }
}
