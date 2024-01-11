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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;
import xyz.jonesdev.sonar.velocity.audience.AudienceListener;
import xyz.jonesdev.sonar.velocity.command.VelocitySonarCommand;
import xyz.jonesdev.sonar.velocity.fallback.FallbackListener;

@Getter
public final class SonarVelocity extends SonarBootstrap<SonarVelocityPlugin> {
  public static SonarVelocity INSTANCE;

  public SonarVelocity(final @NotNull SonarVelocityPlugin plugin) {
    super(plugin, plugin.getDataDirectory().toFile(), SonarPlatform.VELOCITY);
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

  @Override
  public void enable() {

    // Initialize bStats.org metrics
    getPlugin().getMetricsFactory().make(getPlugin(), getPlatform().getMetricsId());

    // Register Sonar command
    getPlugin().getServer().getCommandManager().register("sonar", new VelocitySonarCommand());

    // Register Fallback listener
    getPlugin().getServer().getEventManager().register(getPlugin(), new FallbackListener());

    // Register audience register listener
    getPlugin().getServer().getEventManager().register(getPlugin(), new AudienceListener());
  }
}
