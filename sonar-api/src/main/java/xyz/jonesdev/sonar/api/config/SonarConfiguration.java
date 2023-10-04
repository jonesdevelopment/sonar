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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.dependencies.Dependency;

import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Getter
public final class SonarConfiguration {
  private final @NotNull File pluginFolder;
  private SimpleYamlConfig generalConfig, messagesConfig;

  public SonarConfiguration(final @NotNull File pluginFolder) {
    this.pluginFolder = pluginFolder;
  }

  private String language;
  private String prefix;
  private String supportUrl;
  private String noPermission;

  private String actionBarLayout;
  private List<String> animation;

  private boolean enableVerification;
  private boolean logPlayerAddresses;
  private boolean checkGravity;
  private boolean checkCollisions;
  private boolean logConnections;
  private boolean logDuringAttack;
  private Pattern validNameRegex;
  private Pattern validBrandRegex;
  private Pattern validLocaleRegex;
  private String verificationConnectLog;
  private String verificationFailedLog;
  private String verificationSuccessfulLog;
  private String verificationBlacklistLog;
  private short gamemodeId;
  private int maximumBrandLength;
  private int maximumMovementTicks;
  private int minimumPlayersForAttack;
  private int maximumVerifyingPlayers;
  private int maximumOnlinePerIp;
  private int maximumQueuePolls;
  private int maximumLoginPackets;
  private int verificationTimeout;
  private int verificationReadTimeout;
  private int verificationDelay;

  private String header, footer;
  private Component tooManyPlayers;
  private Component tooFastReconnect;
  private Component tooManyOnlinePerIp;
  private Component invalidUsername;
  private Component verificationSuccess;
  private Component verificationFailed;
  private Component alreadyVerifying;
  private Component alreadyQueued;
  private Component blacklisted;

  private String incorrectCommandUsage;
  private String incorrectIpAddress;
  private String subCommandNoPerm;
  private String illegalIpAddress;
  private String playersOnly;
  private String consoleOnly;
  private String commandCoolDown;
  private String commandCoolDownLeft;

  private String blacklistEmpty;
  private String blacklistAdd;
  private String blacklistAddWarning;
  private String blacklistDuplicate;
  private String blacklistNotFound;
  private String blacklistRemove;
  private String blacklistCleared;
  private String blacklistSize;

  private String verifiedRemove;
  private String verifiedNotFound;
  private String verifiedCleared;
  private String verifiedSize;
  private String verifiedEmpty;
  private String verifiedBlocked;

  private String verboseSubscribed;
  private String verboseUnsubscribed;
  private String verboseSubscribedOther;
  private String verboseUnsubscribedOther;

  private String reloading;
  private String reloaded;

  @Getter
  @RequiredArgsConstructor
  public enum DatabaseType {
    MYSQL(new Dependency[]{Dependency.MYSQL}, "com.mysql.cj.jdbc.NonRegisteringDriver"),
    MARIADB(new Dependency[]{Dependency.MYSQL, Dependency.MARIADB}, "org.mariadb.jdbc.Driver"),
    NONE(null, null);

    private final Dependency[] dependencies;
    private final String driverClassName;
  }

  private DatabaseType databaseType;
  private String sqlUrl;
  private int sqlPort;
  private String sqlDatabase;
  private String sqlUser;
  private String sqlPassword;

  @Setter
  private boolean lockdownEnabled;
  private boolean lockdownEnableNotify;
  private boolean lockdownLogAttempts;
  private Component lockdownDisconnect;
  private String lockdownActivated;
  private String lockdownDeactivated;
  private String lockdownNotification;
  private String lockdownConsoleLog;

  public void load() {
    if (generalConfig == null) {
      generalConfig = new SimpleYamlConfig(pluginFolder, "config");
    }
    try {
      generalConfig.load();
    } catch (Exception exception) {
      // https://github.com/jonesdevelopment/sonar/issues/33
      // Only save the configuration when necessary
      Sonar.get().getLogger().error("Error while loading configuration: {}", exception);
      return;
    }

    // General options
    generalConfig.getYaml().setComment("general.language",
      "Name of the language file Sonar should use for messages"
    );
    language = generalConfig.getString("general.language", "en");

    generalConfig.getYaml().setComment("general.max-online-per-ip",
      "Maximum number of players online with the same IP address"
    );
    maximumOnlinePerIp = clamp(generalConfig.getInt("general.max-online-per-ip", 3), 1, Byte.MAX_VALUE);

    generalConfig.getYaml().setComment("general.min-players-for-attack",
      "Minimum number of new players in order for an attack to be detected"
    );
    minimumPlayersForAttack = clamp(generalConfig.getInt("general.min-players-for-attack", 5), 2, 1024);

    generalConfig.getYaml().setComment("general.log-player-addresses",
      "Should Sonar log players' IP addresses in the console?"
    );
    logPlayerAddresses = generalConfig.getBoolean("general.log-player-addresses", true);

    // Database
    generalConfig.getYaml().setComment("general.database.type",
      "Type of database Sonar uses to store verified players" + Sonar.LINE_SEPARATOR + "Possible types: NONE, MYSQL, " +
        "MARIADB (experimental)"
    );
    databaseType =
      DatabaseType.valueOf(generalConfig.getString("general.database.type", DatabaseType.NONE.name()).toUpperCase());

    // Message settings
    // Only create a new messages configuration object if the preferred language changed
    // https://github.com/jonesdevelopment/sonar/issues/26
    if (messagesConfig == null || !messagesConfig.getFile().getName().equals(language + ".yml")) {
      messagesConfig = new SimpleYamlConfig(pluginFolder, "lang/" + language);
    }
    try {
      messagesConfig.load();
    } catch (Exception exception) {
      // https://github.com/jonesdevelopment/sonar/issues/33
      // Only save the configuration when necessary
      Sonar.get().getLogger().error("Error while loading configuration: {}", exception);
      return;
    }

    messagesConfig.getYaml().setComment("messages.prefix",
      "Placeholder for every '%prefix%' in this configuration file");
    prefix = formatString(messagesConfig.getString("messages.prefix", "&e&lSonar &7» &f"));

    messagesConfig.getYaml().setComment("messages.support-url",
      "Placeholder for every '%support-url%' in this configuration file");
    supportUrl = messagesConfig.getString("messages.support-url", "https://jonesdev.xyz/discord/");

    // SQL
    generalConfig.getYaml().setComment("general.database.sql.url",
      "URL for authenticating with the SQL database");
    sqlUrl = generalConfig.getString("general.database.sql.url", "localhost");

    generalConfig.getYaml().setComment("general.database.sql.port",
      "Port for authenticating with the SQL database");
    sqlPort = generalConfig.getInt("general.database.sql.port", 3306);

    generalConfig.getYaml().setComment("general.database.sql.database",
      "Name of the SQL database");
    sqlDatabase = generalConfig.getString("general.database.sql.database", "sonar");

    generalConfig.getYaml().setComment("general.database.sql.username",
      "Username for authenticating with the SQL database");
    sqlUser = generalConfig.getString("general.database.sql.username", "");

    generalConfig.getYaml().setComment("general.database.sql.password",
      "Password for authenticating with the SQL database");
    sqlPassword = generalConfig.getString("general.database.sql.password", "");

    // Lockdown
    generalConfig.getYaml().setComment("general.lockdown.enabled",
      "Should Sonar prevent all players from joining the server?");
    lockdownEnabled = generalConfig.getBoolean("general.lockdown.enabled", false);

    generalConfig.getYaml().setComment("general.lockdown.log-attempts",
      "Should Sonar log new login attempts during lockdown?");
    lockdownLogAttempts = generalConfig.getBoolean("general.lockdown.log-attempts", true);

    generalConfig.getYaml().setComment("general.lockdown.notify-admins",
      "Should Sonar notify admins when they join the server during lockdown?");
    lockdownEnableNotify = generalConfig.getBoolean("general.lockdown.notify-admins", true);

    // Queue
    generalConfig.getYaml().setComment("general.queue.max-polls",
      "Maximum number of queue polls per 500 milliseconds");
    maximumQueuePolls = clamp(generalConfig.getInt("general.queue.max-polls", 30), 1, 1000);

    // Verification
    generalConfig.getYaml().setComment("general.verification.enabled",
      "Should Sonar verify new/unknown players? (Recommended)");
    enableVerification = generalConfig.getBoolean("general.verification.enabled", true);

    generalConfig.getYaml().setComment("general.verification.check-gravity",
      "Should Sonar check for valid client gravity? (Recommended)");
    checkGravity = generalConfig.getBoolean("general.verification.check-gravity", true);

    generalConfig.getYaml().setComment("general.verification.check-collisions",
      "Should Sonar check for valid client collisions? (Recommended)");
    checkCollisions = generalConfig.getBoolean("general.verification.check-collisions", true);

    generalConfig.getYaml().setComment("general.verification.gamemode",
      "The gamemode of the player during verification (0, 1, 2, or 3)");
    gamemodeId = (short) clamp(generalConfig.getInt("general.verification.gamemode", 3), 0, 3);

    generalConfig.getYaml().setComment("general.verification.log-connections",
      "Should Sonar log new verification attempts?");
    logConnections = generalConfig.getBoolean("general.verification.log-connections", true);

    generalConfig.getYaml().setComment("general.verification.log-during-attack",
      "Should Sonar log new verification attempts during attacks?");
    logDuringAttack = generalConfig.getBoolean("general.verification.log-during-attack", false);

    generalConfig.getYaml().setComment("general.verification.valid-name-regex",
      "Regex for validating usernames during verification");
    validNameRegex = Pattern.compile(generalConfig.getString(
      "general.verification.valid-name-regex", "^[a-zA-Z0-9_.*!]+$"));

    generalConfig.getYaml().setComment("general.verification.valid-brand-regex",
      "Regex for validating client brands during verification");
    validBrandRegex = Pattern.compile(generalConfig.getString(
      "general.verification.valid-brand-regex", "^[!-~ ]+$"));

    generalConfig.getYaml().setComment("general.verification.valid-locale-regex",
      "Regex for validating client locale during verification");
    validLocaleRegex = Pattern.compile(generalConfig.getString(
      "general.verification.valid-locale-regex", "^[a-zA-Z_]+$"));

    generalConfig.getYaml().setComment("general.verification.max-brand-length",
      "Maximum client brand length during verification");
    maximumBrandLength = generalConfig.getInt("general.verification.max-brand-length", 64);

    generalConfig.getYaml().setComment("general.verification.timeout",
      "Amount of time that has to pass before a player is disconnected");
    verificationTimeout = clamp(generalConfig.getInt("general.verification.timeout", 10000), 1500, 30000);

    generalConfig.getYaml().setComment("general.verification.read-timeout",
      "Amount of time that has to pass before a player times out");
    verificationReadTimeout = clamp(generalConfig.getInt("general.verification.read-timeout", 5000), 500, 30000);

    generalConfig.getYaml().setComment("general.verification.max-login-packets",
      "Maximum number of login packets the player has to send in order to be kicked");
    maximumLoginPackets = clamp(generalConfig.getInt("general.verification.max-login-packets", 256), 128, 8192);

    generalConfig.getYaml().setComment("general.verification.max-movement-ticks",
      "Maximum number of movement packets the player has to send in order to be verified");
    maximumMovementTicks = clamp(generalConfig.getInt("general.verification.max-movement-ticks", 8), 2, 100);

    generalConfig.getYaml().setComment("general.verification.max-players",
      "Maximum number of players verifying at the same time");
    maximumVerifyingPlayers = clamp(generalConfig.getInt("general.verification.max-players", 1024), 1,
      Short.MAX_VALUE);

    generalConfig.getYaml().setComment("general.verification.rejoin-delay",
      "Minimum number of rejoin delay during verification");
    verificationDelay = clamp(generalConfig.getInt("general.verification.rejoin-delay", 8000), 0, 100000);

    // load this here otherwise it could cause issues
    messagesConfig.getYaml().setComment("messages.header",
      "Placeholder for every '%header%' in this configuration file");
    header = fromList(messagesConfig.getStringList("messages.header",
      Arrays.asList(
        "&e&lSonar",
        "&r"
      )));

    messagesConfig.getYaml().setComment("messages.footer",
      "Placeholder for every '%footer%' in this configuration file");
    footer = fromList(messagesConfig.getStringList("messages.footer",
      Arrays.asList("&7If you believe that this is an error, contact an administrator.")));

    messagesConfig.getYaml().setComment("messages.no-permission",
      "Message that is shown when a player tries running /sonar without permission");
    noPermission = formatString(messagesConfig.getString("messages.no-permission",
      "%prefix%&cYou do not have permission to execute this command."));

    messagesConfig.getYaml().setComment("messages.lockdown.enabled",
      "Message that is shown when a player enables server lockdown");
    lockdownActivated = formatString(messagesConfig.getString("messages.lockdown.enabled",
      "%prefix%The server is now in lockdown mode."));

    messagesConfig.getYaml().setComment("messages.lockdown.disabled",
      "Message that is shown when a player disables server lockdown");
    lockdownDeactivated = formatString(messagesConfig.getString("messages.lockdown.disabled",
      "%prefix%The server is no longer in lockdown mode."));

    messagesConfig.getYaml().setComment("messages.lockdown.notification",
      "Message that is shown when an admin joins the server during lockdown");
    lockdownNotification = formatString(messagesConfig.getString("messages.lockdown.notification",
      "%prefix%&aHey, the server is currently in lockdown mode. If you want to disable the lockdown mode, " +
        "type " +
        "&f/sonar" +
        " lockdown&a."
    ));

    messagesConfig.getYaml().setComment("messages.lockdown.console-log",
      "Message that is shown to console when a normal player tries joining the server during lockdown");
    lockdownConsoleLog = messagesConfig.getString("messages.lockdown.console-log",
      "%player% (%ip%, %protocol%) tried to join during lockdown mode.");

    messagesConfig.getYaml().setComment("messages.lockdown.disconnect-message",
      "Message that is shown to a normal player when they try joining the server during lockdown");
    lockdownDisconnect = deserialize(fromList(messagesConfig.getStringList("messages.lockdown.disconnect-message",
      Arrays.asList(
        "%header%",
        "&cThe server is currently locked down, please try again later.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.reload.start",
      "Message that is shown when someone starts reloading Sonar");
    reloading = formatString(messagesConfig.getString("messages.reload.start",
      "%prefix%Reloading Sonar..."));

    messagesConfig.getYaml().setComment("messages.reload.finish",
      "Message that is shown when Sonar has finished reloading");
    reloaded = formatString(messagesConfig.getString("messages.reload.finish",
      "%prefix%&aSuccessfully reloaded &7(%taken%ms)"));

    messagesConfig.getYaml().setComment("messages.verbose.subscribed",
      "Message that is shown when a player subscribes to Sonar verbose");
    verboseSubscribed = formatString(messagesConfig.getString("messages.verbose.subscribed",
      "%prefix%You are now viewing Sonar verbose."));

    messagesConfig.getYaml().setComment("messages.verbose.unsubscribed",
      "Message that is shown when a player unsubscribes from Sonar verbose");
    verboseUnsubscribed = formatString(messagesConfig.getString("messages.verbose.unsubscribed",
      "%prefix%You are no longer viewing Sonar verbose."));

    messagesConfig.getYaml().setComment("messages.verbose.subscribed-other",
      "Message that is shown when a player makes another player subscribe to Sonar verbose");
    verboseSubscribedOther = formatString(messagesConfig.getString("messages.verbose.subscribed-other",
      "%prefix%%player% is now viewing Sonar verbose."));

    messagesConfig.getYaml().setComment("messages.verbose.unsubscribed-other",
      "Message that is shown when a player makes another player unsubscribe from Sonar verbose");
    verboseUnsubscribedOther = formatString(messagesConfig.getString("messages.verbose.unsubscribed-other",
      "%prefix%%player% is no longer viewing Sonar verbose."));

    messagesConfig.getYaml().setComment("messages.incorrect-command-usage",
      "Message that is shown when someone uses a command incorrectly");
    incorrectCommandUsage = formatString(messagesConfig.getString("messages.incorrect-command-usage",
      "%prefix%&cUsage: /sonar %usage%"));

    messagesConfig.getYaml().setComment("messages.invalid-ip-address",
      "Message that is shown when someone provides an invalid IP address (Invalid characters)");
    incorrectIpAddress = formatString(messagesConfig.getString("messages.invalid-ip-address",
      "%prefix%The IP address you provided seems to be invalid."));

    messagesConfig.getYaml().setComment("messages.illegal-ip-address",
      "Message that is shown when someone provides an illegal IP address (Local IP)");
    illegalIpAddress = formatString(messagesConfig.getString("messages.illegal-ip-address",
      "%prefix%The IP address you provided seems to be either a local or loopback IP."));

    messagesConfig.getYaml().setComment("messages.player-only",
      "Message that is shown when the console runs a command that is player-only");
    playersOnly = formatString(messagesConfig.getString("messages.player-only",
      "%prefix%&cYou can only execute this command as a player."));

    messagesConfig.getYaml().setComment("messages.console-only",
      "Message that is shown when a player runs a command that is console-only");
    consoleOnly = formatString(messagesConfig.getString("messages.console-only",
      "%prefix%&cFor security reasons, you can only execute this command through console."));

    messagesConfig.getYaml().setComment("messages.command-cool-down",
      "Message that is shown when a player executes Sonar commands too quickly");
    commandCoolDown = formatString(messagesConfig.getString("messages.command-cool-down",
      "%prefix%&cYou can only execute this command every 0.5 seconds."));
    commandCoolDownLeft = formatString(messagesConfig.getString("messages.command-cool-down-left",
      "%prefix%&cPlease wait another &l%time-left%s&r&c."));

    messagesConfig.getYaml().setComment("messages.sub-command-no-permission",
      "Message that is shown when a player does not have permission to execute a certain subcommand");
    subCommandNoPerm = formatString(messagesConfig.getString("messages.sub-command-no-permission",
      "%prefix%&cYou do not have permission to execute this subcommand. &7(%permission%)"));

    messagesConfig.getYaml().setComment("messages.blacklist.empty",
      "Message that is shown when someone tries clearing the blacklist but is is empty");
    blacklistEmpty = formatString(messagesConfig.getString("messages.blacklist.empty",
      "%prefix%The blacklist is currently empty. Therefore, no IP addresses were removed from the blacklist."));

    messagesConfig.getYaml().setComment("messages.blacklist.cleared",
      "Message that is shown when someone clears the blacklist");
    blacklistCleared = formatString(messagesConfig.getString("messages.blacklist.cleared",
      "%prefix%You successfully removed a total of %removed% IP address(es) from the blacklist."));

    messagesConfig.getYaml().setComment("messages.blacklist.size",
      "Message that is shown when someone checks the size of the blacklist");
    blacklistSize = formatString(messagesConfig.getString("messages.blacklist.size",
      "%prefix%The blacklist currently contains %amount% IP address(es)."));

    messagesConfig.getYaml().setComment("messages.blacklist.added",
      "Message that is shown when someone adds an IP address to the blacklist");
    blacklistAdd = formatString(messagesConfig.getString("messages.blacklist.added",
      "%prefix%Successfully added %ip% to the blacklist."));

    messagesConfig.getYaml().setComment("messages.blacklist.added-warning",
      "Message that is shown when someone adds an IP address to the blacklist that is verified");
    blacklistAddWarning = formatString(messagesConfig.getString("messages.blacklist.added-warning",
      "%prefix%&cWarning: &f%ip% is currently whitelisted. " +
        "Consider removing the IP address from the list of verified players to avoid potential issues."));

    messagesConfig.getYaml().setComment("messages.blacklist.removed",
      "Message that is shown when someone removes an IP address from the blacklist");
    blacklistRemove = formatString(messagesConfig.getString("messages.blacklist.removed",
      "%prefix%Successfully removed %ip% from the blacklist."));

    messagesConfig.getYaml().setComment("messages.blacklist.duplicate-ip",
      "Message that is shown when someone adds an IP address to the blacklist but it is already blacklisted");
    blacklistDuplicate = formatString(messagesConfig.getString("messages.blacklist.duplicate-ip",
      "%prefix%The IP address you provided is already blacklisted."));

    messagesConfig.getYaml().setComment("messages.blacklist.ip-not-found",
      "Message that is shown when someone removes an IP address from the blacklist but it is not blacklisted");
    blacklistNotFound = formatString(messagesConfig.getString("messages.blacklist.ip-not-found",
      "%prefix%The IP address you provided is not blacklisted."));

    messagesConfig.getYaml().setComment("messages.verified.empty",
      "Message that is shown when someone tries clearing the list of verified players but is is empty");
    verifiedEmpty = formatString(messagesConfig.getString("messages.verified.empty",
      "%prefix%The list of verified players is currently empty. Therefore, no players were unverified."));

    messagesConfig.getYaml().setComment("messages.verified.cleared",
      "Message that is shown when someone clears the list of verified players");
    verifiedCleared = formatString(messagesConfig.getString("messages.verified.cleared",
      "%prefix%You successfully unverified a total of %removed% unique player(s)."));

    messagesConfig.getYaml().setComment("messages.verified.size",
      "Message that is shown when someone checks the size of the list of verified players");
    verifiedSize = formatString(messagesConfig.getString("messages.verified.size",
      "%prefix%There are currently %amount% unique player(s) verified."));

    messagesConfig.getYaml().setComment("messages.verified.removed",
      "Message that is shown when someone un-verifies an IP address");
    verifiedRemove = formatString(messagesConfig.getString("messages.verified.removed",
      "%prefix%Successfully unverified %ip%."));

    messagesConfig.getYaml().setComment("messages.verified.ip-not-found",
      "Message that is shown when someone un-verifies an IP address but it is not verified");
    verifiedNotFound = formatString(messagesConfig.getString("messages.verified.ip-not-found",
      "%prefix%The IP address you provided is not verified."));

    messagesConfig.getYaml().setComment("messages.verified.blocked",
      "Message that is shown when someone tries un-verifying the same IP address twice (double operation)");
    verifiedBlocked = formatString(messagesConfig.getString("messages.verified.blocked",
      "%prefix%Please wait for the current operation to finish."));

    messagesConfig.getYaml().setComment("messages.verification.too-many-players",
      "Disconnect message that is shown when too many players are verifying at the same time");
    tooManyPlayers = deserialize(fromList(messagesConfig.getStringList("messages.verification.too-many-players",
      Arrays.asList(
        "%header%",
        "&6Too many players are currently trying to log in, try again later.",
        "&7Please wait a few seconds before trying to join again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.verification.logs.connection",
      "Message that is logged to console whenever a new player joins the server");
    verificationConnectLog = formatString(messagesConfig.getString("messages.verification.logs.connection",
      "%name%%ip% (%protocol%) has connected."));

    messagesConfig.getYaml().setComment("messages.verification.logs.failed",
      "Message that is logged to console whenever a player fails verification");
    verificationFailedLog = formatString(messagesConfig.getString("messages.verification.logs.failed",
      "%ip% (%protocol%) has failed the bot check for: %reason%"));

    messagesConfig.getYaml().setComment("messages.verification.logs.blacklisted",
      "Message that is logged to console whenever a player is blacklisted");
    verificationBlacklistLog = formatString(messagesConfig.getString("messages.verification.logs.blacklisted",
      "%ip% (%protocol%) was blacklisted for too many failed attempts"));

    messagesConfig.getYaml().setComment("messages.verification.logs.successful",
      "Message that is logged to console whenever a player is verified");
    verificationSuccessfulLog = formatString(messagesConfig.getString("messages.verification.logs.successful",
      "%name% has been verified successfully (%time%s!)."));

    messagesConfig.getYaml().setComment("messages.verification.too-fast-reconnect",
      "Disconnect message that is shown when someone rejoins too fast during verification");
    tooFastReconnect = deserialize(fromList(messagesConfig.getStringList("messages.verification.too-fast-reconnect",
      Arrays.asList(
        "%header%",
        "&6You reconnected too fast, try again later.",
        "&7Please wait a few seconds before trying to verify again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.verification.already-verifying",
      "Disconnect message that is shown when someone joins but is already verifying");
    alreadyVerifying = deserialize(fromList(messagesConfig.getStringList("messages.verification.already-verifying",
      Arrays.asList(
        "%header%",
        "&cYour IP address is currently being verified.",
        "&cPlease wait a few seconds before trying to verify again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.verification.already-queued",
      "Disconnect message that is shown when someone joins but is already queued for verification");
    alreadyQueued = deserialize(fromList(messagesConfig.getStringList("messages.verification.already-queued",
      Arrays.asList(
        "%header%",
        "&cYour IP address is currently queued for verification.",
        "&cPlease wait a few minutes before trying to verify again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.verification.blacklisted",
      "Disconnect message that is shown when someone joins but is temporarily blacklisted");
    blacklisted = deserialize(fromList(messagesConfig.getStringList("messages.verification.blacklisted",
      Arrays.asList(
        "%header%",
        "&cYou are currently denied from entering the server.",
        "&cPlease wait a few minutes to be able to join the server again.",
        "&6False positive? &7%support-url%",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.verification.invalid-username",
      "Disconnect message that is shown when someone joins with an invalid username");
    invalidUsername = deserialize(fromList(messagesConfig.getStringList("messages.verification.invalid-username",
      Arrays.asList(
        "%header%",
        "&cYour username contains invalid characters.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.verification.success",
      "Disconnect message that is shown when someone verifies successfully");
    verificationSuccess = deserialize(fromList(messagesConfig.getStringList("messages.verification.success",
      Arrays.asList(
        "%header%",
        "&aYou have successfully passed the verification.",
        "&fYou are now able to play on the server when you reconnect."
      ))));

    messagesConfig.getYaml().setComment("messages.verification.failed",
      "Disconnect message that is shown when someone fails verification");
    verificationFailed = deserialize(fromList(messagesConfig.getStringList("messages.verification.failed",
      Arrays.asList(
        "%header%",
        "&cYou have failed the verification.",
        "&7Please wait a few seconds before trying to verify again.",
        "&6Need help? &7%support-url%",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.verification.too-many-online-per-ip",
      "Disconnect message that is shown when someone joins but there are too many online players with their IP address");
    tooManyOnlinePerIp = deserialize(fromList(messagesConfig.getStringList("messages.too-many-online-per-ip",
      Arrays.asList(
        "%header%",
        "&cThere are too many players online with your IP address.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("messages.action-bar.layout",
      "General layout for the verbose action-bar" +
        Sonar.LINE_SEPARATOR + "Placeholders and their descriptions:" +
        Sonar.LINE_SEPARATOR + "- %queued% Number of queued connections" +
        Sonar.LINE_SEPARATOR + "- %verifying% Number of verifying connections" +
        Sonar.LINE_SEPARATOR + "- %blacklisted% Number of blacklisted IP addresses" +
        Sonar.LINE_SEPARATOR + "- %total-joins% Number of total joins (not unique!)" +
        Sonar.LINE_SEPARATOR + "- %per-second-joins% Number of joins per second" +
        Sonar.LINE_SEPARATOR + "- %verify-total% Number of total verification attempts" +
        Sonar.LINE_SEPARATOR + "- %verify-success% Number of verified IP addresses" +
        Sonar.LINE_SEPARATOR + "- %verify-failed% Number of failed verifications" +
        Sonar.LINE_SEPARATOR + "- %incoming-traffic% Incoming bandwidth usage per second" +
        Sonar.LINE_SEPARATOR + "- %outgoing-traffic% Outgoing bandwidth usage per second" +
        Sonar.LINE_SEPARATOR + "- %incoming-traffic-ttl% Total incoming bandwidth usage" +
        Sonar.LINE_SEPARATOR + "- %outgoing-traffic-ttl% Total outgoing bandwidth usage" +
        Sonar.LINE_SEPARATOR + "- %used-memory% Amount of used memory (JVM process)" +
        Sonar.LINE_SEPARATOR + "- %total-memory% Amount of total memory (JVM process)" +
        Sonar.LINE_SEPARATOR + "- %max-memory% Amount of max memory (JVM process)" +
        Sonar.LINE_SEPARATOR + "- %free-memory% Amount of free memory (JVM process)" +
        Sonar.LINE_SEPARATOR + "- %animation% Animated spinning circle (by default)"
    );
    actionBarLayout = formatString(messagesConfig.getString(
      "messages.action-bar.layout",
      String.join(" &3╺ ", Arrays.asList(
        "%prefix%&7Queued &f%queued%",
        "&7Verifying &f%verifying%",
        "&7Blacklisted &f%blacklisted%" +
          " &3| &a⬆ &f%outgoing-traffic%/s &c⬇ &f%incoming-traffic%/s" +
          "  &a&l%animation%"
      ))));
    animation = Collections.unmodifiableList(messagesConfig.getStringList("messages.action-bar.animation",
      Arrays.asList("◜", "◝", "◞", "◟") // ▙ ▛ ▜ ▟
    ));

    generalConfig.save();
    messagesConfig.save();
  }

  private static int clamp(final int v, final int max, final int min) {
    return Math.max(Math.min(v, min), max);
  }

  public String formatAddress(final InetAddress inetAddress) {
    if (logPlayerAddresses) {
      return inetAddress.toString();
    }
    return "/<ip address withheld>";
  }

  private @NotNull String fromList(final @NotNull Collection<String> list) {
    return formatString(String.join(Sonar.LINE_SEPARATOR, list));
  }

  private static Component deserialize(final String legacy) {
    return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
  }

  private @NotNull String formatString(final @NotNull String str) {
    return translateAlternateColorCodes(str)
      .replace("%prefix%", prefix == null ? "" : prefix)
      .replace("%support-url%", supportUrl == null ? "" : supportUrl)
      .replace("%header%", header == null ? "" : header)
      .replace("%footer%", footer == null ? "" : footer);
  }

  private static @NotNull String translateAlternateColorCodes(final @NotNull String str) {
    final char[] b = str.toCharArray();

    for (int i = 0; i < b.length - 1; i++) {
      if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
        b[i] = '§';
        b[i + 1] = Character.toLowerCase(b[i + 1]);
      }
    }

    return new String(b);
  }
}
