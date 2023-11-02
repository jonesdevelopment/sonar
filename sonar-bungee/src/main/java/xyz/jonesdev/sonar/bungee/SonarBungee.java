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
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import org.bstats.bungeecord.Metrics;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.bungee.audience.AudienceListener;
import xyz.jonesdev.sonar.bungee.command.BungeeSonarCommand;
import xyz.jonesdev.sonar.bungee.fallback.FallbackListener;
import xyz.jonesdev.sonar.bungee.fallback.injection.BaseInjectionHelper;
import xyz.jonesdev.sonar.bungee.fallback.injection.ChildChannelInitializer;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;

@Getter
public final class SonarBungee extends SonarBootstrap<SonarBungeePlugin> {
  public static SonarBungee INSTANCE;

  public SonarBungee(final @NotNull SonarBungeePlugin plugin) {
    super(plugin, plugin.getDataFolder(), SonarPlatform.BUNGEE);
    INSTANCE = this;
  }

  /**
   * Wrapper for BungeeCord audiences
   */
  private final BungeeAudiences bungeeAudiences = BungeeAudiences.create(getPlugin());

  /**
   * Create a wrapper for the plugin logger, so we can use it outside
   * the velocity module.
   * <br>
   * We have to do this, so we can access all necessary API functions.
   */
  private final LoggerWrapper logger = new LoggerWrapper() {

    @Override
    public void info(final String message, final Object... args) {
      getPlugin().getLogger().info(buildFullMessage(message, args));
    }

    @Override
    public void warn(final String message, final Object... args) {
      getPlugin().getLogger().warning(buildFullMessage(message, args));
    }

    @Override
    public void error(final String message, final Object... args) {
      getPlugin().getLogger().severe(buildFullMessage(message, args));
    }
  };

  @Override
  public void enable() {

    // Initialize bStats.org metrics
    new Metrics(getPlugin(), getPlatform().getMetricsId());

    // Register Sonar command
    getPlugin().getServer().getPluginManager().registerCommand(getPlugin(), new BungeeSonarCommand());

    // Register Fallback listener
    getPlugin().getServer().getPluginManager().registerListener(getPlugin(), new FallbackListener());

    // Register audience register listener
    getPlugin().getServer().getPluginManager().registerListener(getPlugin(), new AudienceListener());

    // Inject base into ProtocolUtils
    BaseInjectionHelper.inject(ChildChannelInitializer.INSTANCE);
  }
}
