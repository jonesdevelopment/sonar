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

package xyz.jonesdev.sonar.bungee;

import lombok.Getter;
import net.md_5.bungee.api.chat.TextComponent;
import org.bstats.bungeecord.Metrics;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.SonarSupplier;
import xyz.jonesdev.sonar.api.command.InvocationSender;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.database.DatabaseType;
import xyz.jonesdev.sonar.api.logger.Logger;
import xyz.jonesdev.sonar.api.server.ServerWrapper;
import xyz.jonesdev.sonar.bungee.command.SonarCommand;
import xyz.jonesdev.sonar.bungee.fallback.FallbackListener;
import xyz.jonesdev.sonar.bungee.verbose.ActionBarVerbose;
import xyz.jonesdev.sonar.common.SonarBootstrap;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static xyz.jonesdev.sonar.api.database.Database.IP_COLUMN;
import static xyz.jonesdev.sonar.api.database.Database.VERIFIED_TABLE;

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

  /**
   * Create a wrapper object for our server, so we can use it outside
   * the velocity module.
   * We have to do this, so we can access all necessary API functions.
   *
   * @since 2.0.0 (7faa4b6)
   */
  @Getter
  public final ServerWrapper server = new ServerWrapper() {

    @Override
    public SonarPlatform getPlatform() {
      return SonarPlatform.BUNGEE;
    }

    @Override
    public Optional<InvocationSender> getOnlinePlayer(final String username) {
      return getPlugin().getServer().getPlayers().stream()
        .filter(player -> player.getName().equalsIgnoreCase(username))
        .findFirst()
        .map(player -> new InvocationSender() {

          @Override
          public String getName() {
            return player.getName();
          }

          @Override
          public void sendMessage(final String message) {
            player.sendMessage(new TextComponent(message));
          }
        });
    }
  };

  @Override
  public void enable(final SonarBungeePlugin plugin) {
    this.plugin = plugin;

    final long start = System.currentTimeMillis();

    // Set the API to this class
    SonarSupplier.set(this);

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

    getLogger().info("Successfully shut down. Goodbye!");
  }

  @Override
  public void reload() {
    getConfig().load();
    FallbackListener.CachedMessages.update();

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

      getLogger().info("[database] Done.");
    }
  }
}
