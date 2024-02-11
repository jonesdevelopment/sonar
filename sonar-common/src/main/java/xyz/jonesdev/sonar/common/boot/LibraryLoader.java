/*
 * Copyright (C) 2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.boot;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
class LibraryLoader {
  void loadLibraries(final @NotNull LibraryManager libraryManager) {
    libraryManager.addMavenCentral();
    libraryManager.addJitPack();
    libraryManager.loadLibraries(
      // Simple-YAML
      Library.builder()
        .groupId("com{}github{}Carleslc{}Simple-YAML")
        .artifactId("Simple-Yaml")
        .version("1.8.4")
        .relocate("com{}simpleyaml", "xyz{}jonesdev{}sonar{}libs{}yaml")
        .build(),
      // Gson
      Library.builder()
        .groupId("com{}google{}code{}gson")
        .artifactId("gson")
        .version("2.10.1")
        .relocate("com{}google{}code{}gson", "xyz{}jonesdev{}sonar{}libs{}gson")
        .build(),
      // Caffeine
      Library.builder()
        .groupId("com.github.ben-manes.caffeine")
        .artifactId("caffeine")
        .version("3.1.8")
        .relocate("com{}github{}benmanes{}caffeine", "xyz{}jonesdev{}sonar{}libs{}caffeine")
        .build(),
      // ORMLite
      Library.builder()
        .groupId("com{}j256{}ormlite")
        .artifactId("ormlite-jdbc")
        .version("6.1")
        .relocate("com{}j256{}ormlite", "xyz{}jonesdev{}sonar{}libs{}ormlite")
        .build(),
      // MySQL
      Library.builder()
        .groupId("com{}mysql")
        .artifactId("mysql-connector-j")
        .version("8.3.0")
        .build()
    );
  }
}
