/*
 * Copyright (C) 2024 Sonar Contributors
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

import com.alessiodp.libby.BukkitLibraryManager;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.bukkit.command.BukkitSonarCommand;
import xyz.jonesdev.sonar.bukkit.fallback.FallbackBukkitInjector;
import xyz.jonesdev.sonar.bukkit.listener.BukkitJoinListener;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public final class SonarBukkit extends SonarBootstrap<SonarBukkitPlugin> {
  public SonarBukkit(final @NotNull SonarBukkitPlugin plugin) {
    super(plugin, SonarPlatform.BUKKIT, plugin.getDataFolder(), new BukkitLibraryManager(plugin));
  }

  /**
   * Wrapper for Bukkit audiences
   */
  private final BukkitAudiences bukkitAudiences = BukkitAudiences.create(getPlugin());

  @Override
  public @Nullable Audience audience(final @Nullable UUID uniqueId) {
    if (uniqueId == null) {
      return null;
    }
    return bukkitAudiences.player(uniqueId);
  }

  @Override
  public @NotNull Audience sender(final @NotNull Object object) {
    return bukkitAudiences.sender((CommandSender) object);
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

  public static final CompletableFuture<Void> INITIALIZE_LISTENER = new CompletableFuture<>();

  @Override
  public void enable() {
    // Initialize bStats.org metrics
    metrics = new Metrics(getPlugin(), getPlatform().getMetricsId());

    // Add charts for some configuration options
    metrics.addCustomChart(new SimplePie("verification",
      () -> getConfig().getVerification().getTiming().getDisplayName()));
    metrics.addCustomChart(new SimplePie("captcha",
      () -> getConfig().getVerification().getMap().getTiming().getDisplayName()));
    metrics.addCustomChart(new SimplePie("language",
      () -> getConfig().getLanguage().getName()));
    metrics.addCustomChart(new SimplePie("database_type",
      () -> getConfig().getDatabase().getType().getDisplayName()));

    // Register Sonar command
    Objects.requireNonNull(getPlugin().getCommand("sonar")).setExecutor(new BukkitSonarCommand());

    // Try to inject into the server
    if (FallbackBukkitInjector.isLateBindEnabled()) {
      getPlugin().getServer().getScheduler().runTask(getPlugin(), FallbackBukkitInjector::inject);
    } else {
      getPlugin().getServer().getPluginManager().registerEvents(new BukkitJoinListener(), getPlugin());
    }

    // Let the injector know that the plugin has been enabled
    INITIALIZE_LISTENER.complete(null);
  }

  @Override
  public void disable() {
    if (metrics != null) {
      // Make sure to properly shutdown bStats metrics
      metrics.shutdown();
    }
  }
}
