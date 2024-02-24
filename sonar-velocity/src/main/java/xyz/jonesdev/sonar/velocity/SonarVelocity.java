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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;
import xyz.jonesdev.sonar.common.statistics.CachedBandwidthStatistics;
import xyz.jonesdev.sonar.velocity.audience.AudienceListener;
import xyz.jonesdev.sonar.velocity.command.VelocitySonarCommand;
import xyz.jonesdev.sonar.velocity.fallback.FallbackInjectionHelper;
import xyz.jonesdev.sonar.velocity.fallback.FallbackLoginListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Getter
public final class SonarVelocity extends SonarBootstrap<SonarVelocityPlugin> {
  public static SonarVelocity INSTANCE;

  public SonarVelocity(final @NotNull SonarVelocityPlugin plugin) {
    super(plugin, new VelocityLibraryManager<>(
        plugin, plugin.getLogger(), plugin.getDataDirectory(), plugin.getServer().getPluginManager()),
      plugin.getDataDirectory().toFile(), SonarPlatform.VELOCITY);
    INSTANCE = this;
  }

  /**
   * Custom wrapper for Velocity audiences
   */
  public static final Map<UUID, Audience> AUDIENCES = new ConcurrentHashMap<>();

  @Override
  public @Nullable Audience audience(final @Nullable UUID uniqueId) {
    if (uniqueId == null) {
      return getPlugin().getServer().getConsoleCommandSource();
    }
    return AUDIENCES.get(uniqueId);
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

  @Override
  public void enable() {

    // Initialize bStats.org metrics
    getPlugin().getMetricsFactory().make(getPlugin(), getPlatform().getMetricsId());

    // Register Sonar command
    getPlugin().getServer().getCommandManager().register("sonar", new VelocitySonarCommand());

    // Register Fallback listener
    getPlugin().getServer().getEventManager().register(getPlugin(), new FallbackLoginListener());

    // TODO: find a proper way of handling Audiences on Velocity
    // Register audience register listener
    getPlugin().getServer().getEventManager().register(getPlugin(), new AudienceListener());

    // Register traffic service
    getPlugin().getServer().getScheduler().buildTask(getPlugin(), CachedBandwidthStatistics::reset)
      .repeat(1L, TimeUnit.SECONDS)
      .schedule();

    // Register queue service
    getPlugin().getServer().getScheduler().buildTask(getPlugin(), getFallback().getQueue().getPollTask())
      .repeat(500L, TimeUnit.MILLISECONDS)
      .schedule();

    // Register verbose service
    getPlugin().getServer().getScheduler().buildTask(getPlugin(), Sonar.get().getVerboseHandler()::update)
      .repeat(200L, TimeUnit.MILLISECONDS)
      .schedule();

    // Make sure to inject into the server's connection handler
    FallbackInjectionHelper.inject(getPlugin().getServer());
  }
}
