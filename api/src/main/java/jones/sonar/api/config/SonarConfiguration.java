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
  public int VERIFICATIONS_PER_MINUTE;

  public void load() {
    Objects.requireNonNull(yamlConfig);

    yamlConfig.load();

    // General options
    MINIMUM_PLAYERS_FOR_ATTACK = yamlConfig.getInt("general.min-players-for-attack", 5);
    MAXIMUM_VERIFYING_PLAYERS = yamlConfig.getInt("general.max-verifying-players", Short.MAX_VALUE / 4);
    MAXIMUM_QUEUED_PLAYERS = yamlConfig.getInt("general.max-queued-players", Short.MAX_VALUE / 4);
    MAXIMUM_QUEUE_POLLS = yamlConfig.getInt("general.queue.max-polls", 10);

    VERIFICATION_TIMEOUT = yamlConfig.getInt("general.verification.timeout", 4500);
    VERIFICATIONS_PER_MINUTE = yamlConfig.getInt("general.verification.max-per-minute", 3);

    // Message settings
    PREFIX = ChatColor.translateAlternateColorCodes('&',
      yamlConfig.getString("messages.prefix", "&e&lSonar &7» &f"));

    ACTION_BAR_LAYOUT = ChatColor.translateAlternateColorCodes('&', yamlConfig.getString(
      "messages.action-bar.layout",
      "&e&lSonar" +
        " &3▪ &7Queued &f%queued%" +
        " &3▪ &7Verifying &f%verifying%" +
        " &3▪ &7Blacklisted &f%blacklisted%" +
        " &3▪ &7Total &f%total%" +
        " &3▪ &6%animation%"
    ));
    ANIMATION = yamlConfig.getStringList("messages.action-bar.animation", Arrays.asList("▙", "▛", "▜", "▟"));
  }
}
