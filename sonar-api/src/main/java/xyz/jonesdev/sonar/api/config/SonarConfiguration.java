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

import com.j256.ormlite.db.DatabaseType;
import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.SonarCommand;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.api.ormlite.MariaDbDatabaseTypeAdapter;
import xyz.jonesdev.sonar.api.ormlite.MysqlDatabaseTypeAdapter;
import xyz.jonesdev.sonar.api.webhook.DiscordWebhook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

public final class SonarConfiguration {
  @Getter
  private final SimpleYamlConfig generalConfig, messagesConfig, webhookConfig;
  private final File languageFile, pluginFolder;

  static final LoggerWrapper LOGGER = new LoggerWrapper() {

    @Override
    public void info(final String message, final Object... args) {
      Sonar.get().getLogger().info("[config] " + message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      Sonar.get().getLogger().warn("[config] " + message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      Sonar.get().getLogger().error("[config] " + message, args);
    }
  };

  public SonarConfiguration(final @NotNull File pluginFolder) {
    this.pluginFolder = pluginFolder;
    this.messagesConfig = new SimpleYamlConfig(new File(pluginFolder, "messages.yml"));
    this.generalConfig = new SimpleYamlConfig(new File(pluginFolder, "config.yml"));
    this.webhookConfig = new SimpleYamlConfig(new File(pluginFolder, "webhook.yml"));
    this.languageFile = new File(pluginFolder, "language.properties");
  }

  private static final Language DEFAULT_FALLBACK_LANGUAGE = Language.EN;

  private Language getPreferredLanguage() {
    if (!languageFile.exists()) {
      final URL defaultLanguageFile = Sonar.class.getResource("/assets/language.properties");
      // Make sure the file actually exists before trying to copy it
      if (defaultLanguageFile == null) {
        LOGGER.error("Cannot check for custom language (is the file missing?)");
        return DEFAULT_FALLBACK_LANGUAGE;
      }
      // Copy the file to the plugin data directory
      try (final InputStream inputStream = defaultLanguageFile.openStream()) {
        Files.copy(inputStream, languageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException exception) {
        LOGGER.error("Error copying file: {}", exception);
        return DEFAULT_FALLBACK_LANGUAGE;
      }
    }

    // Try to read the property from the file
    try (final InputStream input = new FileInputStream(languageFile)) {
      final Properties properties = new Properties();
      properties.load(input);
      final String property = properties.getProperty("language");
      try {
        // Try parsing the property as a language
        return Language.fromCode(property);
      } catch (Throwable throwable) {
        LOGGER.error("Could not find requested language: {}", throwable);
      }
    } catch (Exception exception) {
      LOGGER.error("Error reading language file: {}", exception);
    }
    return DEFAULT_FALLBACK_LANGUAGE;
  }

  public void load() {
    // Make sure the plugin folder actually exists before trying to copy files into it
    if (!pluginFolder.exists() && !pluginFolder.mkdirs()) {
      throw new IllegalStateException("Could not create plugin folder (insufficient permissions?)");
    }

    // Generate the language file and check what it's set to
    Language preferredLanguage = getPreferredLanguage();
    if (preferredLanguage == Language.SYSTEM) {
      try {
        // Try using the system language to determine the language file
        final String property = System.getProperty("user.language", "en");
        preferredLanguage = Language.fromCode(property);
        // Make sure the user knows that we're using the system language for translations
        LOGGER.info("Using system language ({}) for translations.", preferredLanguage);
      } catch (Exception exception) {
        LOGGER.warn("Could not use system language for translations.");
        LOGGER.warn("Using default language ({}) for translations.", preferredLanguage);
      }
    } else {
      LOGGER.info("Using custom language ({}) for translations.", preferredLanguage);
    }

    // Load all configurations
    try {
      generalConfig.load(getAsset("config", preferredLanguage));
      messagesConfig.load(getAsset("messages", preferredLanguage));
      webhookConfig.load(getAsset("webhook", preferredLanguage));
    } catch (Exception exception) {
      throw new IllegalStateException("Error loading configuration", exception);
    }

    // Load all values
    loadValues();
  }

  public void loadValues() {
    // Since we are loading from a file, we don't need to provide default values

    // General settings
    logPlayerAddresses = generalConfig.getBoolean("general.log-player-addresses");
    maxOnlinePerIp = clamp(generalConfig.getInt("general.max-online-per-ip"), 1, 100);

    // Attack tracker
    minPlayersForAttack = clamp(generalConfig.getInt("attack-tracker.min-players-for-attack"), 2, 1024);
    minAttackDuration = clamp(generalConfig.getInt("attack-tracker.min-attack-duration"), 1000, 900000);
    minAttackThreshold = clamp(generalConfig.getInt("attack-tracker.min-attack-threshold"), 0, 20);
    attackCooldownDelay = clamp(generalConfig.getInt("attack-tracker.attack-cooldown-delay"), 100, 30000);

    // Database
    database.type = Database.Type.valueOf(generalConfig.getString("database.type").toUpperCase());
    database.host = generalConfig.getString("database.host");
    database.port = generalConfig.getInt("database.port");
    database.name = generalConfig.getString("database.name");
    database.username = generalConfig.getString("database.username");
    database.password = generalConfig.getString("database.password");
    database.maximumAge = clamp(generalConfig.getInt("database.maximum-age"), 1, 365);

    // Queue
    queue.maxQueuePolls = clamp(generalConfig.getInt("queue.max-polls"), 1, 1000);

    // Verification
    verification.timing = Verification.Timing.valueOf(generalConfig.getString("verification.timing"));
    // Warn the user if the verification timing is set to NEVER
    if (verification.timing == Verification.Timing.NEVER) {
      LOGGER.warn(" ");
      LOGGER.warn("You have set the verification timing to 'NEVER'.");
      LOGGER.warn("Sonar will NOT perform the bot verification at all, therefore making it useless.");
      LOGGER.warn("It is highly suggested to set this option to either 'DURING_ATTACK' or 'ALWAYS'.");
      LOGGER.warn("Please only edit this option if you really know what you are doing.");
      LOGGER.warn(" ");
    }

    verification.gravity.enabled = generalConfig.getBoolean("verification.checks.gravity.enabled");
    verification.gravity.checkCollisions = generalConfig.getBoolean("verification.checks.gravity.check-collisions");
    verification.gravity.captchaOnFail = generalConfig.getBoolean("verification.checks.gravity.captcha-on-fail");
    verification.gravity.maxMovementTicks = clamp(generalConfig.getInt("verification.checks.gravity.max-movement-ticks"), 2, 100);
    verification.gravity.gamemode = Verification.Gravity.Gamemode.valueOf(generalConfig.getString("verification.checks.gravity.gamemode"));

    verification.vehicle.timing = Verification.Timing.valueOf(generalConfig.getString("verification.checks.vehicle.timing"));

    verification.map.timing = Verification.Timing.valueOf(generalConfig.getString("verification.checks.map-captcha.timing"));
    verification.map.scratches = generalConfig.getBoolean("verification.checks.map-captcha.effects.scratches");
    verification.map.ripple = generalConfig.getBoolean("verification.checks.map-captcha.effects.ripple");
    verification.map.bump = generalConfig.getBoolean("verification.checks.map-captcha.effects.bump");

    verification.map.distortion.enabled = generalConfig.getBoolean("verification.checks.map-captcha.effects.distortion.enabled");
    verification.map.distortion.shape = generalConfig.getInt("verification.checks.map-captcha.effects.distortion.shape");
    verification.map.distortion.distance = generalConfig.getInt("verification.checks.map-captcha.effects.distortion.distance");
    verification.map.distortion.density = (float) generalConfig.getYaml().getDouble("verification.checks.map-captcha.effects.distortion.density");
    verification.map.distortion.mix = (float) generalConfig.getYaml().getDouble("verification.checks.map-captcha.effects.distortion.mix");

    final String backgroundImagePath = generalConfig.getString("verification.checks.map-captcha.background");
    if (!backgroundImagePath.isEmpty()) {
      verification.map.backgroundImage = new File(pluginFolder, backgroundImagePath);
      if (!verification.map.backgroundImage.exists()) {
        Sonar.get().getLogger().error("The background image does not exist! Please check the configuration.");
      }
    } else {
      verification.map.backgroundImage = null;
    }

    verification.map.autoColor = generalConfig.getBoolean("verification.checks.map-captcha.auto-color");
    verification.map.precomputeAmount = clamp(generalConfig.getInt("verification.checks.map-captcha.precompute"), 1, 1000);
    verification.map.maxDuration = clamp(generalConfig.getInt("verification.checks.map-captcha.max-duration"), 5000, 360000);
    verification.map.maxTries = generalConfig.getInt("verification.checks.map-captcha.max-tries");
    verification.map.dictionary = generalConfig.getString("verification.checks.map-captcha.dictionary");

    verification.brand.enabled = generalConfig.getBoolean("verification.checks.client-brand.enabled");
    verification.brand.validRegex = Pattern.compile(generalConfig.getString("verification.checks.client-brand.valid-regex"));
    verification.brand.maxLength = generalConfig.getInt("verification.checks.client-brand.max-length");

    verification.validNameRegex = Pattern.compile(generalConfig.getString("verification.checks.valid-name-regex"));
    verification.validLocaleRegex = Pattern.compile(generalConfig.getString("verification.checks.valid-locale-regex"));
    verification.maxLoginPackets = clamp(generalConfig.getInt("verification.checks.max-login-packets"), 128, 8192);

    verification.transfer.enabled = generalConfig.getBoolean("verification.transfer.enabled");
    verification.transfer.host = generalConfig.getString("verification.transfer.destination-host");
    verification.transfer.port = clamp(generalConfig.getInt("verification.transfer.destination-port"), 0, 0xffff);

    if (verification.transfer.enabled) {
      Sonar.get().getLogger().info("Transferring 1.20.5+ clients is enabled. Please make sure to follow the instructions in order for this to work properly.");
    }

    verification.checkGeyser = generalConfig.getBoolean("verification.check-geyser-players");
    verification.logConnections = generalConfig.getBoolean("verification.log-connections");
    verification.logDuringAttack = generalConfig.getBoolean("verification.log-during-attack");
    verification.debugXYZPositions = generalConfig.getBoolean("verification.debug-xyz-positions");
    verification.readTimeout = clamp(generalConfig.getInt("verification.read-timeout"), 500, 30000);
    verification.reconnectDelay = clamp(generalConfig.getInt("verification.rejoin-delay"), 0, 100000);
    verification.rememberTime = clamp(generalConfig.getInt("verification.remember-time"), 0, 86400000);
    verification.blacklistTime = clamp(generalConfig.getInt("verification.blacklist-time"), 0, 86400000);
    verification.blacklistThreshold = clamp(generalConfig.getInt("verification.blacklist-threshold"), 0, 100);
    verification.whitelistedProtocols.clear();
    verification.whitelistedProtocols.addAll(generalConfig.getIntList("verification.whitelisted-protocols"));
    verification.blacklistedProtocols.clear();
    verification.blacklistedProtocols.addAll(generalConfig.getIntList("verification.blacklisted-protocols"));

    // Webhooks
    webhook.url = webhookConfig.getString("webhook.url");
    webhook.username = webhookConfig.getString("webhook.username");
    webhook.avatarUrl = webhookConfig.getString("webhook.avatar-url");
    webhook.content = webhookConfig.getString("webhook.content");
    webhook.footer.text = webhookConfig.getString("webhook.embed.footer.text");
    webhook.footer.iconUrl = webhookConfig.getString("webhook.embed.footer.icon-url");

    final String realEmbedPath = "webhook.embed";
    final String embedPath = realEmbedPath + ".";

    webhook.embed.title = webhookConfig.getString(embedPath + "title");
    webhook.embed.titleUrl = webhookConfig.getString(embedPath + "title-url");
    webhook.embed.description = formatString(String.join("\n",
      webhookConfig.getStringList(embedPath + "description")));
    webhook.embed.r = webhookConfig.getInt(embedPath + "color.red");
    webhook.embed.g = webhookConfig.getInt(embedPath + "color.green");
    webhook.embed.b = webhookConfig.getInt(embedPath + "color.blue");

    if (!webhook.url.isEmpty()) {
      if (webhook.username.isEmpty()) {
        throw new IllegalStateException("Webhook username cannot be empty");
      }
      webhook.discordWebhook = new DiscordWebhook(webhook.url);
    } else if (webhook.discordWebhook != null) {
      // Reset if Discord webhooks were disabled
      webhook.discordWebhook = null;
    }

    // Messages
    prefix = formatString(messagesConfig.getString("prefix"));
    supportUrl = messagesConfig.getString("support-url");

    header = fromList(messagesConfig.getStringList("header"));
    footer = fromList(messagesConfig.getStringList("footer"));
    tooManyOnlinePerIp = deserialize(fromList(messagesConfig.getStringList("too-many-online-per-ip")));

    noPermission = formatString(messagesConfig.getString("commands.no-permission"));
    commands.incorrectCommandUsage = formatString(messagesConfig.getString("commands.incorrect-usage"));
    commands.invalidIpAddress = formatString(messagesConfig.getString("commands.invalid-ip-address"));
    commands.playersOnly = formatString(messagesConfig.getString("commands.player-only"));
    commands.consoleOnly = formatString(messagesConfig.getString("commands.console-only"));
    commands.commandCoolDown = formatString(messagesConfig.getString("commands.cool-down"));
    commands.commandCoolDownLeft = formatString(messagesConfig.getString("commands.cool-down-left"));
    commands.subCommandNoPerm = formatString(messagesConfig.getString("commands.subcommand-no-permission"));
    commands.helpHeader = messagesConfig.getStringList("commands.main.header");
    commands.helpSubcommands = formatString(messagesConfig.getString("commands.main.subcommands"));

    SonarCommand.prepareCachedMessages();

    commands.reloading = formatString(messagesConfig.getString("commands.reload.start"));
    commands.reloaded = formatString(messagesConfig.getString("commands.reload.finish"));

    commands.verboseSubscribed = formatString(messagesConfig.getString("commands.verbose.subscribed"));
    commands.verboseUnsubscribed = formatString(messagesConfig.getString("commands.verbose.unsubscribed"));

    commands.notificationsSubscribed = formatString(messagesConfig.getString("commands.notify.subscribed"));
    commands.notificationsUnsubscribed = formatString(messagesConfig.getString("commands.notify.unsubscribed"));

    commands.blacklistEmpty = formatString(messagesConfig.getString("commands.blacklist.empty"));
    commands.blacklistCleared = formatString(messagesConfig.getString("commands.blacklist.cleared"));
    commands.blacklistSize = formatString(messagesConfig.getString("commands.blacklist.size"));
    commands.blacklistAdd = formatString(messagesConfig.getString("commands.blacklist.added"));
    commands.blacklistAddWarning = formatString(messagesConfig.getString("commands.blacklist.added-warning"));
    commands.blacklistRemove = formatString(messagesConfig.getString("commands.blacklist.removed"));
    commands.blacklistDuplicate = formatString(messagesConfig.getString("commands.blacklist.duplicate-ip"));
    commands.blacklistNotFound = formatString(messagesConfig.getString("commands.blacklist.ip-not-found"));

    commands.verifiedEmpty = formatString(messagesConfig.getString("commands.verified.empty"));
    commands.verifiedCleared = formatString(messagesConfig.getString("commands.verified.cleared"));
    commands.verifiedSize = formatString(messagesConfig.getString("commands.verified.size"));
    commands.verifiedRemove = formatString(messagesConfig.getString("commands.verified.removed"));
    commands.verifiedAdd = formatString(messagesConfig.getString("commands.verified.added"));
    commands.verifiedAlready = formatString(messagesConfig.getString("commands.verified.already"));
    commands.verifiedNotFound = formatString(messagesConfig.getString("commands.verified.ip-not-found"));
    commands.verifiedBlocked = formatString(messagesConfig.getString("commands.verified.blocked"));

    commands.statisticsHeader = formatString(messagesConfig.getString("commands.statistics.header"));
    commands.unknownStatisticType = formatString(messagesConfig.getString("commands.statistics.unknown-type"));
    commands.generalStatistics = formatString(fromList(messagesConfig.getStringList("commands.statistics.general")));
    commands.cpuStatistics = formatString(fromList(messagesConfig.getStringList("commands.statistics.cpu")));
    commands.memoryStatistics = formatString(fromList(messagesConfig.getStringList("commands.statistics.memory")));
    commands.networkStatistics = formatString(fromList(messagesConfig.getStringList("commands.statistics.network")));

    verification.connectLog = formatString(messagesConfig.getString("verification.logs.connection"));
    verification.failedLog = formatString(messagesConfig.getString("verification.logs.failed"));
    verification.blacklistLog = formatString(messagesConfig.getString("verification.logs.blacklisted"));
    verification.successLog = formatString(messagesConfig.getString("verification.logs.successful"));

    verification.map.enterCode = deserialize(formatString(messagesConfig.getString("verification.captcha.enter-code")));
    verification.map.failedCaptcha = deserialize(formatString(messagesConfig.getString("verification.captcha.incorrect")));
    verification.currentlyPreparing = deserialize(fromList(messagesConfig.getStringList("verification.currently-preparing")));
    verification.tooFastReconnect = deserialize(fromList(messagesConfig.getStringList("verification.too-fast-reconnect")));
    verification.alreadyVerifying = deserialize(fromList(messagesConfig.getStringList("verification.already-verifying")));
    verification.alreadyQueued = deserialize(fromList(messagesConfig.getStringList("verification.already-queued")));
    verification.blacklisted = deserialize(fromList(messagesConfig.getStringList("verification.blacklisted")));
    verification.invalidUsername = deserialize(fromList(messagesConfig.getStringList("verification.invalid-username")));
    verification.protocolBlacklisted = deserialize(fromList(messagesConfig.getStringList("verification.blacklisted-protocol")));
    verification.verificationSuccess = deserialize(fromList(messagesConfig.getStringList("verification.success")));
    verification.verificationFailed = deserialize(fromList(messagesConfig.getStringList("verification.failed")));

    verbose.actionBarLayout = deserialize(formatString(messagesConfig.getString("verbose.layout.normal")));
    verbose.actionBarLayoutDuringAttack = deserialize(formatString(messagesConfig.getString("verbose.layout.attack")));
    verbose.animation = Collections.unmodifiableList(messagesConfig.getStringList("verbose.animation"));

    notifications.notificationTitle = deserialize(formatString(messagesConfig.getString("notifications.title")));
    notifications.notificationSubtitle = deserialize(formatString(messagesConfig.getString("notifications.subtitle")));
    notifications.title = Title.title(
      Sonar.get().getConfig().getNotifications().getNotificationTitle(),
      Sonar.get().getConfig().getNotifications().getNotificationSubtitle());
    notifications.notificationChat = deserialize(formatString(fromList(messagesConfig.getStringList("notifications.chat"))));
  }

  private @NotNull URL getAsset(final String url, final @NotNull Language language) {
    final String resourceName = url + "/" + language.getCode() + ".yml";
    URL result = Sonar.class.getResource("/assets/" + resourceName);
    if (result == null) {
      LOGGER.warn("Could not find " + resourceName + "! Using en.yml!");
      result = Objects.requireNonNull(Sonar.class.getResource("/assets/" + url + "/en.yml"));
    }
    return result;
  }

  private static int clamp(final int v, final int max, final int min) {
    final int output = Math.max(Math.min(v, min), max);
    if (output != v) {
      Sonar.get().getLogger().warn("Clamped configuration value {} to {}", v, output);
    }
    return output;
  }

  public String formatAddress(final InetAddress inetAddress) {
    return logPlayerAddresses ? inetAddress.toString() : "/<ip address withheld>";
  }

  private @NotNull String fromList(final @NotNull Collection<String> list) {
    return formatString(String.join("<newline>", list));
  }

  private static @NotNull Component deserialize(final String legacy) {
    return MiniMessage.miniMessage().deserialize(legacy);
  }

  private @NotNull String formatString(final @NotNull String str) {
    return str
      .replace("%prefix%", prefix == null ? "" : prefix)
      .replace("%support-url%", supportUrl == null ? "" : supportUrl)
      .replace("%header%", header == null ? "" : header)
      .replace("%footer%", footer == null ? "" : footer);
  }

  @Getter
  private final Verbose verbose = new Verbose();
  @Getter
  private final Commands commands = new Commands();
  @Getter
  private final Queue queue = new Queue();
  @Getter
  private final Verification verification = new Verification();
  @Getter
  private final Database database = new Database();
  @Getter
  private final Webhook webhook = new Webhook();
  @Getter
  private final Notifications notifications = new Notifications();

  @Getter
  private String prefix;
  private String supportUrl;
  @Getter
  private String noPermission;
  private String header, footer;
  private boolean logPlayerAddresses;
  @Getter
  private int maxOnlinePerIp;
  @Getter
  private int minPlayersForAttack;
  @Getter
  private int minAttackDuration;
  @Getter
  private int minAttackThreshold;
  @Getter
  private int attackCooldownDelay;
  @Getter
  private Component tooManyOnlinePerIp;

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Verbose {
    private Component actionBarLayout;
    private Component actionBarLayoutDuringAttack;
    private List<String> animation;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Notifications {
    private Title title;
    private Component notificationTitle;
    private Component notificationSubtitle;
    private Component notificationChat;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Queue {
    private int maxQueuePolls;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Verification {
    private Timing timing;

    @Getter
    @RequiredArgsConstructor
    public enum Timing {
      ALWAYS("Always"),
      DURING_ATTACK("During Attack"),
      NEVER("Never");

      private final String displayName;
    }

    private final Map map = new Map();
    private final Gravity gravity = new Gravity();
    private final Vehicle vehicle = new Vehicle();
    private final Brand brand = new Brand();
    private final Transfer transfer = new Transfer();

    @Getter
    public static final class Map {
      private Timing timing;
      private boolean scratches;
      private boolean ripple;
      private boolean bump;
      private File backgroundImage;
      private boolean autoColor;
      private int precomputeAmount;
      private int maxDuration;
      private int maxTries;
      private String dictionary;
      private Component enterCode;
      private Component failedCaptcha;

      private Smear distortion = new Smear();

      @Getter
      public static final class Smear {
        private boolean enabled;
        private int shape;
        private int distance;
        private float density;
        private float mix;
      }
    }

    @Getter
    public static final class Gravity {
      private boolean enabled;
      private boolean checkCollisions;
      private boolean captchaOnFail;
      private Gamemode gamemode;
      private int maxMovementTicks;

      @Getter
      @RequiredArgsConstructor
      public enum Gamemode {
        SURVIVAL(0),
        CREATIVE(1),
        ADVENTURE(2),
        // Keep this for backwards compatibility
        SPECTATOR(2);

        private final int id;
      }
    }

    @Getter
    public static final class Vehicle {
      private Timing timing;
    }

    @Getter
    public static final class Brand {
      private boolean enabled;
      private int maxLength;
      private Pattern validRegex;
    }

    @Getter
    public static final class Transfer {
      private boolean enabled;
      private String host;
      private int port;
    }

    private boolean checkGeyser;
    private boolean logConnections;
    private boolean logDuringAttack;
    private boolean debugXYZPositions;
    private Pattern validNameRegex;
    private Pattern validLocaleRegex;
    private String connectLog;
    private String failedLog;
    private String successLog;
    private String blacklistLog;

    private int maxLoginPackets;
    private int readTimeout;
    private int reconnectDelay;
    private int rememberTime;
    private int blacklistTime;
    private int blacklistThreshold;
    private final Collection<Integer> whitelistedProtocols = new HashSet<>(0);
    private final Collection<Integer> blacklistedProtocols = new HashSet<>(0);

    private Component tooFastReconnect;
    private Component invalidUsername;
    private Component verificationSuccess;
    private Component verificationFailed;
    private Component alreadyVerifying;
    private Component alreadyQueued;
    private Component blacklisted;
    private Component protocolBlacklisted;
    private Component currentlyPreparing;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Commands {
    private String incorrectCommandUsage;
    private String invalidIpAddress;
    private String subCommandNoPerm;
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
    private String verifiedAdd;
    private String verifiedAlready;
    private String verifiedNotFound;
    private String verifiedCleared;
    private String verifiedSize;
    private String verifiedEmpty;
    private String verifiedBlocked;

    private String statisticsHeader;
    private String unknownStatisticType;
    private String generalStatistics;
    private String memoryStatistics;
    private String networkStatistics;
    private String cpuStatistics;

    private List<String> helpHeader;
    private String helpSubcommands;

    private String verboseSubscribed;
    private String verboseUnsubscribed;

    private String notificationsSubscribed;
    private String notificationsUnsubscribed;

    private String reloading;
    private String reloaded;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Database {

    @Getter
    @RequiredArgsConstructor
    public enum Type {
      MYSQL("jdbc:mysql://%s:%d/%s", new MysqlDatabaseTypeAdapter()),
      MARIADB("jdbc:mariadb://%s:%d/%s", new MariaDbDatabaseTypeAdapter()),
      NONE(null, null);

      private final String connectionString;
      private final DatabaseType databaseType;
    }

    private Type type;
    private String host;
    private int port;
    private String name;
    private String username;
    private String password;
    private int maximumAge;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Webhook {
    private String url;
    private String username;
    private String avatarUrl;
    private String content;

    private final Footer footer = new Footer();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Footer {
      private String text;
      private String iconUrl;
    }

    private final Embed embed = new Embed();

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Embed {
      private String title;
      private String titleUrl;
      @Setter
      private String description;
      private int r, g, b;
    }

    @Getter
    private @Nullable DiscordWebhook discordWebhook;
  }
}
