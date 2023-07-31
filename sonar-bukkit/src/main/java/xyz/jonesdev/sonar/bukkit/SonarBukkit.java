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
import org.bstats.bukkit.Metrics;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.SonarSupplier;
import xyz.jonesdev.sonar.api.command.InvocationSender;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandRegistry;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.logger.Logger;
import xyz.jonesdev.sonar.api.server.ServerWrapper;
import xyz.jonesdev.sonar.bukkit.command.SonarCommand;
import xyz.jonesdev.sonar.bukkit.verbose.ActionBarVerbose;
import xyz.jonesdev.sonar.common.SonarBootstrap;
import xyz.jonesdev.sonar.common.command.SubcommandRegistryHolder;
import xyz.jonesdev.sonar.common.timer.DelayTimer;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public enum SonarBukkit implements Sonar, SonarBootstrap<SonarBukkitPlugin> {

  INSTANCE;

  @Getter
  private SonarBukkitPlugin plugin;

  @Getter
  private ActionBarVerbose actionBarVerbose;

  @Getter
  private SonarConfiguration config;

  @Getter
  private SubcommandRegistry subcommandRegistry;

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
      return SonarPlatform.VELOCITY;
    }

    @Override
    public Optional<InvocationSender> getOnlinePlayer(final String username) {
      return getPlugin().getServer().getOnlinePlayers().stream()
        .filter(player -> player.getName().equalsIgnoreCase(username))
        .findFirst()
        .map(player -> new InvocationSender() {

          @Override
          public String getName() {
            return player.getName();
          }

          @Override
          public void sendMessage(final String message) {
            player.sendMessage(message);
          }
        });
    }
  };

  @Override
  public void enable(final SonarBukkitPlugin plugin) {
    this.plugin = plugin;

    final DelayTimer timer = new DelayTimer();

    // Set the API to this class
    SonarSupplier.set(this);

    logger.info("Initializing Sonar...");

    // Initialize configuration
    config = new SonarConfiguration(plugin.getDataFolder());
    reload();

    // Initialize sub commands
    subcommandRegistry = new SubcommandRegistryHolder();

    // Initialize bStats.org metrics
    new Metrics(plugin, getServiceId());

    // Register Sonar command
    Objects.requireNonNull(plugin.getCommand("sonar")).setExecutor(new SonarCommand());

    // Initialize action bar verbose
    actionBarVerbose = new ActionBarVerbose(plugin.getServer());

    // Register action bar verbose task
    plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, actionBarVerbose::update,
      100L, 100L);

    // Done
    logger.info("Done ({}s)!", timer.formattedDelay());
  }
}
