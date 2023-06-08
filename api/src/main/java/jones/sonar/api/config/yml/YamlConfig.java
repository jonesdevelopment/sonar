/*
 * Copyright (C) 2023, jones
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

package jones.sonar.api.config.yml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class YamlConfig {
  private final Yaml yaml;
  private final File folder, file;
  private Map<String, Object> config;

  public YamlConfig(final File dataFolder, final String fileName) {
    this(new File(dataFolder, fileName + ".yml"), dataFolder);
  }

  private YamlConfig(final File file, final File folder) {
    this.file = file;
    this.folder = folder;

    final DumperOptions options = new DumperOptions();

    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    yaml = new Yaml(options);
  }

  public void load() {
    try {
      if (!folder.exists() && !folder.mkdir()) {
        throw new IllegalStateException("Could not create folder?!");
      }

      file.createNewFile();

      try (final InputStream is = Files.newInputStream(file.toPath())) {
        try {
          config = yaml.load(is);
        } catch (YAMLException exception) {
          throw new IllegalStateException("Invalid configuration encountered!", exception);
        }
      }

      if (config == null) {
        config = new HashMap<>();
      } else {
        config = new HashMap<>(config);
      }
    } catch (IOException exception) {
      throw new RuntimeException("Could not load configuration!", exception);
    }
  }

  private <T> T get(String path, T def) {
    return get(path, def, config);
  }

  @SuppressWarnings("unchecked")
  private <T> T get(String path, T def, Map submap) {
    int index = path.indexOf('.');

    if (index == -1) {
      Object val = submap.get(path);
      if (val == null && def != null) {
        val = def;
        submap.put(path, def);
        save();
      }
      return (T) val;
    } else {
      String first = path.substring(0, index);
      String second = path.substring(index + 1);
      Map sub = (Map) submap.get(first);
      if (sub == null) {
        sub = new LinkedHashMap<>();
        submap.put(first, sub);
      }
      return get(second, def, sub);
    }
  }

  public void set(String path, Object val) {
    set(path, val, config);
  }

  @SuppressWarnings("unchecked")
  private void set(String path, Object val, Map submap) {
    int index = path.indexOf('.');
    if (index == -1) {
      if (val == null) {
        submap.remove(path);
      } else {
        submap.put(path, val);
      }

      save();
    } else {
      String first = path.substring(0, index);
      String second = path.substring(index + 1);
      Map sub = (Map) submap.get(first);

      if (sub == null) {
        sub = new LinkedHashMap<>();
        submap.put(first, sub);
      }

      set(second, val, sub);
    }
  }

  private void save() {
    try (final Writer wr = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
      yaml.dump(config, wr);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  public double getDouble(String path, double def) {
    return get(path, def);
  }

  public int getInt(String path, int def) {
    return get(path, def);
  }

  public String getString(String path, String def) {
    return get(path, def.replace("§", "&"));
  }

  public boolean getBoolean(String path, boolean def) {
    return get(path, def);
  }

  public Collection<?> getList(String path, Collection<?> def) {
    return get(path, def);
  }

  public Collection<Integer> getIntList(String path, Collection<Integer> def) {
    return get(path, def);
  }

  public Collection<String> getStringList(String path, Collection<String> def) {
    return get(path, def);
  }
}
