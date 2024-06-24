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

package xyz.jonesdev.sonar.api;

import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.attack.AttackTracker;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandRegistry;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.controller.VerifiedPlayerController;
import xyz.jonesdev.sonar.api.event.SonarEventManager;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.api.verbose.Notification;
import xyz.jonesdev.sonar.api.verbose.Verbose;

import java.text.DecimalFormat;
import java.util.UUID;

public interface Sonar {
  DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.##");

  /**
   * Since we want to use Adventure on every server platform,
   * we have to use their platform module to support BungeeCord and Bukkit
   */
  @ApiStatus.Internal
  @Nullable Audience audience(final @Nullable UUID uniqueId);

  /**
   * @return The platform the plugin is being run on
   */
  @NotNull SonarPlatform getPlatform();

  /**
   * @return A small wrapper for the plugin logger so we can use the logger everywhere
   */
  @NotNull LoggerWrapper getLogger();

  @NotNull SonarConfiguration getConfig();

  @NotNull SubcommandRegistry getSubcommandRegistry();

  VerifiedPlayerController getVerifiedPlayerController();

  @NotNull Verbose getVerboseHandler();

  @NotNull Notification getNotificationHandler();

  @NotNull SystemTimer getLaunchTimer();

  @NotNull SonarStatistics getStatistics();

  /**
   * Set a custom verbose handler
   */
  @SuppressWarnings("unused")
  void setVerboseHandler(final @NotNull Verbose verboseHandler);

  /**
   * Set a custom notification handler
   */
  @SuppressWarnings("unused")
  void setNotificationHandler(final @NotNull Notification notificationHandler);

  /**
   * Reloads the entire plugin
   */
  void reload();

  @NotNull
  default SonarVersion getVersion() {
    return SonarVersion.INSTANCE;
  }

  @NotNull
  default SonarEventManager getEventManager() {
    // We don't want anyone else to create a new instance.
    return SonarEventManager.INSTANCE;
  }

  @NotNull
  default AttackTracker getAttackTracker() {
    // We don't want anyone else to create a new instance.
    return AttackTracker.INSTANCE;
  }

  @NotNull
  default Fallback getFallback() {
    // We don't want anyone else to create a new instance.
    return Fallback.INSTANCE;
  }

  @NotNull
  static Sonar get() {
    return SonarSupplier.get();
  }
}
