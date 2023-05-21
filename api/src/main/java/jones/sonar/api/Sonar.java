/*
 * Copyright (C) 2023, jones
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

package jones.sonar.api;

import jones.sonar.api.config.SonarConfiguration;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.api.fallback.FallbackHolder;
import jones.sonar.api.logger.Logger;
import jones.sonar.api.statistics.Statistics;
import jones.sonar.api.verbose.Verbose;

public interface Sonar {
  SonarPlatform getPlatform();

  SonarConfiguration getConfig();

  default Fallback getFallback() {
    return FallbackHolder.INSTANCE;
  }

  Verbose getActionBarVerbose();

  Logger getLogger();

  Statistics getStatistics();

  void reload();

  static Sonar get() {
    return SonarProvider.get();
  }
}
