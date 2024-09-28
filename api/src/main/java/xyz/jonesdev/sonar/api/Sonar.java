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

  @NotNull SonarPlatform getPlatform();

  @NotNull LoggerWrapper getLogger();

  @NotNull SonarConfiguration getConfig();

  @NotNull SubcommandRegistry getSubcommandRegistry();

  VerifiedPlayerController getVerifiedPlayerController();

  @NotNull ActionBarNotificationHandler getActionBarNotificationHandler();

  @NotNull ChatNotificationHandler getChatNotificationHandler();

  @NotNull SystemTimer getLaunchTimer();

  @NotNull SonarStatistics getStatistics();

  @SuppressWarnings("unused")
  void setActionBarNotificationHandler(final @NotNull ActionBarNotificationHandler notificationHandler);

  @SuppressWarnings("unused")
  void setChatNotificationHandler(final @NotNull ChatNotificationHandler notificationHandler);

  void reload();

  @NotNull
  default SonarVersion getVersion() {
    return SonarVersion.INSTANCE;
  }

  @NotNull
  default SonarEventManager getEventManager() {
    return SonarEventManager.INSTANCE;
  }

  @NotNull
  default AttackTracker getAttackTracker() {
    return AttackTracker.INSTANCE;
  }

  @NotNull
  default Fallback getFallback() {
    return Fallback.INSTANCE;
  }

  @NotNull
  static Sonar get() {
    return SonarSupplier.sonar;
  }
}
