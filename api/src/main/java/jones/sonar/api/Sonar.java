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
import jones.sonar.api.version.SonarVersion;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public interface Sonar {
  SonarPlatform getPlatform();

  @NotNull SonarConfiguration getConfig();

  @NotNull DecimalFormat getFormatter();

  @NotNull
  default SonarVersion getVersion() {
    return SonarVersion.GET;
  }

  @NotNull
  default Fallback getFallback() {
    return FallbackHolder.INSTANCE;
  }

  @NotNull Verbose getActionBarVerbose();

  @NotNull Logger getLogger();

  @NotNull Statistics getStatistics();

  void reload();

  @NotNull
  static Sonar get() {
    return SonarProvider.get();
  }
}
