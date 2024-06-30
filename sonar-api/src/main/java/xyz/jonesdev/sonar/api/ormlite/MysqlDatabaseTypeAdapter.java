/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.api.ormlite;

import com.j256.ormlite.jdbc.db.MysqlDatabaseType;
import org.jetbrains.annotations.NotNull;

public final class MysqlDatabaseTypeAdapter extends MysqlDatabaseType {

  // We need to override the default driver class name
  // to use the custom relocated MySQL driver
  @Override
  protected String @NotNull [] getDriverClassNames() {
    return new String[]{"xyz.jonesdev.sonar.libs.mysql.cj.jdbc.Driver"};
  }
}
