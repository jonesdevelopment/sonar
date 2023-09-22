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

package xyz.jonesdev.sonar.api;

import lombok.Getter;

@Getter
public final class SonarVersion {
  static final SonarVersion GET = new SonarVersion();
  private final String semanticVersion, full, formatted, commitSHA;
  private final int build;

  SonarVersion() {
    final Package manifest = Sonar.class.getPackage();
    final String versionString = manifest.getImplementationVersion();

    if (versionString == null) {
      this.full = "<could not retrieve version>";
      this.semanticVersion = "<unknown>";
      this.commitSHA = "<unknown>";
      this.build = 0;
      this.formatted = full;
      Sonar.get().getLogger().error("Could not find version information (Is the manifest missing?)");
      return;
    }

    this.full = versionString;
    this.semanticVersion = versionString.split("-")[0];
    this.commitSHA = versionString.split("-")[1];
    this.build = Integer.parseInt(versionString.split("-")[2]);
    this.formatted = semanticVersion + " build " + build + " (" + commitSHA + ")";
  }

  @Override
  public String toString() {
    return formatted;
  }
}
