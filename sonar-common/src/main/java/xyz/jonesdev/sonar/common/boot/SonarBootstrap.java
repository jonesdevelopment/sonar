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

package xyz.jonesdev.sonar.common.boot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.cappuccino.Cappuccino;
import xyz.jonesdev.cappuccino.ExpiringCache;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarSupplier;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandRegistry;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.controller.VerifiedPlayerController;
import xyz.jonesdev.sonar.api.fallback.FallbackRatelimiter;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.api.verbose.Verbose;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer;
import xyz.jonesdev.sonar.common.subcommand.SubcommandRegistryHolder;

import java.io.File;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

@Getter
@RequiredArgsConstructor
public abstract class SonarBootstrap<T> implements Sonar {
  private T plugin;
  private Verbose verboseHandler;
  private SonarConfiguration config;
  private VerifiedPlayerController verifiedPlayerController;
  private File dataDirectory;
  private final SubcommandRegistry subcommandRegistry;
  private final SystemTimer launchTimer = new SystemTimer();

  public SonarBootstrap(final @NotNull T plugin,
                        final File dataDirectory,
                        final Verbose verboseHandler) {
    // Set the API to this instance so the config doesn't have issues
    SonarSupplier.set(this);

    // Set the plugin instance before anything else
    this.plugin = plugin;
    this.dataDirectory = dataDirectory;
    this.verboseHandler = verboseHandler;
    this.config = new SonarConfiguration(dataDirectory);
    this.subcommandRegistry = new SubcommandRegistryHolder();
  }

  public final void initialize() {
    // Check if the branch is not the main branch to warn about unstable versions
    if (!getVersion().isOnMainBranch()) {
      getLogger().warn("You are currently using an unofficial experimental branch.");
      getLogger().warn("It is highly recommended to use the latest stable release of Sonar:");
      getLogger().warn("https://github.com/jonesdevelopment/sonar/releases");
    }

    getLogger().info("Successfully booted in {}s!", launchTimer);
    getLogger().info("Initializing shared components...");

    // Reload configuration
    reload();

    getLogger().info("Successfully initialized components in {}s!", launchTimer);
    getLogger().info("Enabling all tasks and features...");

    try {
      // Run the per-platform initialization method
      enable();

      // Done
      getLogger().info("Done ({}s)!", launchTimer);
    } catch (Throwable throwable) {
      // An error has occurred
      getLogger().error("An error has occurred while launching Sonar: {}", throwable);
    }
  }

  public abstract void enable();

  public final void reload() {
    // Load the configuration
    getConfig().load();

    // Warn player if they reloaded and changed the database type
    if (getVerifiedPlayerController() != null
      && getVerifiedPlayerController().getCachedDatabaseType() != getConfig().DATABASE_TYPE) {
      Sonar.get().getLogger().warn("Reloading the server after changing the database type"
        + " is generally not recommended as it can sometimes cause data loss.");
    }

    // Prepare cached packets
    FallbackPreparer.prepare();

    // Update ratelimiter
    final ExpiringCache<InetAddress> expiringCache = Cappuccino.buildExpiring(
      getConfig().VERIFICATION_DELAY, TimeUnit.MILLISECONDS, 250L
    );
    FallbackRatelimiter.INSTANCE.setExpiringCache(expiringCache);

    // Reinitialize database controller
    verifiedPlayerController = new VerifiedPlayerController();

    // Call post reload task
    postReload();
  }

  public abstract void postReload();

  public void shutdown() {
    getLogger().info("Starting shutdown process...");
    // ...
    getLogger().info("Successfully shut down. Goodbye!");
  }
}
