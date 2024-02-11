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

package xyz.jonesdev.sonar.api.config;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.MemorySection;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static xyz.jonesdev.sonar.api.config.SonarConfiguration.LOGGER;

@Getter
public final class SimpleYamlConfig {
  private final File file;
  private final YamlFile yaml;

  public SimpleYamlConfig(final @NotNull File file) {
    this.file = file;
    this.yaml = new YamlFile(file.getPath());
  }

  private void replaceConfigFile(final @NotNull URL defaultData) throws Exception {
    try (final InputStream inputStream = defaultData.openStream()) {
      Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public void load(final @NotNull URL defaultData) throws Exception {
    final boolean exists = yaml.exists();

    // Copy the configuration from the translations
    if (!exists) {
      replaceConfigFile(defaultData);
    }

    // Load the configuration
    yaml.loadWithComments();

    // I hate my life... but this works... somehow
    if (exists) {
      // First, we store all values from the current (old) config
      final Map<String, Object> values = yaml.getValues(true);
      // Then, we replace the old config file with the default one
      replaceConfigFile(defaultData);
      // Re-load the configuration
      yaml.loadWithComments();
      // Loop through the new values from the new configuration
      yaml.getValues(true).forEach((path, v) -> {
        final Object value = values.get(path);
        // Make sure we actually need to update the value
        if (value != null && !value.equals(v)
          && !(v instanceof MemorySection)
          && !(value instanceof MemorySection)) {
          // Set the old values to the new values
          yaml.set(path, value);
        }
      });
      // Save the yaml file to ensure we keep the original values
      yaml.save();
    }
  }

  public void set(final String path, final Object v) {
    yaml.set(path, v);
  }

  public int getInt(final String path, final int def) {
    yaml.addDefault(path, def);
    return yaml.getInt(path, def);
  }

  public int getInt(final String path) {
    if (!yaml.contains(path)) {
      LOGGER.warn("Could not find {} in {}.", path, file.getName());
      return 0;
    }
    return yaml.getInt(path);
  }

  public boolean getBoolean(final String path, final boolean def) {
    yaml.addDefault(path, def);
    return yaml.getBoolean(path, def);
  }

  public boolean getBoolean(final String path) {
    if (!yaml.contains(path)) {
      LOGGER.warn("Could not find {} in {}.", path, file.getName());
      return false;
    }
    return yaml.getBoolean(path);
  }

  public String getString(final String path, final String def) {
    final Object object = getObject(path, def);
    if (object instanceof String) {
      return (String) object;
    }
    LOGGER.info("[config] Migrated {} to {}", path, def);
    set(path, def);
    return def;
  }

  public @NotNull String getString(final String path) {
    if (!yaml.contains(path)) {
      LOGGER.warn("Could not find {} in {}.", path, file.getName());
      return "";
    }
    final Object object = getObject(path);
    if (object instanceof String) {
      return (String) object;
    }
    throw new IllegalStateException("Invalid entry " + path);
  }

  public Object getObject(final String path, final Object def) {
    yaml.addDefault(path, def);
    return yaml.get(path, def);
  }

  public Object getObject(final String path) {
    return yaml.get(path);
  }

  public List<String> getStringList(final String path, final List<String> def) {
    yaml.addDefault(path, def);
    return yaml.getStringList(path);
  }

  public List<String> getStringList(final String path) {
    if (!yaml.contains(path)) {
      LOGGER.warn("Could not find {} in {}.", path, file.getName());
      return new ArrayList<>(0);
    }
    return yaml.getStringList(path);
  }

  public List<Integer> getIntList(final String path, final List<Integer> def) {
    yaml.addDefault(path, def);
    return yaml.getIntegerList(path);
  }

  public List<Integer> getIntList(final String path) {
    if (!yaml.contains(path)) {
      LOGGER.warn("Could not find {} in {}.", path, file.getName());
      return new ArrayList<>(0);
    }
    return yaml.getIntegerList(path);
  }
}
