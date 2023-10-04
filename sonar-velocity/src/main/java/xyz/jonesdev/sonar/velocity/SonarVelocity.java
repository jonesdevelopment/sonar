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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.api.server.ServerWrapper;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;
import xyz.jonesdev.sonar.velocity.command.VelocityInvocationSource;
import xyz.jonesdev.sonar.velocity.command.VelocitySonarCommand;
import xyz.jonesdev.sonar.velocity.fallback.FallbackListener;
import xyz.jonesdev.sonar.velocity.verbose.VerboseWrapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Getter
public final class SonarVelocity extends SonarBootstrap<SonarVelocityPlugin> {
  public static SonarVelocity INSTANCE;

  public SonarVelocity(final @NotNull SonarVelocityPlugin plugin) {
    super(plugin, plugin.getDataDirectory().toFile(), new VerboseWrapper(plugin.getServer()));
    INSTANCE = this;
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

  /**
   * Create a wrapper object for our server, so we can use it outside
   * the velocity module.
   * <br>
   * We have to do this, so we can access all necessary API functions.
   */
  public final ServerWrapper server = new ServerWrapper(SonarPlatform.VELOCITY) {

    @Override
    public Optional<InvocationSource> getOnlinePlayer(final String username) {
      return getPlugin().getServer().getAllPlayers().stream()
        .filter(player -> player.getUsername().equalsIgnoreCase(username))
        .findFirst()
        .map(VelocityInvocationSource::new);
    }
  };

  @Override
  public void enable() {

    // Initialize bStats.org metrics
    getPlugin().getMetricsFactory().make(getPlugin(), getServer().getPlatform().getMetricsId());

    // Register Sonar command
    getPlugin().getServer().getCommandManager().register("sonar", new VelocitySonarCommand());

    // Register Fallback listener
    getPlugin().getServer().getEventManager().register(getPlugin(), new FallbackListener(getFallback()));

    // Register Fallback queue task
    getPlugin().getServer().getScheduler().buildTask(getPlugin(), getFallback().getQueue()::poll)
      .repeat(500L, TimeUnit.MILLISECONDS)
      .schedule();

    // Register traffic counter reset task
    getPlugin().getServer().getScheduler().buildTask(getPlugin(), TrafficCounter::reset)
      .repeat(1L, TimeUnit.SECONDS)
      .schedule();

    // Register action bar verbose task
    getPlugin().getServer().getScheduler().buildTask(getPlugin(), getVerboseHandler()::update)
      .repeat(100L, TimeUnit.MILLISECONDS)
      .schedule();
  }

  @Override
  public void postReload() {
  }
}
