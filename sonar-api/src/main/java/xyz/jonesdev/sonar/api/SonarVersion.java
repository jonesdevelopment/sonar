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

package xyz.jonesdev.sonar.api;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

@Getter
public final class SonarVersion {
  static final SonarVersion INSTANCE = new SonarVersion();

  private final String version, formatted, gitBranch, gitCommit;

  SonarVersion() {
    // No need to null-check since we'll always just throw an exception if
    // we can't find the version information in this file.
    final Manifest manifest = getManifest();

    this.version = manifest.getMainAttributes().getValue("Implementation-Version");
    this.gitBranch = manifest.getMainAttributes().getValue("Git-Branch");
    this.gitCommit = manifest.getMainAttributes().getValue("Git-Commit");
    this.formatted = version + " (" + gitCommit + ")";
  }

  // Taken from
  // https://github.com/PaperMC/Velocity/pull/1336/
  private static @NotNull Manifest getManifest() {
    final String classLocation = "/" + Sonar.class.getName().replace(".", "/") + ".class";
    final URL resource = Sonar.class.getResource(classLocation);

    if (resource == null) {
      throw new IllegalStateException("Could not find version information. Is the manifest missing?");
    }

    final String classFilePath = resource.toString().replace("\\", "/");
    final String archivePath = classFilePath.substring(0, classFilePath.length() - classLocation.length());
    final String manifestPath = archivePath + "/META-INF/MANIFEST.MF";

    try (final InputStream stream = new URL(manifestPath).openStream()) {
      return new Manifest(stream);
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Override
  public String toString() {
    return formatted;
  }
}
