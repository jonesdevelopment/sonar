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

package xyz.jonesdev.sonar.api.database.controller;

import com.alessiodp.libby.LibraryManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.database.model.VerifiedPlayer;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VerifiedPlayerController {
  private static final ExecutorService DB_UPDATE_SERVICE = Executors.newSingleThreadExecutor();

  private final Map<String, Collection<UUID>> cache = new ConcurrentHashMap<>(128);
  private @Nullable ConnectionSource connectionSource;
  private Dao<VerifiedPlayer, Integer> dao;
  private QueryBuilder<VerifiedPlayer, Integer> queryBuilder;
  @Getter
  private final @NotNull SonarConfiguration.Database.Type cachedDatabaseType;

  public VerifiedPlayerController(final @NotNull LibraryManager libraryManager) {
    final SonarConfiguration.Database database = Sonar.get().getConfig().getDatabase();
    cachedDatabaseType = database.getType();

    if (cachedDatabaseType == SonarConfiguration.Database.Type.NONE) {
      Sonar.get().getLogger().warn("Configure a database to save verified players.");
      return;
    }

    // Make sure to only load the driver once per database type
    if (!cachedDatabaseType.isLoaded()) {
      Sonar.get().getLogger().info("Loading {} driver version {}",
        cachedDatabaseType.getDatabaseType().getDatabaseName(),
        cachedDatabaseType.getDatabaseDriver().getVersion());
      libraryManager.loadLibrary(cachedDatabaseType.getDatabaseDriver());
      cachedDatabaseType.setLoaded(true);
    }

    try {
      final String jdbcURL;

      // H2 has a different JDBC URL layout
      // https://www.codejava.net/java-se/jdbc/connect-to-h2-database-examples
      if (cachedDatabaseType == SonarConfiguration.Database.Type.H2) {
        final File file = new File(Sonar.get().getConfig().getPluginFolder(),
          Sonar.get().getConfig().getGeneralConfig().getString("database.filename"));
        jdbcURL = String.format(cachedDatabaseType.getConnectionString(), file.getAbsolutePath());
      } else {
        // Normal JDBC URL layout for MySQL/MariaDB/...
        jdbcURL = String.format(cachedDatabaseType.getConnectionString(),
          Sonar.get().getConfig().getGeneralConfig().getString("database.host"),
          Sonar.get().getConfig().getGeneralConfig().getInt("database.port"),
          Sonar.get().getConfig().getGeneralConfig().getString("database.name"));
      }

      connectionSource = new JdbcConnectionSource(jdbcURL,
        Sonar.get().getConfig().getGeneralConfig().getString("database.username"),
        Sonar.get().getConfig().getGeneralConfig().getString("database.password"),
        cachedDatabaseType.getDatabaseType());

      // Create database table
      try {
        TableUtils.createTableIfNotExists(connectionSource, VerifiedPlayer.class);
      } catch (SQLException ignored) {
        /*
         * This is caused by a duplicate index;
         * I know this isn't the best method of handling it,
         * but I don't know how else I could address this issue.
         */
      }

      dao = DaoManager.createDao(connectionSource, VerifiedPlayer.class);
      queryBuilder = dao.queryBuilder();

      // Make sure to run the clean task and the caching task in the same thread
      // https://github.com/jonesdevelopment/sonar/issues/150
      DB_UPDATE_SERVICE.execute(() -> {
        try {
          // Make sure to clear all outdated entries first
          clearOld(database.getMaximumAge());
          // Add all entries from the database to the cache
          dao.queryForAll().forEach(this::_add);
        } catch (SQLException exception) {
          Sonar.get().getLogger().error("Error initializing database: {}", exception);
        }
      });
    } catch (SQLException exception) {
      Sonar.get().getLogger().error("Error setting up database: {}", exception);
    }
  }

  /**
   * Closes the connection if instantiated
   */
  public void close() {
    // The connection source will always be null if the database type is NONE.
    if (connectionSource != null) {
      try {
        connectionSource.close();
      } catch (Exception exception) {
        Sonar.get().getLogger().error("Error closing database: {}", exception);
      }
    }
  }

  /**
   * Clear all old entries using the given timestamp.
   */
  private void clearOld(final @Range(from = 1, to = 365) int maximumAge) throws SQLException {
    final long timestamp = Instant.now()
      .minus(maximumAge, ChronoUnit.DAYS)
      .getEpochSecond() * 1000L; // convert to ms

    final List<VerifiedPlayer> oldEntries = queryBuilder.where()
      .lt("timestamp", new Timestamp(timestamp))
      .query();

    if (oldEntries != null && !oldEntries.isEmpty()) {
      for (final VerifiedPlayer player : oldEntries) {
        dao.delete(player);
      }
      Sonar.get().getLogger().info("Removed {} database entries older than {} days.",
        oldEntries.size(), maximumAge);
    }
  }

  /**
   * First, remove the player from the local cache and then,
   * secondly, asynchronously add the player to the database.
   *
   * @param inetAddress InetAddress of the VerifiedPlayer model
   */
  public void remove(final @NotNull String inetAddress) {
    _remove(inetAddress);

    // Don't try to update the column if the database type is NONE
    if (cachedDatabaseType == SonarConfiguration.Database.Type.NONE) {
      return;
    }

    DB_UPDATE_SERVICE.execute(() -> {
      // We cannot throw a NullPointerException within the executor service
      // because we want to handle the error instead of simply throwing an exception
      if (connectionSource == null) {
        return;
      }

      try {
        final List<VerifiedPlayer> verifiedPlayer = queryBuilder.where()
          .eq("ip_address", inetAddress)
          .query();

        if (verifiedPlayer != null) {
          for (final VerifiedPlayer player : verifiedPlayer) {
            dao.delete(player);
          }
        }
      } catch (SQLException exception) {
        Sonar.get().getLogger().error("Error trying to remove entry: {}", exception);
      }
    });
  }

  /**
   * Locally remove the object from the cache
   *
   * @param inetAddress IP address of the player
   */
  private void _remove(final @NotNull String inetAddress) {
    cache.remove(inetAddress);
  }

  /**
   * First, cache the player locally and then,
   * secondly, asynchronously add the player to the database.
   *
   * @param player VerifiedPlayer model
   */
  public void add(final @NotNull VerifiedPlayer player) {
    _add(player);

    // Don't try to update the column if the database type is NONE
    if (cachedDatabaseType == SonarConfiguration.Database.Type.NONE) {
      return;
    }

    DB_UPDATE_SERVICE.execute(() -> {
      // We cannot throw a NullPointerException within the executor service
      // because we want to handle the error instead of simply throwing an exception
      if (connectionSource == null) {
        return;
      }

      try {
        dao.createIfNotExists(player);
      } catch (SQLException exception) {
        Sonar.get().getLogger().error("Error trying to add entry: {}", exception);
      }
    });
  }

  /**
   * Locally cache the object
   *
   * @param player VerifiedPlayer model
   */
  private void _add(final @NotNull VerifiedPlayer player) {
    cache.computeIfAbsent(player.getInetAddress(), v -> new ArrayList<>())
      .add(player.getPlayerUuid());
  }

  /**
   * Returns the number of verified IP addresses
   *
   * @return Estimated size of the local cache
   */
  public int estimatedSize() {
    return cache.size();
  }

  /**
   * Returns the sum of verified IP addresses
   * and their respective collection of UUIDs
   *
   * @return Exact size of all total verified players
   */
  @Deprecated
  public synchronized int exactSize() {
    return cache.values().stream()
      .mapToInt(Collection::size)
      .sum();
  }

  /**
   * @return {@link java.util.Collection} of UUIDs associated with an IP address
   */
  public @Unmodifiable Collection<UUID> getUUIDs(final @NotNull String inetAddress) {
    return cache.getOrDefault(inetAddress, Collections.emptyList());
  }

  /**
   * Clear the local cache, and, if the database type is set,
   * remove the table from the database.
   */
  public synchronized void clearAll() {
    cache.clear();

    // Delete the entire table from the database, if necessary
    if (cachedDatabaseType != SonarConfiguration.Database.Type.NONE) {
      try {
        dao.deleteBuilder().delete();
      } catch (SQLException exception) {
        Sonar.get().getLogger().error("Error trying to clear entries: {}", exception);
      }
    }
  }

  /**
   * @param inetAddress IP address
   * @param uuid        UUID associated to the IP
   * @return Whether the local cache contains the IP and UUID
   */
  public boolean has(final @NotNull String inetAddress, final @NotNull UUID uuid) {
    final Collection<UUID> got = cache.get(inetAddress);
    if (got != null) {
      return got.contains(uuid);
    }
    return false;
  }

  /**
   * @param inetAddress IP address
   * @return Whether the local cache contains the IP
   */
  public boolean has(final @NotNull String inetAddress) {
    return cache.containsKey(inetAddress);
  }
}
