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

package xyz.jonesdev.sonar.api.config;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.comments.format.YamlCommentFormat;
import org.simpleyaml.configuration.file.YamlFile;
import xyz.jonesdev.sonar.api.Sonar;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static xyz.jonesdev.sonar.api.Sonar.LINE_SEPARATOR;

@Getter
public final class SimpleYamlConfig {
  private final File file;
  private final YamlFile yaml;

  private static final List<String> HEADER = Arrays.asList(
    "",
    String.format("Running Sonar version %s on %s",
      Sonar.get().getVersion(), Sonar.get().getPlatform().getDisplayName()),
    "Need help or have questions? https://jonesdev.xyz/discord",
    "https://github.com/jonesdevelopment/sonar",
    ""
  );

  public SimpleYamlConfig(final File dataFolder, final String fileName) {
    this(new File(dataFolder, fileName + ".yml"), dataFolder);
  }

  private SimpleYamlConfig(final File file, final @NotNull File folder) {
    if (!folder.exists() && !folder.mkdir()) {
      throw new IllegalStateException("Could not create folder?!");
    }

    this.file = file;
    this.yaml = new YamlFile(file.getPath());
  }

  public void save() {
    try {
      yaml.save();
    } catch (IOException exception) {
      exception.printStackTrace(System.err);
    }
  }

  public void load() throws Exception {
    // https://github.com/jonesdevelopment/sonar/issues/26
    // Only load the configuration if the file already exists
    if (yaml.exists()) {
      yaml.loadWithComments();
    } else {
      yaml.createOrLoadWithComments();
    }

    // Always load the comment format and options
    yaml.setCommentFormat(YamlCommentFormat.DEFAULT);
    yaml.setHeader(String.join(LINE_SEPARATOR, HEADER));
  }

  public void set(final String path, final Object v) {
    yaml.set(path, v);
  }

  public int getInt(final String path, final int def) {
    yaml.addDefault(path, def);
    return yaml.getInt(path, def);
  }

  public boolean getBoolean(final String path, final boolean def) {
    yaml.addDefault(path, def);
    return yaml.getBoolean(path, def);
  }

  public String getString(final String path, final String def) {
    final Object object = getObject(path, def);
    if (object instanceof String) {
      return ((String) object).toUpperCase();
    }
    Sonar.get().getLogger().info("[config] Migrated {} to {}", path, def);
    set(path, def);
    return def;
  }

  public Object getObject(final String path, final Object def) {
    yaml.addDefault(path, def);
    return yaml.get(path, def);
  }

  public List<String> getStringList(final String path, final List<String> def) {
    yaml.addDefault(path, def);
    return yaml.getStringList(path);
  }
}
