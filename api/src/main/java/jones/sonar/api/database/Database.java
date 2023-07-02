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

import jones.sonar.api.Sonar;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface Database {
  ExecutorService service = Executors.newSingleThreadExecutor();

  void initialize(final @NotNull Sonar sonar);

  @SneakyThrows
  default void disconnect() {
    Objects.requireNonNull(getConnection());
    if (!getConnection().isClosed()) {
      getConnection().close();
    }
  }

  default void createTable(final @NotNull String name) {
    prepareStatement("create table if not exists " + name + " (value varchar(16))");
  }

  default void prepareStatement(final @NotNull String query,
                                final Object @NotNull ... arguments) {
    service.execute(() -> {
      try (final PreparedStatement stmt = getConnection().prepareStatement(query)) {
        for (int i = 0; i < arguments.length; i++) {
          stmt.setObject(i + 1, arguments[i]);
        }
        stmt.execute();
      } catch (Throwable throwable) {
        throw new IllegalStateException(throwable);
      }
    });
  }

  default PreparedStatement prepareStatement(final @NotNull String query,
                                             final boolean close,
                                             final Object @NotNull ... arguments) throws Throwable {
    final PreparedStatement stmt = getConnection().prepareStatement(query);
    for (int i = 0; i < arguments.length; i++) {
      stmt.setObject(i + 1, arguments[i]);
    }
    stmt.execute();
    if (close) {
      stmt.close();
    }
    return stmt;
  }

  Collection<String> getListFromTable(final String table);

  void addListToTable(final String table, final Collection<String> collection);

  Connection getConnection();
}
