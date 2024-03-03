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

package xyz.jonesdev.sonar.common.boot;

import com.alessiodp.libby.LibraryManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.SonarSupplier;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandRegistry;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.controller.VerifiedPlayerController;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.api.verbose.Notification;
import xyz.jonesdev.sonar.api.verbose.Verbose;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.subcommand.SubcommandRegistryHolder;
import xyz.jonesdev.sonar.common.update.UpdateChecker;

import java.io.File;
import java.time.Duration;

@Getter
@RequiredArgsConstructor
public abstract class SonarBootstrap<T> implements Sonar {
  private T plugin;
  @Setter
  private Verbose verboseHandler;
  @Setter
  private Notification notificationHandler;
  private SonarConfiguration config;
  private VerifiedPlayerController verifiedPlayerController;
  private File dataDirectory;
  private final LibraryManager libraryManager;
  private final SonarStatistics statistics;
  private final SonarPlatform platform;
  private final SubcommandRegistry subcommandRegistry;
  private final SystemTimer launchTimer = new SystemTimer();

  public SonarBootstrap(final @NotNull T plugin,
                        final LibraryManager libraryManager,
                        final File dataDirectory,
                        final SonarPlatform platform) {
    // Set the Sonar API
    SonarSupplier.set(this);
    // Store our library manager for the API
    this.libraryManager = libraryManager;
    // Load all libraries before anything else
    LibraryLoader.loadLibraries(libraryManager);
    // Set the plugin instance
    this.plugin = plugin;
    this.dataDirectory = dataDirectory;
    this.platform = platform;
    this.statistics = new GlobalSonarStatistics();
    this.verboseHandler = new Verbose();
    this.notificationHandler = new Notification();
    this.config = new SonarConfiguration(dataDirectory);
    this.subcommandRegistry = new SubcommandRegistryHolder();
    // Hide unnecessary debug information
    Logger.setGlobalLogLevel(Level.WARNING);
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
      throwable.printStackTrace(System.err);
      return; // Do not check for updates if the launch failed
    }

    // Check if a new version has been released
    UpdateChecker.checkForUpdates();
  }

  public abstract void enable();

  public final void reload() {
    // Load the configuration
    getConfig().load();

    // Warn player if they reloaded and changed the database type
    if (verifiedPlayerController != null
      && verifiedPlayerController.getCachedDatabaseType() != getConfig().getDatabase().getType()) {
      getLogger().warn("Reloading after changing the database type is not recommended as it may cause data loss.");
    }

    // Prepare cached packets
    FallbackPreparer.prepare();

    // Update ratelimiter cache
    getFallback().getRatelimiter().setExpiringCache(Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMillis(getConfig().getVerification().getReconnectDelay()))
      .build());

    // Update blacklist cache
    final long blacklistTime = getConfig().getVerification().getBlacklistTime();
    final boolean blacklistExists = getFallback().getBlacklist() != null;
    // Make sure the blacklist is only set when we need it to prevent data loss
    if (!blacklistExists // Make sure we create a new cache if it doesn't exist yet
      || getFallback().getBlacklistTime() != blacklistTime) {
      // Create new cache with the configured blacklist time
      getFallback().setBlacklist(Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(getConfig().getVerification().getBlacklistTime()))
        .build());
      // Store the new blacklist time, so we don't have to reset the blacklist every reload
      getFallback().setBlacklistTime(blacklistTime);
      // Warn the user about changing the expiry of the blacklist values
      if (blacklistExists) {
        getLogger().warn("The blacklist has been reset as the duration of entries has changed.");
      }
    }

    // Reinitialize database controller
    verifiedPlayerController = new VerifiedPlayerController();
  }

  public final void shutdown() {
    // Initialize shutdown process
    getLogger().info("Starting shutdown process...");
    // Close database connection
    verifiedPlayerController.close();
    // Run the per-platform disable method
    disable();
    // Thank the user for using Sonar
    getLogger().info("Successfully shut down. Goodbye!");
  }

  public abstract void disable();
}
