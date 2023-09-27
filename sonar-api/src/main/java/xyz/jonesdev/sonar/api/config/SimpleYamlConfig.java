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
import org.simpleyaml.configuration.comments.format.YamlCommentFormat;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.exceptions.InvalidConfigurationException;
import xyz.jonesdev.sonar.api.Sonar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static xyz.jonesdev.sonar.api.Sonar.LINE_SEPARATOR;

@Getter
public final class SimpleYamlConfig {
  private final File file;
  private final YamlFile yaml;

  private static final List<String> HEADER = Arrays.asList(
    "",
    "Sonar anti-bot version " + Sonar.get().getVersion(),
    " Currently running on: " + Sonar.get().getServer().getPlatform().getDisplayName(),
    " Need help? https://jonesdev.xyz/discord",
    ""
  );

  public SimpleYamlConfig(final File dataFolder, final String fileName) {
    this(new File(dataFolder, fileName + ".yml"), dataFolder);
  }

  private SimpleYamlConfig(final File file, final File folder) {
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

  public void load() {
    String backupContent = null;
    try {
      // https://github.com/jonesdevelopment/sonar/issues/26
      // Only load the configuration if the file already exists
      if (yaml.exists()) {
        try {
          // Load all lines from the existing files before trying to load/overwrite it
          backupContent = String.join(LINE_SEPARATOR, Files.readAllLines(file.toPath()));
        } catch (IOException exception) {
          Sonar.get().getLogger().error("Error while creating backup configuration: {}", exception);
        }
        yaml.loadWithComments();
      } else {
        yaml.createOrLoadWithComments();
      }

      // Always load the comment format and options
      yaml.setCommentFormat(YamlCommentFormat.PRETTY);

      yaml.options().headerFormatter().commentPrefix("# ");
      yaml.setHeader(String.join(LINE_SEPARATOR, HEADER));
    } catch (InvalidConfigurationException exception) {
      Sonar.get().getLogger().error("Invalid configuration: {}", exception);

      // Try to save a backup configuration to prevent data loss
      if (backupContent != null) {
        final File backupFile = new File(file.getParentFile(), file.getName() + ".broken");
        try (final FileWriter fileWriter = new FileWriter(backupFile, false)) {
          fileWriter.write(backupContent);
          Sonar.get().getLogger().info("Saved backup configuration to {}", backupFile.getName());
        } catch (Throwable throwable) {
          Sonar.get().getLogger().warn("Could not load from backup configuration: {}", throwable);
        }
      }
    } catch (IOException exception) {
      Sonar.get().getLogger().error("Error while loading configuration: {}", exception);
    }
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
    yaml.addDefault(path, def);
    return yaml.getString(path, def);
  }

  public List<String> getStringList(final String path, final List<String> def) {
    yaml.addDefault(path, def);
    return yaml.getStringList(path);
  }
}
