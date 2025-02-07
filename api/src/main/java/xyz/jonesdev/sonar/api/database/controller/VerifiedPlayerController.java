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
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.database.model.VerifiedPlayer;
import xyz.jonesdev.sonar.api.fingerprint.FingerprintingUtil;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VerifiedPlayerController {
  @Getter
  private final Set<String> cache = Collections.synchronizedSet(new HashSet<>());
  private @Nullable ConnectionSource connectionSource;
  private Dao<VerifiedPlayer, Integer> dao;
  private QueryBuilder<VerifiedPlayer, Integer> queryBuilder;
  @Getter
  private final @NotNull SonarConfiguration.Database.Type cachedDatabaseType;
  private final ExecutorService updateService = Executors.newSingleThreadExecutor();

  public VerifiedPlayerController(final @NotNull LibraryManager libraryManager) {
    final SonarConfiguration.Database database = Sonar.get0().getConfig().getDatabase();
    cachedDatabaseType = database.getType();

    if (cachedDatabaseType == SonarConfiguration.Database.Type.NONE) {
      Sonar.get0().getLogger().warn("Configure a database to save verified players.");
      return;
    }

    // Make sure to only load the driver once per database type
    if (!cachedDatabaseType.isLoaded()) {
      Sonar.get0().getLogger().info("Loading {} driver version {}",
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
        final File file = new File(Sonar.get0().getConfig().getPluginFolder(),
          Sonar.get0().getConfig().getGeneralConfig().getString("database.filename"));
        jdbcURL = String.format(cachedDatabaseType.getConnectionString(), file.getAbsolutePath());
      } else {
        // Normal JDBC URL layout for MySQL/MariaDB/...
        jdbcURL = String.format(cachedDatabaseType.getConnectionString(),
          Sonar.get0().getConfig().getGeneralConfig().getString("database.host"),
          Sonar.get0().getConfig().getGeneralConfig().getInt("database.port"),
          Sonar.get0().getConfig().getGeneralConfig().getString("database.name"));
      }

      connectionSource = new JdbcConnectionSource(jdbcURL,
        Sonar.get0().getConfig().getGeneralConfig().getString("database.username"),
        Sonar.get0().getConfig().getGeneralConfig().getString("database.password"),
        cachedDatabaseType.getDatabaseType());

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
      updateService.execute(() -> {
        if (connectionSource != null) {
          try {
            // Make sure to clear all outdated entries first
            clearOld(database.getMaximumAge());
            // Add all entries from the database to the cache
            dao.queryForAll().forEach(verifiedPlayer -> cache.add(verifiedPlayer.getFingerprint()));
          } catch (SQLException exception) {
            exception.printStackTrace(System.err);
          }
        }
      });
    } catch (SQLException exception) {
      exception.printStackTrace(System.err);
    }
  }

  /**
   * Closes the connection if instantiated
   */
  public void close() {
    // The connection source will always be null if the database type is NONE.
    if (connectionSource != null) {
      // Shut down the update service before letting it re-use the connection.
      updateService.shutdown();
      try {
        connectionSource.close();
      } catch (Exception exception) {
        exception.printStackTrace(System.err);
      }
      // Make sure not to reuse a closed connection
      connectionSource = null;
    }
  }

  /**
   * Clear all old entries using the given timestamp
   */
  private void clearOld(final @Range(from = 1, to = 365) int maximumAge) throws SQLException {
    final long timestamp = Instant.now().minus(maximumAge, ChronoUnit.DAYS).getEpochSecond() * 1000L;

    final List<VerifiedPlayer> oldEntries = queryBuilder.where()
      .lt("timestamp", new Timestamp(timestamp))
      .query();

    if (oldEntries != null && !oldEntries.isEmpty()) {
      for (final VerifiedPlayer player : oldEntries) {
        dao.delete(player);
      }
      Sonar.get0().getLogger().info("Removed {} database entries older than {} days.",
        oldEntries.size(), maximumAge);
    }
  }

  /**
   * First, remove the player from the local cache and then,
   * secondly, asynchronously add the player to the database.
   *
   * @param fingerprint Fingerprint of the verified player
   */
  public void remove(final @NotNull String fingerprint) {
    cache.remove(fingerprint);

    // Don't try to update the column if the database type is NONE
    if (cachedDatabaseType == SonarConfiguration.Database.Type.NONE) {
      return;
    }

    updateService.execute(() -> {
      if (connectionSource != null) {
        try {
          final List<VerifiedPlayer> verifiedPlayer = queryBuilder.where()
            .eq("fingerprint", fingerprint)
            .query();

          if (verifiedPlayer != null) {
            for (final VerifiedPlayer player : verifiedPlayer) {
              dao.delete(player);
            }
          }
        } catch (SQLException exception) {
          exception.printStackTrace(System.err);
        }
      }
    });
  }

  /**
   * Creates a new VerifiedPlayer model from the given username, host address, and timestamp
   */
  public void add(final @NotNull String username, final @NotNull String hostAddress, final long timestamp) {
    final String fingerprint = FingerprintingUtil.getFingerprint(username, hostAddress);
    // Add a new VerifiedPlayer object from the given parameters to the database
    add(new VerifiedPlayer(fingerprint, timestamp));
  }

  /**
   * Caches the player locally and then adds the player to the database
   */
  public void add(final @NotNull VerifiedPlayer player) {
    cache.add(player.getFingerprint());

    // Don't try to update the column if the database type is NONE
    if (cachedDatabaseType == SonarConfiguration.Database.Type.NONE) {
      return;
    }

    updateService.execute(() -> {
      if (connectionSource != null) {
        try {
          dao.createIfNotExists(player);
        } catch (SQLException exception) {
          exception.printStackTrace(System.err);
        }
      }
    });
  }

  /**
   * Clear the local cache, and, if the database type is set,
   * remove the table from the database.
   */
  public void clearAll() {
    cache.clear();

    // Delete the entire table from the database, if necessary
    if (connectionSource != null
      && cachedDatabaseType != SonarConfiguration.Database.Type.NONE) {
      try {
        dao.deleteBuilder().delete();
      } catch (SQLException exception) {
        exception.printStackTrace(System.err);
      }
    }
  }
}
