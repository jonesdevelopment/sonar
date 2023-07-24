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

package xyz.jonesdev.sonar.velocity;

import com.velocitypowered.proxy.util.ratelimit.Ratelimiters;
import lombok.Getter;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.SonarSupplier;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.database.DatabaseType;
import xyz.jonesdev.sonar.api.logger.Logger;
import xyz.jonesdev.sonar.common.SonarBootstrap;
import xyz.jonesdev.sonar.velocity.command.SonarCommand;
import xyz.jonesdev.sonar.velocity.fallback.FallbackListener;
import xyz.jonesdev.sonar.velocity.verbose.ActionBarVerbose;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import static xyz.jonesdev.sonar.api.database.Database.IP_COLUMN;
import static xyz.jonesdev.sonar.api.database.Database.VERIFIED_TABLE;

public enum SonarVelocity implements Sonar, SonarBootstrap<SonarVelocityPlugin> {

  INSTANCE;

  @Getter
  private SonarVelocityPlugin plugin;

  @Getter
  private ActionBarVerbose actionBarVerbose;

  @Getter
  private SonarConfiguration config;

  @Getter
  private File pluginDataFolder;

  @Getter
  private final Logger logger = new Logger() {

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

  @Getter
  private final DecimalFormat formatter = new DecimalFormat("#,###");

  @Override
  public SonarPlatform getPlatform() {
    return SonarPlatform.VELOCITY;
  }

  @Override
  public void enable(final SonarVelocityPlugin plugin) {
    this.plugin = plugin;

    final long start = System.currentTimeMillis();

    // Set the API to this class
    SonarSupplier.set(this);

    logger.info("Initializing Sonar...");

    pluginDataFolder = plugin.getDataDirectory().toFile();

    // Initialize configuration
    config = new SonarConfiguration(plugin.getDataDirectory().toFile());
    reload();

    // Initialize bStats.org metrics
    plugin.getMetricsFactory().make(plugin, getServiceId());

    // Register Sonar command
    plugin.getServer().getCommandManager().register("sonar", new SonarCommand());

    // Register Fallback listener
    plugin.getServer().getEventManager().register(plugin, new FallbackListener(getFallback()));

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

    logger.info("Done ({}s)!", String.format("%.3f", startDelay / 1000D));
  }

  @Override
  public void disable() {
    getLogger().info("Starting shutdown process...");

    if (getConfig().DATABASE != DatabaseType.NONE) {
      getLogger().info("[database] Saving entries to database...");

      // We need to clear the table because we don't want any IPs that aren't present
      // or have been manually removed to still be present in the database
      getDatabase().clear(VERIFIED_TABLE);
      getDatabase().addListToTable(VERIFIED_TABLE, IP_COLUMN, getFallback().getVerified());

      // Dispose the database instance
      getDatabase().dispose();

      getLogger().info("[database] Done.");
    }

    getLogger().info("Successfully shut down.");
  }

  @Override
  public void reload() {
    getConfig().load();
    FallbackListener.CachedMessages.update();

    // Apply filter (connection limiter) to Fallback
    getFallback().setAttemptLimiter(Ratelimiters.createWithMilliseconds(config.VERIFICATION_DELAY)::attempt);

    if (getConfig().DATABASE != DatabaseType.NONE) {
      getLogger().info("[database] Initializing database...");
      getDatabase().initialize(getConfig());

      // Clear all verified IPs from memory to avoid issues with the database
      if (!getFallback().getVerified().isEmpty()) {
        getLogger().info("[database] Cleaning verified IPs from memory...");
        getFallback().getVerified().clear();
      }

      // Load all blacklisted and verified IPs from the database
      getLogger().info("[database] Loading verified IPs from the database...");
      getFallback().getVerified().addAll(getDatabase().getListFromTable(VERIFIED_TABLE, IP_COLUMN));
    }
  }
}
