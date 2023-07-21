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

package jones.sonar.api.config;

import jones.sonar.api.database.DatabaseType;
import jones.sonar.api.yaml.YamlConfig;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

public final class SonarConfiguration {
  @Getter
  private final YamlConfig yamlConfig;

  public SonarConfiguration(final @NotNull File folder) {
    yamlConfig = new YamlConfig(folder, "config");
  }

  public String PREFIX;
  public String SUPPORT_URL;

  public String ACTION_BAR_LAYOUT;
  public Collection<String> ANIMATION;

  public boolean LOG_CONNECTIONS;
  public Pattern VALID_NAME_REGEX;
  public Pattern VALID_BRAND_REGEX;
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
  public String INVALID_USERNAME;
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
  public boolean LOCKDOWN_LOG_ATTEMPTS;
  public String LOCKDOWN_DISCONNECT;
  public String LOCKDOWN_ACTIVATED;
  public String LOCKDOWN_DEACTIVATED;
  public String LOCKDOWN_NOTIFICATION;
  public String LOCKDOWN_CONSOLE_LOG;

  public DatabaseType DATABASE;
  public boolean ALLOW_PURGING;
  public String DATABASE_FILE_NAME;
  public String DATABASE_URL;
  public String DATABASE_NAME;
  public String DATABASE_USERNAME;
  public String DATABASE_PASSWORD;
  public int DATABASE_PORT;
  public int DATABASE_QUERY_LIMIT;

  public String DATABASE_PURGE_DISALLOWED;
  public String DATABASE_PURGE_CONFIRM;
  public String DATABASE_PURGE;
  public String DATABASE_PURGE_ALREADY;
  public String DATABASE_NOT_SELECTED;
  public String DATABASE_RELOADING;
  public String DATABASE_RELOADED;

  public void load() {
    Objects.requireNonNull(yamlConfig);

    yamlConfig.load();

    // Message settings
    PREFIX = formatString(yamlConfig.getString("messages.prefix", "&e&lSonar &7» &f"));
    SUPPORT_URL = yamlConfig.getString("messages.support-url", "https://jonesdev.xyz/discord/");

    // General options
    MAXIMUM_ONLINE_PER_IP = clamp(yamlConfig.getInt("general.max-online-per-ip", 3), 1, Byte.MAX_VALUE);
    MINIMUM_PLAYERS_FOR_ATTACK = clamp(yamlConfig.getInt("general.min-players-for-attack", 5), 2, 1024);

    LOCKDOWN_ENABLED = yamlConfig.getBoolean("general.lockdown.enabled", false);
    LOCKDOWN_LOG_ATTEMPTS = yamlConfig.getBoolean("general.lockdown.log-attempts", true);
    LOCKDOWN_ENABLE_NOTIFY = yamlConfig.getBoolean("general.lockdown.notify-admins", true);

    DATABASE = DatabaseType.getFromString(yamlConfig.getString("general.database.type", "NONE"))
      .orElse(DatabaseType.NONE);
    ALLOW_PURGING = yamlConfig.getBoolean("general.database.allow-purging", true);

    // YAML
    DATABASE_FILE_NAME = yamlConfig.getString("general.database.yaml.file-name", "database");

    // MySQL
    DATABASE_NAME = yamlConfig.getString("general.database.mysql.name", "sonar");
    DATABASE_URL = yamlConfig.getString("general.database.mysql.url", "localhost");
    DATABASE_PORT = clamp(yamlConfig.getInt("general.database.mysql.port", 3306), 0, 65535);
    DATABASE_USERNAME = yamlConfig.getString("general.database.mysql.username", "root");
    DATABASE_PASSWORD = yamlConfig.getString("general.database.mysql.password", "");
    DATABASE_QUERY_LIMIT = clamp(yamlConfig.getInt("general.database.mysql.query-limit", 100000), 1000,
      Integer.MAX_VALUE);

    MAXIMUM_QUEUED_PLAYERS = clamp(yamlConfig.getInt("general.queue.max-players", 8192), 128, Short.MAX_VALUE);
    MAXIMUM_QUEUE_POLLS = clamp(yamlConfig.getInt("general.queue.max-polls", 10), 1, 1000);

    ENABLE_VERIFICATION = yamlConfig.getBoolean("general.verification.enabled", true);
    LOG_CONNECTIONS = yamlConfig.getBoolean("general.verification.log-connections", true);
    LOG_DURING_ATTACK = yamlConfig.getBoolean("general.verification.log-during-attack", false);
    VALID_NAME_REGEX = Pattern.compile(yamlConfig.getString(
      "general.verification.valid-name-regex", "^[a-zA-Z0-9_.*!]+$"
    ));
    VALID_BRAND_REGEX = Pattern.compile(yamlConfig.getString(
      "general.verification.valid-brand-regex", "^[a-zA-Z0-9-/.,:_()\\[\\]{}!?' *]+$"
    ));
    VERIFICATION_TIMEOUT = clamp(yamlConfig.getInt("general.verification.timeout", 4000), 500, 30000);
    MAXIMUM_LOGIN_PACKETS = clamp(yamlConfig.getInt("general.verification.max-login-packets", 256), 128, 8192);
    MAXIMUM_VERIFYING_PLAYERS = clamp(yamlConfig.getInt("general.verification.max-players", 1024), 1,
      Short.MAX_VALUE);
    VERIFICATION_DELAY = clamp(yamlConfig.getInt("general.verification.rejoin-delay", 8000), 0, 100000);
    ENABLE_COMPRESSION = yamlConfig.getBoolean("general.verification.enable-compression", true);

    // load this here otherwise it could cause issues
    HEADER = fromList(yamlConfig.getStringList("messages.header",
      Arrays.asList(
        "&e&lSonar"
      )));
    FOOTER = fromList(yamlConfig.getStringList("messages.footer",
      Arrays.asList(
        "&7If you believe that this is an error, contact an administrator."
      )));

    LOCKDOWN_ACTIVATED = formatString(yamlConfig.getString("messages.lockdown.enabled",
      "%prefix%The server is now in lockdown mode."
    ));
    LOCKDOWN_DEACTIVATED = formatString(yamlConfig.getString("messages.lockdown.disabled",
      "%prefix%The server is no longer in lockdown mode."
    ));
    LOCKDOWN_NOTIFICATION = formatString(yamlConfig.getString("messages.lockdown.notification",
      "%prefix%&aHey, the server is currently in lockdown mode. If you want to disable the lockdown mode, " +
        "type " +
        "&f/sonar" +
        " lockdown&a."
    ));
    LOCKDOWN_CONSOLE_LOG = yamlConfig.getString("messages.lockdown.console-log",
      "%player% (%ip%, %protocol%) tried to join during lockdown mode."
    );
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

    DATABASE_PURGE_DISALLOWED = formatString(yamlConfig.getString("messages.database.disallowed",
      "%prefix%&cPurging the database is currently disallowed. Therefore, your action has been cancelled."
    ));
    DATABASE_PURGE_CONFIRM = formatString(yamlConfig.getString("messages.database.purge-confirm",
      "%prefix%&cPlease confirm that you want to delete all database entries by typing &7/sonar database " +
        "purge " +
        "confirm&c."
    ));
    DATABASE_PURGE = formatString(yamlConfig.getString("messages.database.purge",
      "%prefix%&aSuccessfully purged all database entries."
    ));
    DATABASE_PURGE_ALREADY = formatString(yamlConfig.getString("messages.database.purging",
      "%prefix%&cThere is already a purge currently running."
    ));
    DATABASE_NOT_SELECTED = formatString(yamlConfig.getString("messages.database.not-selected",
      "%prefix%&cYou have not selected any data storage type."
    ));
    DATABASE_RELOADING = formatString(yamlConfig.getString("messages.database.reload.start",
      "%prefix%Reloading all databases..."
    ));
    DATABASE_RELOADED = formatString(yamlConfig.getString("messages.database.reload.finish",
      "%prefix%&aSuccessfully reloaded &7(%taken%ms)"
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
        "&cYour IP address is currently denied from entering the server.",
        "&6False positive? &7%support-url%",
        "%footer%"
      )));
    UNEXPECTED_ERROR = fromList(yamlConfig.getStringList("messages.verification.unexpected-error",
      Arrays.asList(
        "%header%",
        "&6An unexpected error occurred when trying to process your connection.",
        "&7Please wait a few seconds before trying to verify again.",
        "&6Need help? &7%support-url%",
        "%footer%"
      )));
    INVALID_USERNAME = fromList(yamlConfig.getStringList("messages.verification.invalid-username",
      Arrays.asList(
        "%header%",
        "&cYour username contains invalid characters.",
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

  private static int clamp(final int v, final int max, final int min) {
    return Math.max(Math.min(v, min), max);
  }

  private String fromList(final Collection<String> list) {
    return formatString(String.join(System.lineSeparator(), list));
  }

  private String formatString(final String string) {
    return translateAlternateColorCodes(Objects.requireNonNull(string))
      .replace("%prefix%", PREFIX == null ? "" : PREFIX)
      .replace("%support-url%", SUPPORT_URL == null ? "" : SUPPORT_URL)
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
