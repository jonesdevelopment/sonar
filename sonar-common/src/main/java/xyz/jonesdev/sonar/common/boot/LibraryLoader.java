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
import xyz.jonesdev.sonar.api.SonarPlatform;

@UtilityClass
class LibraryLoader {
  void loadLibraries(final @NotNull LibraryManager libraryManager, final @NotNull SonarPlatform platform) {
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
        .relocate("com{}google{}gson", "xyz{}jonesdev{}sonar{}libs{}gson")
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
      // MySQL driver
      Library.builder()
        .groupId("com{}mysql")
        .artifactId("mysql-connector-j")
        .version("8.4.0")
        .relocate("com{}mysql", "xyz{}jonesdev{}sonar{}libs{}mysql")
        .build(),
      // MariaDB driver
      Library.builder()
        .groupId("org{}mariadb{}jdbc")
        .artifactId("mariadb-java-client")
        .version("3.4.0")
        .relocate("org{}mariadb", "xyz{}jonesdev{}sonar{}libs{}mariadb")
        .build(),
      // H2 driver
      Library.builder()
        .groupId("com{}h2database")
        .artifactId("h2")
        .version("2.1.214")
        .relocate("org{}h2", "xyz{}jonesdev{}sonar{}libs{}h2")
        .build()
    );

    // Only load adventure if not on Velocity
    if (platform != SonarPlatform.VELOCITY) {
      libraryManager.loadLibraries(
        Library.builder()
          .groupId("net{}kyori")
          .artifactId("adventure-text-minimessage")
          .version("4.17.0")
          .relocate("net{}kyori", "xyz{}jonesdev{}sonar{}libs{}kyori")
          .build(),
        Library.builder()
          .groupId("net{}kyori")
          .artifactId("adventure-text-serializer-gson")
          .version("4.17.0")
          .relocate("net{}kyori", "xyz{}jonesdev{}sonar{}libs{}kyori")
          .build()
      );
    }
  }
}
