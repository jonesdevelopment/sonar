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

package xyz.jonesdev.sonar.api.dependencies;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

// Mostly taken from
// https://github.com/Elytrium/LimboAuth/blob/master/src/main/java/net/elytrium/limboauth/dependencies/BaseLibrary.java
public enum Dependency {
  // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
  MYSQL(
    "com/mysql",
    "mysql-connector-j",
    "8.2.0"
  ),
  // TODO: MariaDB still seems to have some issues; investigate!
  // https://mvnrepository.com/artifact/org.mariadb.jdbc/mariadb-java-client
  MARIADB(
    "org/mariadb/jdbc",
    "mariadb-java-client",
    "3.1.4"
  );

  private final Path tempFilePath;
  private final URL mvnRepoURL;

  Dependency(final @NotNull String groupId,
             final @NotNull String artifactId,
             final @NotNull String version) {
    final String archiveFileName = String.format("%s-%s.jar", artifactId, version);
    this.tempFilePath = new File(Sonar.get().getDataDirectory(), "libraries/" + archiveFileName).toPath();

    try {
      final String mvnCentralPath = String.format("%s/%s/%s/%s-%s.jar",
        groupId, artifactId, version, artifactId, version);

      this.mvnRepoURL = new URL("https://repo1.maven.org/maven2/" + mvnCentralPath);
    } catch (MalformedURLException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /**
   * @return URL for the local file used for the external URLClassLoader
   */
  public @NotNull URL getClassLoaderURL() throws MalformedURLException {
    if (!Files.exists(tempFilePath)) {
      final SystemTimer timer = new SystemTimer();
      Sonar.get().getLogger().info("Downloading dependency {}...", tempFilePath.getFileName());

      try (final InputStream inputStream = mvnRepoURL.openStream()) {
        Files.createDirectories(tempFilePath.getParent());
        Files.copy(inputStream, Files.createFile(tempFilePath), REPLACE_EXISTING);
      } catch (IOException exception) {
        throw new IllegalStateException(exception);
      }

      Sonar.get().getLogger().info("Finished downloading {} ({}s)!", tempFilePath.getFileName(), timer);
    }

    return tempFilePath.toUri().toURL();
  }
}
