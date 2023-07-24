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

package xyz.jonesdev.sonar.common;

import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.database.DatabaseType;

import static xyz.jonesdev.sonar.api.database.Database.IP_COLUMN;
import static xyz.jonesdev.sonar.api.database.Database.VERIFIED_TABLE;

public interface SonarBootstrap<T> extends Sonar {
  void enable(final T plugin);

  default void reload() {
    if (getConfig().DATABASE != DatabaseType.NONE) {
      getLogger().info("[database] Initializing database...");
      getDatabase().initialize(getConfig());

      // Clear all verified IPs from memory to avoid issues with the database
      if (!getFallback().getVerified().isEmpty()) {
        getLogger().info("[database] Cleaning verified IPs from memory...");
        getFallback().getVerified().clear();
      }

      // Load all blacklisted and verified IPs from the database
      getLogger().info("[database] Loading verified IPs from the database...");
      getFallback().getVerified().addAll(getDatabase().getListFromTable(VERIFIED_TABLE, IP_COLUMN));

      getLogger().info("[database] Done.");
    }
  }

  default void disable() {
    getLogger().info("Starting shutdown process...");

    if (getConfig().DATABASE != DatabaseType.NONE) {
      getLogger().info("[database] Saving entries to database...");

      // We need to clear the table because we don't want any IPs that aren't present
      // or have been manually removed to still be present in the database
      getDatabase().clear(VERIFIED_TABLE);
      getDatabase().addListToTable(VERIFIED_TABLE, IP_COLUMN, getFallback().getVerified());

      // Dispose the database instance
      getDatabase().dispose();

      getLogger().info("[database] Done.");
    }

    getLogger().info("Successfully shut down. Goodbye!");
  }
}
