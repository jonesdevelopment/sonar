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

package jones.sonar.bungee;

import jones.sonar.api.Sonar;
import jones.sonar.api.SonarPlatform;
import jones.sonar.api.SonarProvider;
import jones.sonar.api.config.SonarConfiguration;
import jones.sonar.api.logger.Logger;
import jones.sonar.api.statistics.Statistics;
import jones.sonar.bungee.command.SonarCommand;
import jones.sonar.bungee.verbose.ActionBarVerbose;
import jones.sonar.common.SonarPlugin;
import jones.sonar.common.statistics.SonarStatistics;
import lombok.Getter;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public enum SonarBungee implements Sonar, SonarPlugin<SonarBungeePlugin> {

  INSTANCE;

  @Getter
  private SonarBungeePlugin plugin;

  @Getter
  private ActionBarVerbose actionBarVerbose;

  @Getter
  private SonarConfiguration config;

  @Getter
  private Logger logger;

  @Getter
  private Statistics statistics;

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

    plugin.getLogger().info("Initializing Sonar...");

    // Initialize logger
    logger = new Logger() {

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

    // Initialize statistics
    statistics = new SonarStatistics();

    // Initialize configuration
    config = new SonarConfiguration(plugin.getDataFolder());
    reload();

    // Register Sonar command
    plugin.getServer().getPluginManager().registerCommand(plugin, new SonarCommand());

    // Initialize action bar verbose
    actionBarVerbose = new ActionBarVerbose(plugin.getServer());

    // Register action bar verbose task
    plugin.getServer().getScheduler().schedule(plugin, actionBarVerbose::update,
      100L, TimeUnit.MILLISECONDS);

    // Done
    final long startDelay = System.currentTimeMillis() - start;

    plugin.getLogger().info("Done (" + String.format("%.3f", startDelay / 1000D) + "s)!");
  }

  @Override
  public void disable() {
    // Do nothing
  }

  @Override
  public void reload() {
    getConfig().load();
  }
}
