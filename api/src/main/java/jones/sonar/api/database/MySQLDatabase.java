/*
 * Copyright (C) 2023 jones
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

package jones.sonar.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jones.sonar.api.Sonar;
import jones.sonar.api.config.SonarConfiguration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.Vector;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class MySQLDatabase implements Database {
  @Getter
  private @Nullable DataSource dataSource;
  public static final String VERIFIED_TABLE = "verified_ips";
  public static final String BLACKLIST_TABLE = "blacklisted_ips";
  public static final String IP_COLUMN = "ip_address";

  @Override
  public void initialize(final @NotNull SonarConfiguration config) {
    try {
      // Register MySQL driver
      Class.forName("com.mysql.cj.jdbc.Driver");

      final HikariConfig hikariConfig = new HikariConfig();

      hikariConfig.setJdbcUrl(
        "jdbc:mysql://"
          + config.DATABASE_URL
          + ":" + config.DATABASE_PORT
          + "/" + config.DATABASE_NAME
      );
      hikariConfig.setUsername(config.DATABASE_USERNAME);
      hikariConfig.setPassword(config.DATABASE_PASSWORD);

      dataSource = new HikariDataSource(hikariConfig);

      createTable(IP_COLUMN, VERIFIED_TABLE);
      createTable(IP_COLUMN, BLACKLIST_TABLE);
    } catch (Throwable throwable) {
      Sonar.get().getLogger().error("Failed to connect to database: {}", throwable);
    }
  }

  @Override
  public void dispose() {
    Objects.requireNonNull(dataSource);

    try {
      dataSource.getConnection().close();
    } catch (SQLException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public Collection<String> getListFromTable(final @NotNull String table,
                                             final @NotNull String column) {
    Objects.requireNonNull(getDataSource());

    final Collection<String> output = new Vector<>();

    try (final PreparedStatement statement = getDataSource().getConnection().prepareStatement(
      "select `" + column + "` from `" + table + "` limit " + Sonar.get().getConfig().DATABASE_QUERY_LIMIT
    )) {
      final ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        output.add(resultSet.getString(column));
      }
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
    return output;
  }

  public void addListToTable(final @NotNull String table,
                             final @NotNull String column,
                             final @NotNull Collection<String> collection) {
    Objects.requireNonNull(getDataSource());

    try (final PreparedStatement statement = getDataSource().getConnection().prepareStatement(
      "insert ignore into `" + table + "` (" + column + ") values (?)"
    )) {
      for (final String v : collection) {
        statement.setString(1, v);
        statement.addBatch();
      }

      statement.executeBatch();
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void createTable(final String column, final String @NotNull ... tables) throws SQLException {
    Objects.requireNonNull(dataSource);

    for (final String name : tables) {
      try (final PreparedStatement statement = dataSource.getConnection().prepareStatement(
        "create table if not exists " + name + " (`" + column + "` varchar(16))"
      )) {
        statement.execute();
      }
    }
  }
}
