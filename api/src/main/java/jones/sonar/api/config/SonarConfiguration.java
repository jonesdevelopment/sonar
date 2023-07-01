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

import jones.sonar.api.config.yml.YamlConfig;
import lombok.Getter;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public final class SonarConfiguration {
  @Getter
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
  public int MAXIMUM_LOGIN_PACKETS;
  public int VERIFICATION_TIMEOUT;
  public int VERIFICATION_DELAY;

  public boolean ENABLE_COMPRESSION;
  public boolean ENABLE_VERIFICATION;
  public boolean LOG_DURING_ATTACK;

  public String HEADER, FOOTER;
  public String TOO_MANY_PLAYERS;
  public String TOO_FAST_RECONNECT;
  public String TOO_MANY_ONLINE_PER_IP;
  public String ALREADY_VERIFYING;
  public String ALREADY_QUEUED;
  public String BLACKLISTED;
  public String UNEXPECTED_ERROR;

  public String INCORRECT_COMMAND_USAGE;
  public String INCORRECT_IP_ADDRESS;
  public String SUB_COMMAND_NO_PERM;
  public String ILLEGAL_IP_ADDRESS;
  public String PLAYERS_ONLY;
  public String CONSOLE_ONLY;
  public String COMMAND_COOL_DOWN;
  public String COMMAND_COOL_DOWN_LEFT;

  public String BLACKLIST_EMPTY;
  public String BLACKLIST_ADD;
  public String BLACKLIST_DUPLICATE;
  public String BLACKLIST_NOT_FOUND;
  public String BLACKLIST_REMOVE;
  public String BLACKLIST_CLEARED;
  public String BLACKLIST_SIZE;

  public String WHITELIST_ADD;
  public String WHITELIST_DUPLICATE;
  public String WHITELIST_NOT_FOUND;
  public String WHITELIST_REMOVE;
  public String WHITELIST_SIZE;

  public String VERBOSE_SUBSCRIBED;
  public String VERBOSE_UNSUBSCRIBED;
  public String RELOADING;
  public String RELOADED;

  public boolean LOCKDOWN_ENABLED;
  public boolean LOCKDOWN_ENABLE_NOTIFY;
  public boolean LOCKDOWN_LOG_ATTEMPT;
  public String LOCKDOWN_DISCONNECT;
  public String LOCKDOWN_ACTIVATED;
  public String LOCKDOWN_DEACTIVATED;
  public String LOCKDOWN_NOTIFICATION;
  public String LOCKDOWN_CONSOLE_LOG;

  public boolean DATABASE_ENABLED;
  public String DATABASE_URL;
  public String DATABASE_NAME;
  public String DATABASE_USERNAME;
  public String DATABASE_PASSWORD;
  public int DATABASE_PORT;

  public void load() {
    Objects.requireNonNull(yamlConfig);

    yamlConfig.load();

    // Message settings
    PREFIX = formatString(yamlConfig.getString("messages.prefix", "&e&lSonar &7» &f"));

    // General options
    MAXIMUM_ONLINE_PER_IP = yamlConfig.getInt("general.max-online-per-ip", 3);
    MINIMUM_PLAYERS_FOR_ATTACK = yamlConfig.getInt("general.min-players-for-attack", 5);

    LOCKDOWN_ENABLED = yamlConfig.getBoolean("general.lockdown.enabled", false);
    LOCKDOWN_LOG_ATTEMPT = yamlConfig.getBoolean("general.lockdown.log-attempt", true);
    LOCKDOWN_ENABLE_NOTIFY = yamlConfig.getBoolean("general.lockdown.notify-admins", true);

    DATABASE_ENABLED = yamlConfig.getBoolean("general.database.enabled", false);
    DATABASE_NAME = yamlConfig.getString("general.database.name", "sonar");
    DATABASE_URL = yamlConfig.getString("general.database.url", "localhost");
    DATABASE_PORT = yamlConfig.getInt("general.database.port", 3306);
    DATABASE_USERNAME = yamlConfig.getString("general.database.username", "root");
    DATABASE_PASSWORD = yamlConfig.getString("general.database.password", "");

    MAXIMUM_QUEUED_PLAYERS = yamlConfig.getInt("general.queue.max-players", 8192);
    MAXIMUM_QUEUE_POLLS = yamlConfig.getInt("general.queue.max-polls", 10);

    ENABLE_VERIFICATION = yamlConfig.getBoolean("general.verification.enabled", true);
    LOG_DURING_ATTACK = yamlConfig.getBoolean("general.verification.log-during-attack", false);
    ENABLE_COMPRESSION = yamlConfig.getBoolean("general.verification.enable-compression", true);
    VERIFICATION_TIMEOUT = yamlConfig.getInt("general.verification.timeout", 4000);
    MAXIMUM_LOGIN_PACKETS = yamlConfig.getInt("general.verification.max-login-packets", 20);
    MAXIMUM_VERIFYING_PLAYERS = yamlConfig.getInt("general.verification.max-players", 1024);
    VERIFICATION_DELAY = yamlConfig.getInt("general.verification.rejoin-delay", 8000);

    LOCKDOWN_ACTIVATED = formatString(yamlConfig.getString("messages.lockdown.enabled",
      "%prefix%The server is now in lockdown mode."
    ));
    LOCKDOWN_DEACTIVATED = formatString(yamlConfig.getString("messages.lockdown.disabled",
      "%prefix%The server is no longer in lockdown mode."
    ));
    LOCKDOWN_NOTIFICATION = formatString(yamlConfig.getString("messages.lockdown.notification",
      "%prefix%&aHey, the server is currently in lockdown mode. If you want turn the lockdown mode off, type &f/sonar lockdown&a."
    ));
    LOCKDOWN_CONSOLE_LOG = formatString(yamlConfig.getString("messages.lockdown.console-log",
      "%player% (%ip%, %protocol%) tried to join during lockdown mode."
    ));
    LOCKDOWN_DISCONNECT = fromList(yamlConfig.getStringList("messages.lockdown.disconnect-message",
      Arrays.asList(
        "%header%",
        "&cThe server is currently locked down, please try again later.",
        "%footer%"
      )));

    RELOADING = formatString(yamlConfig.getString("messages.reload.start",
      "%prefix%Reloading Sonar..."
    ));
    RELOADED = formatString(yamlConfig.getString("messages.reload.finish",
      "%prefix%&aSuccessfully reloaded &7(%taken%ms)"
    ));

    VERBOSE_SUBSCRIBED = formatString(yamlConfig.getString("messages.verbose.subscribed",
      "%prefix%You are now viewing Sonar verbose."
    ));
    VERBOSE_UNSUBSCRIBED = formatString(yamlConfig.getString("messages.verbose.unsubscribed",
      "%prefix%You are no longer viewing Sonar verbose."
    ));

    INCORRECT_COMMAND_USAGE = formatString(yamlConfig.getString("messages.incorrect-command-usage",
      "%prefix%&cUsage: /sonar %usage%"
    ));
    INCORRECT_IP_ADDRESS = formatString(yamlConfig.getString("messages.invalid-ip-address",
      "%prefix%The IP address you provided seems to be invalid."
    ));
    ILLEGAL_IP_ADDRESS = formatString(yamlConfig.getString("messages.illegal-ip-address",
      "%prefix%The IP address you provided seems to be either a local or loopback IP."
    ));
    PLAYERS_ONLY = formatString(yamlConfig.getString("messages.players-only",
      "%prefix%&cYou can only execute this command as a player."
    ));
    CONSOLE_ONLY = formatString(yamlConfig.getString("messages.console-only",
      "%prefix%&cFor security reasons, you can only execute this command through console."
    ));
    COMMAND_COOL_DOWN = formatString(yamlConfig.getString("messages.command-cool-down",
      "%prefix%&cYou can only execute this command every 0.5 seconds."
    ));
    COMMAND_COOL_DOWN_LEFT = formatString(yamlConfig.getString("messages.command-cool-down-left",
      "%prefix%&cPlease wait another &l%time-left%s&r&c."
    ));
    SUB_COMMAND_NO_PERM = formatString(yamlConfig.getString("messages.sub-command-no-permission",
      "%prefix%&cYou do not have permission to execute this subcommand. &7(%permission%)"
    ));

    BLACKLIST_EMPTY = formatString(yamlConfig.getString("messages.blacklist.empty",
      "%prefix%The blacklist is currently empty. Therefore, no IP addresses were removed from the blacklist."
    ));
    BLACKLIST_CLEARED = formatString(yamlConfig.getString("messages.blacklist.cleared",
      "%prefix%You successfully removed a total of %removed% IP address(es) from the blacklist."
    ));
    BLACKLIST_SIZE = formatString(yamlConfig.getString("messages.blacklist.size",
      "%prefix%The blacklist currently contains %amount% IP address(es)."
    ));
    BLACKLIST_ADD = formatString(yamlConfig.getString("messages.blacklist.added",
      "%prefix%Successfully added %ip% to the blacklist."
    ));
    BLACKLIST_REMOVE = formatString(yamlConfig.getString("messages.blacklist.removed",
      "%prefix%Successfully removed %ip% from the blacklist."
    ));
    BLACKLIST_DUPLICATE = formatString(yamlConfig.getString("messages.blacklist.duplicate-ip",
      "%prefix%The IP address you provided is already blacklisted."
    ));
    BLACKLIST_NOT_FOUND = formatString(yamlConfig.getString("messages.blacklist.ip-not-found",
      "%prefix%The IP address you provided is not blacklisted."
    ));

    WHITELIST_SIZE = formatString(yamlConfig.getString("messages.whitelist.size",
      "%prefix%The whitelist currently contains %amount% IP address(es)."
    ));
    WHITELIST_ADD = formatString(yamlConfig.getString("messages.whitelist.added",
      "%prefix%Successfully added %ip% to the whitelist."
    ));
    WHITELIST_REMOVE = formatString(yamlConfig.getString("messages.whitelist.removed",
      "%prefix%Successfully removed %ip% from the whitelist."
    ));
    WHITELIST_DUPLICATE = formatString(yamlConfig.getString("messages.whitelist.duplicate-ip",
      "%prefix%The IP address you provided is already whitelisted."
    ));
    WHITELIST_NOT_FOUND = formatString(yamlConfig.getString("messages.whitelist.ip-not-found",
      "%prefix%The IP address you provided is not whitelisted."
    ));

    HEADER = fromList(yamlConfig.getStringList("messages.verification.header",
      Arrays.asList(
        "&e&lSonar"
      )));
    FOOTER = fromList(yamlConfig.getStringList("messages.verification.footer",
      Arrays.asList(
        "&7If you believe that this is an error, contact an administrator."
      )));
    TOO_MANY_PLAYERS = fromList(yamlConfig.getStringList("messages.verification.too-many-players",
      Arrays.asList(
        "%header%",
        "&6Too many players are currently trying to log in, try again later.",
        "&7Please wait a few seconds before trying to join again.",
        "%footer%"
      )));
    TOO_FAST_RECONNECT = fromList(yamlConfig.getStringList("messages.verification.too-fast-reconnect",
      Arrays.asList(
        "%header%",
        "&6You reconnected too fast, try again later.",
        "&7Please wait a few seconds before trying to verify again.",
        "%footer%"
      )));
    ALREADY_VERIFYING = fromList(yamlConfig.getStringList("messages.verification.already-verifying",
      Arrays.asList(
        "%header%",
        "&cYour IP address is currently being verified.",
        "&cPlease wait a few seconds before trying to verify again.",
        "%footer%"
      )));
    ALREADY_QUEUED = fromList(yamlConfig.getStringList("messages.verification.already-queued",
      Arrays.asList(
        "%header%",
        "&cYour IP address is currently queued for verification.",
        "&cPlease wait a few minutes before trying to verify again.",
        "%footer%"
      )));
    BLACKLISTED = fromList(yamlConfig.getStringList("messages.verification.blacklisted",
      Arrays.asList(
        "%header%",
        "&cYour IP address is temporarily denied from verifying.",
        "&cPlease wait a few minutes before trying to verify again.",
        "&6False positive? &7https://discord.jonesdev.xyz/",
        "%footer%"
      )));
    UNEXPECTED_ERROR = fromList(yamlConfig.getStringList("messages.verification.unexpected-error",
      Arrays.asList(
        "%header%",
        "&6An unexpected error occurred when trying to process your connection.",
        "&7Please wait a few seconds before trying to verify again.",
        "%footer%"
      )));
    TOO_MANY_ONLINE_PER_IP = fromList(yamlConfig.getStringList("messages.too-many-online-per-ip",
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
        "  &fMemory &7≅ %used-memory%" +
        "  &a&l%animation%"
    ));
    ANIMATION = yamlConfig.getStringList("messages.action-bar.animation",
      Arrays.asList("◜", "◝", "◞", "◟") // ▙ ▛ ▜ ▟
    );
  }

  private String fromList(final Collection<String> list) {
    return formatString(String.join(System.lineSeparator(), list));
  }

  private String formatString(final String string) {
    return translateAlternateColorCodes(Objects.requireNonNull(string))
      .replace("%prefix%", PREFIX == null ? "" : PREFIX)
      .replace("%header%", HEADER == null ? "" : HEADER)
      .replace("%footer%", FOOTER == null ? "" : FOOTER);
  }

  private static String translateAlternateColorCodes(final String textToTranslate) {
    final char[] b = textToTranslate.toCharArray();

    for (int i = 0; i < b.length - 1; i++) {
      if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
        b[i] = '§';
        b[i + 1] = Character.toLowerCase(b[i + 1]);
      }
    }

    return new String(b);
  }
}
