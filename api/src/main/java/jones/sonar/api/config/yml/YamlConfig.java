/*
 *  Copyright (c) 2023, jones (https://jonesdev.xyz) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jones.sonar.api.config.yml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class YamlConfig {
    private final Yaml yaml;
    private final File file;
    private Map<String, Object> config;

    public YamlConfig(final File dataFolder, final String fileName) {
        this(new File(dataFolder, fileName + ".yml"));
    }

    private YamlConfig(final File file) {
        this.file = file;

        final DumperOptions options = new DumperOptions();

        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        yaml = new Yaml(options);
    }

    public void load() {
        try {
            file.createNewFile();

            try (final InputStream is = Files.newInputStream(file.toPath())) {
                try {
                    config = yaml.load(is);
                } catch (YAMLException exception) {
                    throw new RuntimeException("Invalid configuration encountered - this is a configuration error and NOT a bug!", exception);
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
        Collection<String> defaultStrings = new ArrayList<>();
        def.forEach(string -> defaultStrings.add(string.replace("§", "&")));
        return get(path, defaultStrings);
    }
}
