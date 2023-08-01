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
import xyz.jonesdev.sonar.api.timer.DelayTimer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// Mostly taken from
// https://github.com/Elytrium/LimboAuth/blob/master/src/main/java/net/elytrium/limboauth/dependencies/BaseLibrary.java
public enum Dependency {
  JDBC_DRIVER(
    "com/mysql",
    "mysql-connector-j",
    "8.1.0"
  );

  private @NotNull final Path tempFilePath;
  private @NotNull final URL mvnRepoURL;

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

  public @NotNull URL getClassLoaderURL() throws MalformedURLException {
    if (!Files.exists(tempFilePath)) {
      final DelayTimer timer = new DelayTimer();
      Sonar.get().getLogger().info("Downloading dependency {}...", tempFilePath.getFileName());

      try (final InputStream inputStream = mvnRepoURL.openStream()) {
        Files.createDirectories(tempFilePath.getParent());
        Files.copy(inputStream, Files.createFile(tempFilePath), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException exception) {
        throw new IllegalStateException(exception);
      }

      Sonar.get().getLogger().info("Finished downloading {} ({}s)!",
        tempFilePath.getFileName(), timer.formattedDelay());
    }

    return tempFilePath.toUri().toURL();
  }
}
