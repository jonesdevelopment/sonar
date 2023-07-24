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

package xyz.jonesdev.sonar.api.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.yaml.YamlConfig;

import java.util.Collection;
import java.util.Objects;
import java.util.Vector;

final class YamlDatabase implements Database {
  private @Nullable YamlConfig yamlConfig;

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
  }

  @Override
  public void dispose() {
    yamlConfig = null;
  }

  @Override
  public Collection<String> getListFromTable(final @NotNull String table,
                                             final @NotNull String column) {
    return Objects.requireNonNull(yamlConfig).getStringList(table, new Vector<>());
  }

  @Override
  public void addListToTable(final @NotNull String table,
                             final @NotNull String column,
                             final @NotNull Collection<String> collection) {
    Objects.requireNonNull(yamlConfig).set(table, collection);
  }

  @Override
  public void clear(final @NotNull String table) {
    Objects.requireNonNull(yamlConfig);

    yamlConfig.set(table, new Vector<>());
  }
}
