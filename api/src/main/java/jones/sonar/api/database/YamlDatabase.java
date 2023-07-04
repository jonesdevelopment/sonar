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
import jones.sonar.api.config.SonarConfiguration;
import jones.sonar.api.yaml.YamlConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static jones.sonar.api.database.MySQLDatabase.BLACKLIST_TABLE;
import static jones.sonar.api.database.MySQLDatabase.VERIFIED_TABLE;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class YamlDatabase implements Database {
  private @Nullable YamlConfig yamlConfig;
  @Getter
  @Setter
  private boolean loadedFromDatabase;

  @Override
  public void initialize(final @NotNull SonarConfiguration config) {
    if (yamlConfig == null) {
      yamlConfig = new YamlConfig(Sonar.get().getPluginDataFolder(), config.DATABASE_FILE_NAME);
    }
    yamlConfig.load();
  }

  @Override
  public void purge() {
    Objects.requireNonNull(yamlConfig);

    clear(VERIFIED_TABLE);
    clear(BLACKLIST_TABLE);
  }

  @Override
  public void dispose() {
    yamlConfig = null;
  }

  @Override
  public Collection<String> getListFromTable(final @NotNull String table,
                                             final @NotNull String column) {
    return Objects.requireNonNull(yamlConfig).getStringList(table, new ArrayList<>());
  }

  @Override
  public void addListToTable(final @NotNull String table,
                             final @NotNull String column,
                             final @NotNull Collection<String> collection) {
    Objects.requireNonNull(yamlConfig).set(table, collection);
  }

  @Override
  public void remove(final @NotNull String table,
                     final @NotNull String column,
                     final @NotNull String entry) {
    Objects.requireNonNull(yamlConfig);

    final Collection<String> got = getListFromTable(table, "");
    got.remove(entry);
    yamlConfig.set(table, got);
  }

  @Override
  public void clear(final @NotNull String table) {
    Objects.requireNonNull(yamlConfig);

    if (yamlConfig.getConfig().containsKey(table)) {
      yamlConfig.set(table, new ArrayList<>());
    }
  }
}
