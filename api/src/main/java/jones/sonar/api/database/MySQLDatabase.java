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
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.Vector;

final class MySQLDatabase implements Database {
  @Getter
  private @Nullable HikariDataSource dataSource;
  private Connection connection;

  @Override
  public void initialize(final @NotNull SonarConfiguration config) {
    try {
      // Register MySQL driver
      Class.forName("com.mysql.cj.jdbc.Driver");

      // Set up hikari
      final HikariConfig hikariConfig = new HikariConfig();

      hikariConfig.setJdbcUrl(
        "jdbc:mysql://" + config.DATABASE_URL + ":" + config.DATABASE_PORT + "/" + config.DATABASE_NAME
      );
      hikariConfig.setUsername(config.DATABASE_USERNAME);
      hikariConfig.setPassword(config.DATABASE_PASSWORD);

      // Connect to the server
      dataSource = new HikariDataSource(hikariConfig);
      connection = dataSource.getConnection();

      // Create necessary tables and columns
      createTable(IP_COLUMN, VERIFIED_TABLE);
      createTable(IP_COLUMN, BLACKLIST_TABLE);
    } catch (Throwable throwable) {
      Sonar.get().getLogger().error("Failed to connect to database: {}", throwable);
      throwable.printStackTrace();
    }
  }

  @Override
  public void purge() {
    Objects.requireNonNull(dataSource);

    clear(VERIFIED_TABLE);
    clear(BLACKLIST_TABLE);
  }

  @Override
  public void dispose() {
    Objects.requireNonNull(dataSource);

    dataSource.close();
    dataSource = null;
  }

  public Collection<String> getListFromTable(final @NotNull String table,
                                             final @NotNull String column) {
    Objects.requireNonNull(dataSource);

    final Collection<String> output = new Vector<>();

    try (final PreparedStatement statement = connection.prepareStatement(
      "select `" + column + "` from `" + table + "` limit " + Sonar.get().getConfig().DATABASE_QUERY_LIMIT
    )) {
      final ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        output.add(resultSet.getString(column));
      }
    } catch (SQLException exception) {
      Sonar.get().getLogger().error("Error executing getListFromTable: {}", exception);
      throw new IllegalStateException(exception);
    }
    return output;
  }

  @Override
  public void addListToTable(final @NotNull String table,
                             final @NotNull String column,
                             final @NotNull Collection<String> collection) {
    Objects.requireNonNull(dataSource);

    try (final PreparedStatement selectStatement =
           connection.prepareStatement("select 1 from `" + table + "` where `" + column +
             "` = ?");
         final PreparedStatement insertStatement =
           connection.prepareStatement("insert into `" + table + "` (`" + column + "`) " +
             "values (?)")) {
      for (final String v : collection) {
        selectStatement.setString(1, v);
        final ResultSet resultSet = selectStatement.executeQuery();

        if (!resultSet.next()) {
          insertStatement.setString(1, v);
          insertStatement.executeUpdate();
        }

        resultSet.close();
      }
    } catch (SQLException exception) {
      Sonar.get().getLogger().error("Error executing addListToTable: {}", exception);
      throw new IllegalStateException(exception);
    }
  }

  @Override
  public void clear(final @NotNull String table) {
    Objects.requireNonNull(dataSource);

    try (final PreparedStatement statement = connection.prepareStatement(
      "delete from `" + table + "`"
    )) {
      statement.execute();
    } catch (SQLException exception) {
      Sonar.get().getLogger().error("Error executing prepareRawStatement: {}", exception);
      throw new IllegalStateException(exception);
    }
  }

  @SuppressWarnings("SameParameterValue")
  public void createTable(final String column, final String @NotNull ... tables) {
    Objects.requireNonNull(dataSource);

    for (final String name : tables) {
      try (final PreparedStatement statement = connection.prepareStatement(
        "create table if not exists " + name + " (`" + column + "` varchar(16))"
      )) {
        statement.execute();
      } catch (SQLException exception) {
        Sonar.get().getLogger().error("Error executing createTable: {}", exception);
        throw new IllegalStateException(exception);
      }
    }
  }
}
