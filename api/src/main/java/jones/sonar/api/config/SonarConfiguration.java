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

  public String HEADER, FOOTER;
  public String TOO_MANY_PLAYERS;
  public String TOO_MANY_VERIFICATIONS;
  public String ALREADY_VERIFYING;
  public String BLACKLISTED;
  public String NO_SERVER_FOUND;

  public void load() {
    Objects.requireNonNull(yamlConfig);

    yamlConfig.load();

    // Message settings
    PREFIX = formatString(yamlConfig.getString("messages.prefix", "&e&lSonar &7» &f"));

    // General options
    MINIMUM_PLAYERS_FOR_ATTACK = yamlConfig.getInt("general.min-players-for-attack", 5);
    MAXIMUM_VERIFYING_PLAYERS = yamlConfig.getInt("general.max-verifying-players", Short.MAX_VALUE / 4);
    MAXIMUM_QUEUED_PLAYERS = yamlConfig.getInt("general.max-queued-players", Short.MAX_VALUE / 4);
    MAXIMUM_QUEUE_POLLS = yamlConfig.getInt("general.queue.max-polls", 10);

    VERIFICATION_TIMEOUT = yamlConfig.getInt("general.verification.timeout", 4500);
    VERIFICATIONS_PER_MINUTE = yamlConfig.getInt("general.verification.max-per-minute", 3);

    HEADER = fromList(yamlConfig.getStringList("general.verification.message.header",
      Arrays.asList(
        "&e&lSonar"
      )));
    FOOTER = fromList(yamlConfig.getStringList("general.verification.message.footer",
      Arrays.asList(
        "&7If you believe that this is an error, contact an administrator."
      )));
    TOO_MANY_PLAYERS = fromList(yamlConfig.getStringList("general.verification.too-many-players",
      Arrays.asList(
        "%header%",
        "&cToo many players are currently trying to log in.",
        "&7Please try again in a few seconds.",
        "%footer%"
      )));
    TOO_MANY_VERIFICATIONS = fromList(yamlConfig.getStringList("general.verification.too-many-verifications",
      Arrays.asList(
        "%header%",
        "&cYour ip address is denied from logging into the server.",
        "%footer%"
      )));
    ALREADY_VERIFYING = fromList(yamlConfig.getStringList("general.verification.already-verifying",
      Arrays.asList(
        "%header%",
        "&cYour ip address is currently queued for verification.",
        "&cPlease wait a few minutes before trying to verify again.",
        "%footer%"
      )));
    BLACKLISTED = fromList(yamlConfig.getStringList("general.verification.blacklisted",
      Arrays.asList(
        "%header%",
        "&cYour ip address is temporarily denied from verifying.",
        "&cPlease wait a few minutes before trying to verify again.",
        "%footer%"
      )));
    NO_SERVER_FOUND = fromList(yamlConfig.getStringList("general.verification.no-server",
      Arrays.asList(
        "%header%",
        "&cThere is currently no server available.",
        "&cPlease try again in a few minutes.",
        "%footer%"
      )));

    ACTION_BAR_LAYOUT = formatString(yamlConfig.getString(
      "messages.action-bar.layout",
      "&e&lSonar" +
        " &3▪ &7Queued &f%queued%" +
        " &3▪ &7Verifying &f%verifying%" +
        " &3▪ &7Blacklisted &f%blacklisted%" +
        " &3▪ &7Total &f%total%" +
        " &3▪ &6%animation%"
    ));
    ANIMATION = yamlConfig.getStringList("messages.action-bar.animation",
      Arrays.asList("▙", "▛", "▜", "▟")
    );
  }

  private String fromList(final Collection<String> list) {
    return formatString(String.join("\n", list));
  }

  private String formatString(final String string) {
    return ChatColor.translateAlternateColorCodes('&', string)
      .replace("%prefix%", PREFIX == null ? "" : PREFIX)
      .replace("%header%", HEADER == null ? "" : HEADER)
      .replace("%footer%", FOOTER == null ? "" : FOOTER);
  }
}
