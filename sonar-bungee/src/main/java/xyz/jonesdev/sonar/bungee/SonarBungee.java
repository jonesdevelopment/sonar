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

package xyz.jonesdev.sonar.bungee;

import com.alessiodp.libby.BungeeLibraryManager;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.bungee.command.BungeeSonarCommand;
import xyz.jonesdev.sonar.bungee.fallback.FallbackInjectionHelper;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;
import xyz.jonesdev.sonar.common.statistics.CachedBandwidthStatistics;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public final class SonarBungee extends SonarBootstrap<SonarBungeePlugin> {
  public static SonarBungee INSTANCE;

  public SonarBungee(final @NotNull SonarBungeePlugin plugin) {
    super(plugin, new BungeeLibraryManager(plugin), plugin.getDataFolder(), SonarPlatform.BUNGEE);
    INSTANCE = this;
  }

  /**
   * Wrapper for BungeeCord audiences
   */
  private final BungeeAudiences bungeeAudiences = BungeeAudiences.create(getPlugin());

  @Override
  public @Nullable Audience audience(final @Nullable UUID uniqueId) {
    if (uniqueId == null) {
      return null;
    }
    return bungeeAudiences.player(uniqueId);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasTooManyAccounts(final @NotNull InetAddress inetAddress, final int limit) {
    int count = 1;
    for (final ProxiedPlayer player : getPlugin().getServer().getPlayers()) {
      // Check if the IP address of the player is equal to the IP trying to connect
      if (!Objects.equals(player.getAddress().getAddress(), inetAddress)) continue;
      // Increment count of duplicated accounts
      if (++count >= limit) return true;
    }
    return false;
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

  private Metrics metrics;

  @Override
  public void enable() {
    // Initialize bStats.org metrics
    metrics = new Metrics(getPlugin(), getPlatform().getMetricsId());

    // Add charts for some configuration options
    metrics.addCustomChart(new SimplePie("verification",
      () -> getConfig().getVerification().getTiming().getDisplayName()));
    metrics.addCustomChart(new SimplePie("captcha",
      () -> getConfig().getVerification().getMap().getTiming().getDisplayName()));

    // Register Sonar command
    getPlugin().getServer().getPluginManager().registerCommand(getPlugin(), new BungeeSonarCommand());

    // Register queue and traffic service
    getPlugin().getServer().getScheduler().schedule(getPlugin(), () -> {
        CachedBandwidthStatistics.reset();
        getFallback().getQueue().poll();
      },
      1L, 1L, TimeUnit.SECONDS);

    // Register verbose service
    getPlugin().getServer().getScheduler().schedule(getPlugin(), Sonar.get().getVerboseHandler()::update,
      250L, 250L, TimeUnit.MILLISECONDS);

    // Make sure to inject into the server's connection handler
    FallbackInjectionHelper.inject();
  }

  @Override
  public void disable() {
    // Make sure to properly shutdown bStats metrics
    metrics.shutdown();
  }
}
