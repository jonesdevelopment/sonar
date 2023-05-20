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

package jones.sonar.api.config;

import jones.sonar.api.chatcolor.ChatColor;
import jones.sonar.api.config.yml.YamlConfig;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public final class SonarConfiguration {
    private final YamlConfig yamlConfig;

    public SonarConfiguration(final File folder) {
        if (!folder.exists() && !folder.mkdir()) {
            throw new IllegalStateException("Could not create folder?!");
        }

        yamlConfig = new YamlConfig(folder, "config");
    }

    public String PREFIX;

    public String ACTION_BAR_LAYOUT;
    public Collection<String> ANIMATION;

    public int MINIMUM_PLAYERS_FOR_ATTACK;
    public int MAXIMUM_VERIFYING_PLAYERS;
    public int MAXIMUM_QUEUED_PLAYERS;
    public int MAXIMUM_QUEUE_POLLS;
    public int VERIFICATION_TIMEOUT;

    public void load() {
        Objects.requireNonNull(yamlConfig);

        yamlConfig.load();

        // General options
        MINIMUM_PLAYERS_FOR_ATTACK = yamlConfig.getInt("general.min-players-for-attack", 5);
        MAXIMUM_VERIFYING_PLAYERS = yamlConfig.getInt("general.max-verifying-players", Short.MAX_VALUE);
        MAXIMUM_QUEUED_PLAYERS = yamlConfig.getInt("general.max-queued-players", Short.MAX_VALUE);
        MAXIMUM_QUEUE_POLLS = yamlConfig.getInt("general.queue.max-polls", 10);

        VERIFICATION_TIMEOUT = yamlConfig.getInt("general.verification.timeout", 4500);

        // Message settings
        PREFIX = ChatColor.translateAlternateColorCodes('&',
                yamlConfig.getString("messages.prefix", "&e&lSonar &7» &f"));

        ACTION_BAR_LAYOUT = ChatColor.translateAlternateColorCodes('&', yamlConfig.getString(
                "messages.action-bar.layout",
                "&e&lSonar &3▪ &7Queued &f%queued% &3▪ &7Verifying &f%verifying% &3▪ &6%animation%"
        ));
        ANIMATION = yamlConfig.getStringList("messages.action-bar.animation", Arrays.asList("▙", "▛", "▜", "▟"));
    }
}
