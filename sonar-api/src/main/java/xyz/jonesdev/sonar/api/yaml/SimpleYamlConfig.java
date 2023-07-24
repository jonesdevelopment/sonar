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

package xyz.jonesdev.sonar.api.yaml;

import lombok.Getter;
import org.simpleyaml.configuration.comments.format.YamlCommentFormat;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class SimpleYamlConfig {
  @Getter
  private final YamlFile yaml;

  public SimpleYamlConfig(final File dataFolder, final String fileName) {
    this(new File(dataFolder, fileName + ".yml"), dataFolder);
  }

  private SimpleYamlConfig(final File file, final File folder) {
    if (!folder.exists() && !folder.mkdir()) {
      throw new IllegalStateException("Could not create folder?!");
    }

    this.yaml = new YamlFile(file.getPath());
  }

  public void save() {
    try {
      yaml.save();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  public void load() {
    try {
      yaml.setCommentFormat(YamlCommentFormat.PRETTY);
      yaml.createOrLoadWithComments();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  public void set(final String path, final Object v) {
    yaml.set(path, v);
  }

  public int getInt(final String path, final int def) {
    if (!yaml.contains(path)) {
      yaml.set(path, def);
    }
    return yaml.getInt(path, def);
  }

  public boolean getBoolean(final String path, final boolean def) {
    if (!yaml.contains(path)) {
      yaml.set(path, def);
    }
    return yaml.getBoolean(path, def);
  }

  public String getString(final String path, final String def) {
    if (!yaml.contains(path)) {
      yaml.set(path, def);
    }
    return yaml.getString(path, def);
  }

  public List<String> getStringList(final String path, final List<String> def) {
    if (!yaml.contains(path)) {
      yaml.set(path, def);
    }
    return yaml.getStringList(path);
  }
}
