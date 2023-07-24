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

package jones.sonar.api.logger;

/**
 * The whole purpose of doing this is, so we can have one logger for every module.
 * We wouldn't need to get the plugin logger every time we try to use it.
 * Additionally, we are also able to access this logger from {@link jones.sonar.api.Sonar}.
 */
public interface Logger {
  void info(final String message, final Object... args);

  void warn(final String message, final Object... args);

  void error(final String message, final Object... args);
}
