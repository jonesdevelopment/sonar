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
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.command.InvocationSender;
import xyz.jonesdev.sonar.api.fallback.traffic.TrafficCounter;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.api.server.ServerWrapper;
import xyz.jonesdev.sonar.bungee.command.SonarCommand;
import xyz.jonesdev.sonar.bungee.fallback.FallbackListener;
import xyz.jonesdev.sonar.bungee.fallback.injection.BaseInjectionHelper;
import xyz.jonesdev.sonar.bungee.fallback.injection.ChildChannelInitializer;
import xyz.jonesdev.sonar.bungee.verbose.VerboseWrapper;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Getter
public final class SonarBungee extends SonarBootstrap<SonarBungeePlugin> {
  public static SonarBungee INSTANCE;

  public SonarBungee(final @NotNull SonarBungeePlugin plugin) {
    super(plugin, plugin.getDataFolder(), new VerboseWrapper(plugin.getServer()));
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
      getPlugin().getLogger().log(Level.INFO, message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      getPlugin().getLogger().log(Level.WARNING, message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      getPlugin().getLogger().log(Level.SEVERE, message, args);
    }
  };

  /**
   * Create a wrapper object for our server, so we can use it outside
   * the velocity module.
   * <br>
   * We have to do this, so we can access all necessary API functions.
   */
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
  public void enable() {

    // Initialize bStats.org metrics
    new Metrics(getPlugin(), getServer().getPlatform().getMetricsId());

    // Register Sonar command
    getPlugin().getServer().getPluginManager().registerCommand(getPlugin(), new SonarCommand());

    // Register Fallback listener
    getPlugin().getServer().getPluginManager().registerListener(getPlugin(), new FallbackListener(getFallback()));

    // Register Fallback queue task
    getPlugin().getServer().getScheduler().schedule(getPlugin(), getFallback().getQueue()::poll,
      500L, 500L, TimeUnit.MILLISECONDS);

    // Register traffic counter reset task
    getPlugin().getServer().getScheduler().schedule(getPlugin(), TrafficCounter::reset,
      1L, 1L, TimeUnit.SECONDS);

    // Register action bar verbose task
    getPlugin().getServer().getScheduler().schedule(getPlugin(), getVerboseHandler()::update,
      100L, 100L, TimeUnit.MILLISECONDS);

    // Inject base into ProtocolUtils
    BaseInjectionHelper.inject(ChildChannelInitializer.INSTANCE);
  }

  @Override
  public void postReload() {

    // Prepare cached messages
    FallbackListener.CachedMessages.update();
  }
}
