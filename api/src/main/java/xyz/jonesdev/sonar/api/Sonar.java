/*
 * Copyright (C) 2025 Sonar Contributors
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
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandRegistry;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.database.controller.VerifiedPlayerController;
import xyz.jonesdev.sonar.api.event.SonarEventManager;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.api.notification.ActionBarNotificationHandler;
import xyz.jonesdev.sonar.api.notification.ChatNotificationHandler;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.api.tracker.AttackTracker;

import java.text.DecimalFormat;
import java.util.UUID;

public interface Sonar {
  DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.##");

  /**
   * Since we want to use Adventure on every server platform,
   * we have to use their platform module to support BungeeCord and Bukkit.
   */
  @ApiStatus.Internal
  @Nullable Audience audience(final @Nullable UUID uniqueId);

  @ApiStatus.Internal
  @NotNull Audience sender(final @NotNull Object object);

  /**
   * Gets the platform Sonar is running on (e.g., Bukkit, BungeeCord, Velocity).
   *
   * @return The {@link SonarPlatform} enum representing the current platform.
   */
  @NotNull SonarPlatform getPlatform();

  /**
   * Gets the logger instance for Sonar.
   *
   * @return The {@link LoggerWrapper} instance.
   */
  @NotNull LoggerWrapper getLogger();

  /**
   * Gets the Sonar configuration.
   *
   * @return The {@link SonarConfiguration} instance.
   */
  @NotNull SonarConfiguration getConfig();

  /**
   * Gets the subcommand registry for Sonar commands.
   *
   * @return The {@link SubcommandRegistry} instance.
   */
  @NotNull SubcommandRegistry getSubcommandRegistry();

  /**
   * Gets the controller for verified players.
   * Used for managing players who have passed the verification.
   *
   * @return The {@link VerifiedPlayerController} instance.
   */
  VerifiedPlayerController getVerifiedPlayerController();

  /**
   * Gets the handler for action bar notifications.
   *
   * @return The {@link ActionBarNotificationHandler} instance.
   */
  @NotNull ActionBarNotificationHandler getActionBarNotificationHandler();

  /**
   * Gets the handler for chat notifications.
   *
   * @return The {@link ChatNotificationHandler} instance.
   */
  @NotNull ChatNotificationHandler getChatNotificationHandler();

  /**
   * Gets the system timer used for tracking plugin launch time.
   *
   * @return The {@link SystemTimer} instance.
   */
  @NotNull SystemTimer getLaunchTimer();

  /**
   * Gets the statistics tracker for Sonar.
   *
   * @return The {@link SonarStatistics} instance.
   */
  @NotNull SonarStatistics getStatistics();

  @SuppressWarnings("unused")
  void setActionBarNotificationHandler(final @NotNull ActionBarNotificationHandler notificationHandler);

  @SuppressWarnings("unused")
  void setChatNotificationHandler(final @NotNull ChatNotificationHandler notificationHandler);

  /**
   * Reloads the Sonar configuration.
   */
  void reload();

  /**
   * Gets the current version of Sonar.
   *
   * @return The {@link SonarVersion} instance.
   */
  @NotNull default SonarVersion getVersion() {
    return SonarVersion.INSTANCE;
  }

  /**
   * Gets the event manager for Sonar.
   *
   * @return The {@link SonarEventManager} instance.
   */
  @NotNull default SonarEventManager getEventManager() {
    return SonarEventManager.INSTANCE;
  }

  /**
   * Gets the attack tracker for Sonar.
   *
   * @return The {@link AttackTracker} instance.
   */
  @NotNull default AttackTracker getAttackTracker() {
    return AttackTracker.INSTANCE;
  }

  /**
   * Gets the anti-bot handler (called "Fallback") for Sonar.
   *
   * @return The {@link Fallback} instance.
   */
  @NotNull default Fallback getFallback() {
    return Fallback.INSTANCE;
  }

  /**
   * Gets the instance of the Sonar API.
   *
   * @return The {@link Sonar} instance.
   * @throws IllegalStateException If Sonar has not been initialized.
   */
  @NotNull static Sonar get() {
    return SonarSupplier.get();
  }

  /**
   * Gets the instance of the Sonar API.
   * Only use this method if you understand the implications,
   * and have a specific reason to bypass the standard {@link #get()} method.
   * In most cases, {@link #get()} is the preferred way to access the Sonar API.
   *
   * @return The Sonar instance.
   */
  @ApiStatus.Internal
  @NotNull static Sonar get0() {
    return SonarSupplier.sonar;
  }
}
