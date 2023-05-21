/*
 * Copyright (C) 2023, jones
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

package jones.sonar.velocity;

import jones.sonar.api.Sonar;
import jones.sonar.api.SonarPlatform;
import jones.sonar.api.SonarProvider;
import jones.sonar.api.config.SonarConfiguration;
import jones.sonar.api.logger.Logger;
import jones.sonar.api.statistics.Statistics;
import jones.sonar.common.SonarPlugin;
import jones.sonar.common.statistics.SonarStatistics;
import jones.sonar.velocity.command.SonarCommand;
import jones.sonar.velocity.fallback.FallbackAttemptLimiter;
import jones.sonar.velocity.fallback.FallbackListener;
import jones.sonar.velocity.verbose.ActionBarVerbose;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

public enum SonarVelocity implements Sonar, SonarPlugin<SonarVelocityPlugin> {

  INSTANCE;

  @Getter
  private SonarVelocityPlugin plugin;

  @Getter
  private ActionBarVerbose actionBarVerbose;

  @Getter
  private SonarConfiguration config;

  @Getter
  private Logger logger;

  @Getter
  private Statistics statistics;

  @Override
  public SonarPlatform getPlatform() {
    return SonarPlatform.VELOCITY;
  }

  @Override
  public void enable(final SonarVelocityPlugin plugin) {
    this.plugin = plugin;

    final long start = System.currentTimeMillis();

    // Set the API to this class
    SonarProvider.set(this);

    plugin.getLogger().info("Initializing Sonar...");

    // Initialize logger
    logger = new Logger() {

      @Override
      public void info(final String message, final Object... args) {
        plugin.getLogger().info(message, args);
      }

      @Override
      public void warn(final String message, final Object... args) {
        plugin.getLogger().warn(message, args);
      }

      @Override
      public void error(final String message, final Object... args) {
        plugin.getLogger().error(message, args);
      }
    };

    // Initialize statistics
    statistics = new SonarStatistics();

    // Initialize configuration
    config = new SonarConfiguration(plugin.getDataDirectory().toFile());
    config.load();

    // Register Sonar command
    plugin.getServer().getCommandManager().register("sonar", new SonarCommand());

    // Register Fallback listener
    plugin.getServer().getEventManager().register(plugin, new FallbackListener(logger, getFallback()));

    // Apply filter (connection limiter) to Fallback
    Sonar.get().getFallback().setAttemptLimiter(FallbackAttemptLimiter::shouldAllow);

    // Register Fallback queue task
    plugin.getServer().getScheduler().buildTask(plugin, getFallback().getQueue()::poll)
      .repeat(500L, TimeUnit.MILLISECONDS)
      .schedule();

    // Initialize action bar verbose
    actionBarVerbose = new ActionBarVerbose(plugin.getServer());

    // Register action bar verbose task
    plugin.getServer().getScheduler().buildTask(plugin, actionBarVerbose::update)
      .repeat(100L, TimeUnit.MILLISECONDS)
      .schedule();

    // Done
    final long startDelay = System.currentTimeMillis() - start;

    plugin.getLogger().info("Done ({}s)!", String.format("%.3f", startDelay / 1000D));
  }

  @Override
  public void disable() {
    // Do nothing
  }

  @Override
  public void reload() {
    getConfig().load();
    FallbackListener.CachedMessages.update();
  }
}
