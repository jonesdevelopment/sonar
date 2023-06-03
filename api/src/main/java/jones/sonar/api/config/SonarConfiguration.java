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
    yamlConfig = new YamlConfig(folder, "config");
  }

  public String PREFIX;

  public String ACTION_BAR_LAYOUT;
  public Collection<String> ANIMATION;

  public int MINIMUM_PLAYERS_FOR_ATTACK;
  public int MAXIMUM_VERIFYING_PLAYERS;
  public int MAXIMUM_ONLINE_PER_IP;
  public int MAXIMUM_QUEUED_PLAYERS;
  public int MAXIMUM_QUEUE_POLLS;
  public int VERIFICATION_TIMEOUT;
  public int VERIFICATIONS_PER_MINUTE;

  public String HEADER, FOOTER;
  public String TOO_MANY_PLAYERS;
  public String TOO_MANY_VERIFICATIONS;
  public String TOO_MANY_ONLINE_PER_IP;
  public String ALREADY_VERIFYING;
  public String BLACKLISTED;

  public String VERBOSE_SUBSCRIBED;
  public String VERBOSE_UNSUBSCRIBED;
  public String RELOADING;
  public String RELOADED;

  public void load() {
    Objects.requireNonNull(yamlConfig);

    yamlConfig.load();

    // Message settings
    PREFIX = formatString(yamlConfig.getString("messages.prefix", "&e&lSonar &7» &f"));

    // General options
    MAXIMUM_ONLINE_PER_IP = yamlConfig.getInt("general.max-online-per-ip", 3);
    MINIMUM_PLAYERS_FOR_ATTACK = yamlConfig.getInt("general.min-players-for-attack", 5);
    MAXIMUM_VERIFYING_PLAYERS = yamlConfig.getInt("general.max-verifying-players", Short.MAX_VALUE / 4);
    MAXIMUM_QUEUED_PLAYERS = yamlConfig.getInt("general.max-queued-players", Short.MAX_VALUE / 4);
    MAXIMUM_QUEUE_POLLS = yamlConfig.getInt("general.queue.max-polls", 10);

    VERIFICATION_TIMEOUT = yamlConfig.getInt("general.verification.timeout", 4500);
    VERIFICATIONS_PER_MINUTE = yamlConfig.getInt("general.verification.max-per-minute", 3);

    RELOADING = formatString(yamlConfig.getString("general.reload.start",
      "%prefix%Reloading Sonar..."
    ));
    RELOADED = formatString(yamlConfig.getString("general.reload.finish",
      "%prefix%&aSuccessfully reloaded &7(%taken%ms)"
    ));

    VERBOSE_SUBSCRIBED = formatString(yamlConfig.getString("general.verbose.subscribed",
      "%prefix%You are now viewing Sonar verbose."
    ));
    VERBOSE_UNSUBSCRIBED = formatString(yamlConfig.getString("general.verbose.unsubscribed",
      "%prefix%You are no longer viewing Sonar verbose."
    ));

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
        "&cYour IP address is denied from logging into the server.",
        "%footer%"
      )));
    ALREADY_VERIFYING = fromList(yamlConfig.getStringList("general.verification.already-verifying",
      Arrays.asList(
        "%header%",
        "&cYour IP address is currently queued for verification.",
        "&cPlease wait a few minutes before trying to verify again.",
        "%footer%"
      )));
    BLACKLISTED = fromList(yamlConfig.getStringList("general.verification.blacklisted",
      Arrays.asList(
        "%header%",
        "&cYour IP address is temporarily denied from verifying.",
        "&cPlease wait a few minutes before trying to verify again.",
        "%footer%"
      )));
    TOO_MANY_ONLINE_PER_IP = fromList(yamlConfig.getStringList("general.too-many-online-per-ip",
      Arrays.asList(
        "%header%",
        "&cThere are too many players online with your IP address.",
        "%footer%"
      )));

    ACTION_BAR_LAYOUT = formatString(yamlConfig.getString(
      "messages.action-bar.layout",
      "%prefix%&fQueued &7%queued%" +
        "  &fVerifying &7%verifying%" +
        "  &fBlacklisted &7%blacklisted%" +
        "  &fTotal &7%total%" +
        "  &a%animation%"
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
