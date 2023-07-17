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

package jones.sonar.bungee;

import jones.sonar.api.Sonar;
import jones.sonar.api.SonarPlatform;
import jones.sonar.api.SonarProvider;
import jones.sonar.api.config.SonarConfiguration;
import jones.sonar.api.database.DatabaseType;
import jones.sonar.api.logger.Logger;
import jones.sonar.bungee.command.SonarCommand;
import jones.sonar.bungee.fallback.FallbackListener;
import jones.sonar.bungee.verbose.ActionBarVerbose;
import jones.sonar.common.SonarBootstrap;
import lombok.Getter;
import org.bstats.bungeecord.Metrics;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static jones.sonar.api.database.Database.*;

public enum SonarBungee implements Sonar, SonarBootstrap<SonarBungeePlugin> {

  INSTANCE;

  @Getter
  private SonarBungeePlugin plugin;

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
      plugin.getLogger().log(Level.INFO, message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      plugin.getLogger().log(Level.WARNING, message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      plugin.getLogger().log(Level.SEVERE, message, args);
    }
  };

  @Getter
  private final DecimalFormat formatter = new DecimalFormat("#,###");

  @Override
  public SonarPlatform getPlatform() {
    return SonarPlatform.BUNGEE;
  }

  @Override
  public void enable(final SonarBungeePlugin plugin) {
    this.plugin = plugin;

    final long start = System.currentTimeMillis();

    // Set the API to this class
    SonarProvider.set(this);

    logger.info("Initializing Sonar...");

    pluginDataFolder = plugin.getDataFolder();

    // Initialize configuration
    config = new SonarConfiguration(plugin.getDataFolder());
    reload();

    // Initialize bStats.org metrics
    new Metrics(plugin, getServiceId());

    // Register Sonar command
    plugin.getServer().getPluginManager().registerCommand(plugin, new SonarCommand());

    // Register Fallback listener
    plugin.getServer().getPluginManager().registerListener(plugin, new FallbackListener(getFallback()));

    // Register Fallback queue task
    plugin.getServer().getScheduler().schedule(plugin, getFallback().getQueue()::poll,
      500L, 500L, TimeUnit.MILLISECONDS);

    // Initialize action bar verbose
    actionBarVerbose = new ActionBarVerbose(plugin.getServer());

    // Register action bar verbose task
    plugin.getServer().getScheduler().schedule(plugin, actionBarVerbose::update,
      100L, 100L, TimeUnit.MILLISECONDS);

    // Done
    final long startDelay = System.currentTimeMillis() - start;

    logger.info("Done (" + String.format("%.3f", startDelay / 1000D) + "s)!");
  }

  @Override
  public void disable() {
    if (getConfig().DATABASE != DatabaseType.NONE) {
      getLogger().info("[database] Saving entries to database...");

      // We need to clear the table because we don't want any IPs which aren't present
      // or have been manually removed to still be present in the database
      getDatabase().clear(BLACKLIST_TABLE);
      getDatabase().addListToTable(BLACKLIST_TABLE, IP_COLUMN, getFallback().getBlacklisted());
      getDatabase().clear(VERIFIED_TABLE);
      getDatabase().addListToTable(VERIFIED_TABLE, IP_COLUMN, getFallback().getVerified());

      // Dispose the database instance
      getDatabase().dispose();
    }
  }

  @Override
  public void reload() {
    getConfig().load();
    FallbackListener.CachedMessages.update();

    if (getConfig().DATABASE != DatabaseType.NONE) {
      getLogger().info("[database] Initializing database...");
      getDatabase().initialize(getConfig());

      // Clear all blacklisted and verified IPs from memory
      if (!getFallback().getVerified().isEmpty()
        || !getFallback().getBlacklisted().isEmpty()) {
        getLogger().info("[database] Cleaning verified and blacklisted IPs from memory...");
        getFallback().getBlacklisted().clear();
        getFallback().getVerified().clear();
      }

      // Load all blacklisted and verified IPs from the database
      getLogger().info("[database] Loading verified and blacklisted IPs from database...");
      getFallback().getBlacklisted().addAll(getDatabase().getListFromTable(BLACKLIST_TABLE, IP_COLUMN));
      getFallback().getVerified().addAll(getDatabase().getListFromTable(VERIFIED_TABLE, IP_COLUMN));
    }
  }
}
