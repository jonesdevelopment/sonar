/*
 * Copyright (C) 2025 Sonar Contributors
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

import com.alessiodp.libby.Library;
import com.j256.ormlite.db.DatabaseType;
import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.SonarCommand;
import xyz.jonesdev.sonar.api.database.ormlite.H2DatabaseTypeAdapter;
import xyz.jonesdev.sonar.api.database.ormlite.MariaDbDatabaseTypeAdapter;
import xyz.jonesdev.sonar.api.database.ormlite.MysqlDatabaseTypeAdapter;
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
  @Getter
  private final File languageFile, pluginFolder;
  @Getter
  private Language language;

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
        Sonar.get0().getLogger().error("Cannot check for custom language (is the file missing?)");
        return DEFAULT_FALLBACK_LANGUAGE;
      }
      // Copy the file to the plugin data directory
      try (final InputStream inputStream = defaultLanguageFile.openStream()) {
        Files.copy(inputStream, languageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException exception) {
        Sonar.get0().getLogger().error("Error copying file: {}", exception);
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
        Sonar.get0().getLogger().error("Could not find requested language: {}", throwable);
        Sonar.get0().getLogger().error("You can view a full list of valid language codes here:");
        Sonar.get0().getLogger().error("https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes");
        Sonar.get0().getLogger().error("If a translation does not exist yet, Sonar will use English (en).");
      }
    } catch (IOException exception) {
      Sonar.get0().getLogger().error("Error reading language file: {}", exception);
    }
    return DEFAULT_FALLBACK_LANGUAGE;
  }

  public void load() {
    // Make sure the plugin folder actually exists before trying to copy files into it
    if (!pluginFolder.exists() && !pluginFolder.mkdirs()) {
      throw new IllegalStateException("Could not create plugin folder (insufficient permissions?)");
    }

    // Generate the language file and check what it's set to
    language = getPreferredLanguage();
    if (language == Language.SYSTEM) {
      try {
        // Try using the system language to determine the language file
        final String property = System.getProperty("user.language", "en");
        language = Language.fromCode(property);
        // Make sure the user knows that we're using the system language for translations
        Sonar.get0().getLogger().info("Using system language ({}) for translations.", language);
      } catch (Exception exception) {
        Sonar.get0().getLogger().warn("Could not use system language for translations.");
        Sonar.get0().getLogger().warn("Using default language ({}) for translations.", language);
      }
    } else {
      Sonar.get0().getLogger().info("Using custom language ({}) for translations.", language);
    }

    // Load all configurations
    try {
      generalConfig.load(getAsset("config", language));
      messagesConfig.load(getAsset("messages", language));
      webhookConfig.load(getAsset("webhook", language));
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
    maxOnlinePerIp = clamp(generalConfig.getInt("general.max-online-per-ip"), 0, 99);

    // Attack tracker
    minPlayersForAttack = clamp(generalConfig.getInt("attack-tracker.min-players-for-attack"), 2, 1024);
    minAttackDuration = clamp(generalConfig.getInt("attack-tracker.min-attack-duration"), 1000, 900000);
    minAttackThreshold = clamp(generalConfig.getInt("attack-tracker.min-attack-threshold"), 0, 20);
    attackCooldownDelay = clamp(generalConfig.getInt("attack-tracker.attack-cooldown-delay"), 100, 30000);

    // Database
    database.type = Database.Type.valueOf(generalConfig.getString("database.type").toUpperCase());
    database.maximumAge = clamp(generalConfig.getInt("database.maximum-age"), 1, 365);

    // Queue
    queue.maxQueuePolls = clamp(generalConfig.getInt("queue.max-polls"), 1, 1000);

    // Verification
    verification.timing = Verification.Timing.valueOf(generalConfig.getString("verification.timing"));
    // Warn the user if the verification timing is set to NEVER
    if (verification.timing == Verification.Timing.NEVER) {
      Sonar.get0().getLogger().warn(" ");
      Sonar.get0().getLogger().warn("You have set the verification timing to 'NEVER'.");
      Sonar.get0().getLogger().warn("Sonar will NOT perform the bot verification at all, therefore making it useless.");
      Sonar.get0().getLogger().warn("It is highly suggested to set this option to either 'DURING_ATTACK' or 'ALWAYS'.");
      Sonar.get0().getLogger().warn("Please only edit this option if you really know what you are doing.");
      Sonar.get0().getLogger().warn(" ");
    }

    verification.gravity.enabled = generalConfig.getBoolean("verification.checks.gravity.enabled");
    verification.gravity.checkCollisions = generalConfig.getBoolean("verification.checks.collision.enabled");
    verification.gravity.captchaOnFail = generalConfig.getBoolean("verification.checks.gravity.captcha-on-fail");
    verification.gravity.maxMovementTicks = clamp(generalConfig.getInt("verification.checks.gravity.max-movement-ticks"), 2, 100);

    verification.vehicle.enabled = generalConfig.getBoolean("verification.checks.vehicle.enabled");
    verification.vehicle.minimumPackets = clamp(generalConfig.getInt("verification.checks.vehicle.minimum-packets"), 0, 20);

    verification.map.timing = Verification.Timing.valueOf(generalConfig.getString("verification.checks.map-captcha.timing"));
    verification.map.precomputeAmount = clamp(generalConfig.getInt("verification.checks.map-captcha.precompute"), 10, 5000);
    verification.map.maxDuration = clamp(generalConfig.getInt("verification.checks.map-captcha.max-duration"), 5000, 360000);
    verification.map.maxTries = clamp(generalConfig.getInt("verification.checks.map-captcha.max-tries"), 1, 100);
    verification.map.alphabet = generalConfig.getString("verification.checks.map-captcha.alphabet");
    verification.map.backgroundImage = null;

    final String backgroundPath = generalConfig.getString("verification.checks.map-captcha.background");
    if (!backgroundPath.isEmpty()) {
      final File backgroundFile = new File(pluginFolder, backgroundPath);
      if (backgroundFile.exists()) {
        verification.map.backgroundImage = backgroundFile;
      } else {
        Sonar.get0().getLogger().error("Could not find background image {}", backgroundFile.getAbsolutePath());
      }
    }

    verification.brand.enabled = generalConfig.getBoolean("verification.checks.client-brand.enabled");
    verification.brand.validRegex = Pattern.compile(generalConfig.getString("verification.checks.client-brand.valid-regex"));
    verification.brand.maxLength = generalConfig.getInt("verification.checks.client-brand.max-length");

    verification.timeOfDay = clamp(generalConfig.getInt("verification.time-of-day"), 0, 24000);
    verification.gamemode = Verification.Gamemode.valueOf(generalConfig.getString("verification.gamemode"));
    verification.validNameRegex = Pattern.compile(generalConfig.getString("verification.checks.valid-name-regex"));

    verification.checkGeyser = generalConfig.getBoolean("verification.check-geyser-players");
    verification.logConnections = generalConfig.getBoolean("verification.log-connections");
    verification.logDuringAttack = generalConfig.getBoolean("verification.log-during-attack");
    verification.debugXYZPositions = generalConfig.getBoolean("verification.debug-xyz-positions");
    verification.readTimeout = clamp(generalConfig.getInt("verification.read-timeout"), 1000, 30000);
    verification.writeTimeout = clamp(generalConfig.getInt("verification.write-timeout"), 1000, 30000);
    verification.reconnectDelay = clamp(generalConfig.getInt("verification.rejoin-delay"), 0, 100000);
    verification.rememberTime = clamp(generalConfig.getInt("verification.remember-time"), 0, 86400000);
    verification.blacklistTime = clamp(generalConfig.getInt("verification.blacklist-time"), 0, 86400000);
    verification.blacklistThreshold = clamp(generalConfig.getInt("verification.blacklist-threshold"), 0, 100);
    verification.blacklistedProtocols.clear();
    verification.blacklistedProtocols.addAll(generalConfig.getIntList("verification.blacklisted-protocols"));

    // Webhooks
    webhook.url = webhookConfig.getString("webhook.url");
    webhook.username = webhookConfig.getString("webhook.username");
    webhook.avatarUrl = webhookConfig.getString("webhook.avatar-url");
    webhook.content = webhookConfig.getString("webhook.content");
    webhook.footer.text = webhookConfig.getString("webhook.embed.footer.text");
    webhook.footer.iconUrl = webhookConfig.getString("webhook.embed.footer.icon-url");
    webhook.embed.title = webhookConfig.getString("webhook.embed.title");
    webhook.embed.titleUrl = webhookConfig.getString("webhook.embed.title-url");
    webhook.embed.description = String.join("\n", webhookConfig.getStringList("webhook.embed.description"));
    webhook.embed.r = webhookConfig.getInt("webhook.embed.color.red");
    webhook.embed.g = webhookConfig.getInt("webhook.embed.color.green");
    webhook.embed.b = webhookConfig.getInt("webhook.embed.color.blue");

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
    prefix = MiniMessage.miniMessage().deserialize(messagesConfig.getString("prefix"));

    final String noPermissionMessage = messagesConfig.getString("commands.no-permission");
    if (noPermissionMessage.isEmpty()) {
      noPermission = null;
    } else {
      noPermission = MiniMessage.miniMessage().deserialize(noPermissionMessage,
        Placeholder.component("prefix", prefix));
    }

    SonarCommand.prepareCachedTabSuggestions();

    supportUrl = messagesConfig.getString("support-url");
    header = MiniMessage.miniMessage().deserialize(
      String.join("<newline>", messagesConfig.getStringList("header")),
      Placeholder.unparsed("support-url", supportUrl),
      Placeholder.component("prefix", prefix));
    footer = MiniMessage.miniMessage().deserialize(
      String.join("<newline>", messagesConfig.getStringList("footer")),
      Placeholder.unparsed("support-url", supportUrl),
      Placeholder.component("prefix", prefix));

    tooManyOnlinePerIp = deserializeDisconnectMessage("too-many-online-per-ip");
    verification.currentlyPreparing = deserializeDisconnectMessage("verification.currently-preparing");
    verification.unsupportedVersion = deserializeDisconnectMessage("verification.unsupported-version");
    verification.tooFastReconnect = deserializeDisconnectMessage("verification.too-fast-reconnect");
    verification.alreadyVerifying = deserializeDisconnectMessage("verification.already-verifying");
    verification.alreadyQueued = deserializeDisconnectMessage("verification.already-queued");
    verification.blacklisted = deserializeDisconnectMessage("verification.blacklisted");
    verification.invalidUsername = deserializeDisconnectMessage("verification.invalid-username");
    verification.protocolBlacklisted = deserializeDisconnectMessage("verification.blacklisted-protocol");
    verification.verificationSuccess = deserializeDisconnectMessage("verification.success");
    verification.verificationFailed = deserializeDisconnectMessage("verification.failed");

    verboseAnimation = Collections.unmodifiableList(messagesConfig.getStringList("verbose.animation"));
  }

  private @NotNull Component deserializeDisconnectMessage(final String path) {
    return MiniMessage.miniMessage().deserialize(
      String.join("<newline>", messagesConfig.getStringList(path)),
      Placeholder.component("prefix", prefix),
      Placeholder.component("header", header),
      Placeholder.component("footer", footer),
      Placeholder.unparsed("support-url", supportUrl));
  }

  private @NotNull URL getAsset(final String url, final @NotNull Language language) {
    final String resourceName = url + "/" + language.getCode() + ".yml";
    URL result = Sonar.class.getResource("/assets/" + resourceName);
    if (result == null) {
      Sonar.get0().getLogger().warn("Could not find " + resourceName + "! Using en.yml!");
      result = Objects.requireNonNull(Sonar.class.getResource("/assets/" + url + "/en.yml"));
    }
    return result;
  }

  private static int clamp(final int v, final int max, final int min) {
    final int output = Math.max(Math.min(v, min), max);
    if (output != v) {
      Sonar.get0().getLogger().warn("Clamped configuration value {} to {}", v, output);
    }
    return output;
  }

  public @NotNull String formatAddress(final @NotNull InetAddress inetAddress) {
    return logPlayerAddresses ? inetAddress.getHostAddress() : "<ip address withheld>";
  }

  @Getter
  private final Queue queue = new Queue();
  @Getter
  private final Verification verification = new Verification();
  @Getter
  private final Database database = new Database();
  @Getter
  private final Webhook webhook = new Webhook();

  @Getter
  private Component prefix;
  private String supportUrl;
  private Component header;
  private Component footer;
  @Getter
  private Component noPermission;
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
  private List<String> verboseAnimation;

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

    @Getter
    public static final class Map {
      private Timing timing;
      private int precomputeAmount;
      private int maxDuration;
      private int maxTries;
      private String alphabet;
      private File backgroundImage;
    }

    @Getter
    public static final class Gravity {
      private boolean enabled;
      private boolean checkCollisions;
      private boolean captchaOnFail;
      private int maxMovementTicks;
    }

    @Getter
    public static final class Vehicle {
      private boolean enabled;
      private int minimumPackets;
    }

    @Getter
    public static final class Brand {
      private boolean enabled;
      private int maxLength;
      private Pattern validRegex;
    }

    private Gamemode gamemode;

    @Getter
    @RequiredArgsConstructor
    public enum Gamemode {
      NOT_SET(-1),
      SURVIVAL(0),
      CREATIVE(1),
      ADVENTURE(2);

      private final int id;

      public boolean isSurvivalOrAdventure() {
        return this == SURVIVAL || this == ADVENTURE;
      }
    }

    private int timeOfDay;
    private boolean checkGeyser;
    private boolean logConnections;
    private boolean logDuringAttack;
    private boolean debugXYZPositions;
    private Pattern validNameRegex;

    private int readTimeout;
    private int writeTimeout;
    private int reconnectDelay;
    private int rememberTime;
    private int blacklistTime;
    private int blacklistThreshold;
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
    private Component unsupportedVersion;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Database {

    @Getter
    @RequiredArgsConstructor
    public enum Type {
      MYSQL("MySQL", "jdbc:mysql://%s:%d/%s%s", new MysqlDatabaseTypeAdapter(),
        Library.builder()
          .groupId("com{}mysql")
          .artifactId("mysql-connector-j")
          .version("9.0.0")
          .relocate("com{}mysql", "xyz{}jonesdev{}sonar{}libs{}mysql")
          .build()),
      MARIADB("MariaDB", "jdbc:mariadb://%s:%d/%s%s", new MariaDbDatabaseTypeAdapter(),
        Library.builder()
          .groupId("org{}mariadb{}jdbc")
          .artifactId("mariadb-java-client")
          .version("3.4.1")
          .relocate("org{}mariadb", "xyz{}jonesdev{}sonar{}libs{}mariadb")
          .build()),
      H2("H2", "jdbc:h2:file:%s", new H2DatabaseTypeAdapter(),
        Library.builder()
          .groupId("com{}h2database")
          .artifactId("h2")
          .version("2.2.220")
          .relocate("org{}h2", "xyz{}jonesdev{}sonar{}libs{}h2")
          .build()),
      NONE("None", null, null, null);

      private final String displayName;
      private final String connectionString;
      private final DatabaseType databaseType;
      private final Library databaseDriver;
      @Setter
      private boolean loaded;
    }

    private Type type;
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
