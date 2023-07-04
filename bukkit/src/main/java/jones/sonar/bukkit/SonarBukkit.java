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

package jones.sonar.bukkit;

import jones.sonar.api.Sonar;
import jones.sonar.api.SonarPlatform;
import jones.sonar.api.SonarProvider;
import jones.sonar.api.config.SonarConfiguration;
import jones.sonar.api.database.DatabaseType;
import jones.sonar.api.logger.Logger;
import jones.sonar.bukkit.command.SonarCommand;
import jones.sonar.bukkit.verbose.ActionBarVerbose;
import jones.sonar.common.SonarPlugin;
import lombok.Getter;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.logging.Level;

import static jones.sonar.api.database.MySQLDatabase.*;

public enum SonarBukkit implements Sonar, SonarPlugin<SonarBukkitPlugin> {

  INSTANCE;

  @Getter
  private SonarBukkitPlugin plugin;

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
    return SonarPlatform.BUKKIT;
  }

  @Override
  public void enable(final SonarBukkitPlugin plugin) {
    this.plugin = plugin;

    final long start = System.currentTimeMillis();

    // Set the API to this class
    SonarProvider.set(this);

    logger.info("Initializing Sonar...");

    pluginDataFolder = plugin.getDataFolder();

    // Initialize configuration
    config = new SonarConfiguration(plugin.getDataFolder());
    reload();

    // Register Sonar command
    Objects.requireNonNull(plugin.getCommand("sonar")).setExecutor(new SonarCommand());

    // Initialize action bar verbose
    actionBarVerbose = new ActionBarVerbose(plugin.getServer());

    // Register action bar verbose task
    plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, actionBarVerbose::update,
      100L, 100L);

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
