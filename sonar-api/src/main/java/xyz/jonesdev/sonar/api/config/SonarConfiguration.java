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

import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.SonarCommand;
import xyz.jonesdev.sonar.api.dependencies.Dependency;
import xyz.jonesdev.sonar.api.webhook.DiscordWebhook;

import java.awt.*;
import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

import static xyz.jonesdev.sonar.api.Sonar.LINE_SEPARATOR;

public final class SonarConfiguration {
  private final @NotNull File pluginFolder;
  @Getter
  private SimpleYamlConfig generalConfig, messagesConfig;

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

  public SonarConfiguration(final @NotNull File pluginFolder) {
    this.pluginFolder = pluginFolder;
  }

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
    private String actionBarLayout;
    private String actionBarLayoutDuringAttack;
    private List<String> animation;
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
    public enum Timing {
      ALWAYS, DURING_ATTACK, NEVER
    }

    private final Map map = new Map();
    private final Gravity gravity = new Gravity();

    @Getter
    public static final class Map {
      private Timing timing;
      private boolean randomizePositions;
      private boolean randomizeFontSize;
      private double distortionsFactorX;
      private double distortionsFactorY;
      private int randomLinesAmount;
      private int randomOvalsAmount;
      private int precomputeAmount;
      private int maxDuration;
      private int maxTries;
      private String dictionary;
      private Component enterCode;
      private Component failedCaptcha;
      private String enterCodeActionBar;
      private List<String> fonts;
    }

    @Getter
    public static final class Gravity {
      private boolean enabled;
      private Gamemode gamemode;
      private int maxMovementTicks;
      private int maxIgnoredTicks;
      private Component youAreBeingChecked;

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

    private boolean logConnections;
    private boolean logDuringAttack;
    private boolean debugXYZPositions;
    private Pattern validNameRegex;
    private Pattern validBrandRegex;
    private Pattern validLocaleRegex;
    private String connectLog;
    private String failedLog;
    private String successLog;
    private String blacklistLog;

    private int maxBrandLength;
    private int maxLoginPackets;
    private int maxPing;
    private int readTimeout;
    private int reconnectDelay;
    private final Collection<Integer> whitelistedProtocols = new HashSet<>(0);
    private final Collection<Integer> blacklistedProtocols = new HashSet<>(0);

    private Component tooManyPlayers;
    private Component tooFastReconnect;
    private Component invalidUsername;
    private Component invalidProtocol;
    private Component alreadyConnected;
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
    private String incorrectIpAddress;
    private String illegalIpAddress;
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

    private String reloading;
    private String reloaded;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Database {

    @Getter
    @RequiredArgsConstructor
    public enum Type {
      MYSQL(Dependency.MYSQL, "com.mysql.cj.jdbc.NonRegisteringDriver"),
      NONE(null, null);

      private final Dependency dependency;
      private final String driverClassName;
    }

    private Type type;
    private String url;
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
  }

  @Getter
  private @Nullable DiscordWebhook discordWebhook;

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
    generalConfig.getYaml().setComment("language",
      "Name of the language file Sonar should use for messages");
    String language = generalConfig.getString("language", "en");

    generalConfig.getYaml().setComment("max-online-per-ip",
      "Maximum number of players online with the same IP address");
    maxOnlinePerIp = clamp(generalConfig.getInt("max-online-per-ip", 3), 1, Byte.MAX_VALUE);

    generalConfig.getYaml().setComment("min-players-for-attack",
      "Minimum number of new players in order for an attack to be detected");
    minPlayersForAttack = clamp(generalConfig.getInt("min-players-for-attack", 8), 2, 1024);

    generalConfig.getYaml().setComment("min-attack-duration",
      "Amount of time (in milliseconds) that has to pass in order for an attack to be over");
    minAttackDuration = clamp(generalConfig.getInt("min-attack-duration", 30000), 1000, 900000);

    generalConfig.getYaml().setComment("min-attack-threshold",
      "Number of times an incident has to be reported in order to be acknowledged as an attack"
        + LINE_SEPARATOR + "This number acts as a buffer to filter out false attack notifications");
    minAttackThreshold = clamp(generalConfig.getInt("min-attack-threshold", 2), 0, 20);

    generalConfig.getYaml().setComment("attack-cooldown-delay",
      "Amount of time (in milliseconds) that has to pass in order for a new attack to be detected");
    attackCooldownDelay = clamp(generalConfig.getInt("attack-cooldown-delay", 3000), 100, 30000);

    generalConfig.getYaml().setComment("log-player-addresses",
      "Should Sonar log players' IP addresses in console?");
    logPlayerAddresses = generalConfig.getBoolean("log-player-addresses", true);

    // Message settings
    // Only create a new messages configuration object if the preferred language changed
    // https://github.com/jonesdevelopment/sonar/issues/26
    if (messagesConfig == null || !messagesConfig.getFile().getName().equals(language + ".yml")) {
      messagesConfig = new SimpleYamlConfig(pluginFolder, "translations/" + language);
    }
    try {
      messagesConfig.load();
    } catch (Exception exception) {
      // https://github.com/jonesdevelopment/sonar/issues/33
      // Only save the configuration when necessary
      Sonar.get().getLogger().error("Error while loading configuration: {}", exception);
      return;
    }

    messagesConfig.getYaml().setComment("prefix",
      "Placeholder for every '%prefix%' in this configuration file");
    prefix = formatString(messagesConfig.getString("prefix", "<yellow><bold>Sonar<reset> <gray>» <white>"));

    messagesConfig.getYaml().setComment("support-url",
      "Placeholder for every '%support-url%' in this configuration file");
    supportUrl = messagesConfig.getString("support-url", "https://jonesdev.xyz/discord/");

    // Database
    generalConfig.getYaml().setComment("database.type",
      "Type of database Sonar uses to store verified players"
        + LINE_SEPARATOR + "Possible types: NONE, MYSQL");
    database.type = Database.Type.valueOf(
      generalConfig.getString("database.type", Database.Type.NONE.name()).toUpperCase());

    generalConfig.getYaml().setComment("database",
      "You can connect Sonar to a database to keep verified players even after restarting your server"
        + LINE_SEPARATOR + "Note: IP addresses are saved in plain text. You are responsible for keeping your database" +
        " safe!"
        + LINE_SEPARATOR + "However, IP addresses cannot be traced back to players as Sonar uses UUIDs instead of " +
        "usernames");
    generalConfig.getYaml().setComment("database.url",
      "URL for authenticating with the SQL database");
    database.url = generalConfig.getString("database.url", "localhost");

    generalConfig.getYaml().setComment("database.port",
      "Port for authenticating with the SQL database");
    database.port = generalConfig.getInt("database.port", 3306);

    generalConfig.getYaml().setComment("database.name",
      "Name of the SQL database");
    database.name = generalConfig.getString("database.name", "sonar");

    generalConfig.getYaml().setComment("database.username",
      "Username for authenticating with the SQL database");
    database.username = generalConfig.getString("database.username", "");

    generalConfig.getYaml().setComment("database.password",
      "Password for authenticating with the SQL database");
    database.password = generalConfig.getString("database.password", "");

    generalConfig.getYaml().setComment("database.maximum-age",
      "How many days should Sonar keep verified players in the database?");
    database.maximumAge = clamp(generalConfig.getInt("database.maximum-age", 5), 1, 365);

    // Queue
    generalConfig.getYaml().setComment("queue",
      "Every new login request will be queued to avoid spam join attacks"
        + LINE_SEPARATOR + "The queue is updated every 500 milliseconds (10 ticks)");
    generalConfig.getYaml().setComment("queue.max-polls",
      "Maximum number of concurrent queue polls per 500 milliseconds");
    queue.maxQueuePolls = clamp(generalConfig.getInt("queue.max-polls", 30), 1, 1000);

    // Verification
    generalConfig.getYaml().setComment("verification",
      "Every new player that joins for the first time will be sent to"
        + LINE_SEPARATOR + "a lightweight limbo server where advanced bot checks are performed");
    generalConfig.getYaml().setComment("verification.timing",
      "When should Sonar verify new players?"
        + LINE_SEPARATOR + "Possible types: ALWAYS, DURING_ATTACK, NEVER"
        + LINE_SEPARATOR + "- ALWAYS: New players will always be checked (Recommended)"
        + LINE_SEPARATOR + "- DURING_ATTACK: New players will only be checked during an attack"
        + LINE_SEPARATOR + "- NEVER: New players will never be checked");
    verification.timing = Verification.Timing.valueOf(
      generalConfig.getString("verification.timing", Verification.Timing.ALWAYS.name()).toUpperCase());

    generalConfig.getYaml().setComment("verification.checks.gravity",
      "Checks if the players' falling motion is following Minecraft's gravity formula"
        + LINE_SEPARATOR + "All predicted motions are precalculated in order to save performance");
    generalConfig.getYaml().setComment("verification.checks.gravity.enabled",
      "Should Sonar check for valid client gravity? (Recommended)");
    verification.gravity.enabled = generalConfig.getBoolean("verification.checks.gravity.enabled", true);

    generalConfig.getYaml().setComment("verification.checks.gravity.max-movement-ticks",
      "Maximum number of ticks the player has to fall in order to be allowed to hit the platform");
    verification.gravity.maxMovementTicks = clamp(generalConfig.getInt(
      "verification.checks.gravity.max-movement-ticks", 8), 2, 100);

    generalConfig.getYaml().setComment("verification.checks.gravity.max-ignored-ticks",
      "Maximum number of ignored Y movement changes before a player fails verification");
    verification.gravity.maxIgnoredTicks = clamp(generalConfig.getInt(
      "verification.checks.gravity.max-ignored-ticks", 5), 1, 128);

    generalConfig.getYaml().setComment("verification.checks.gravity.gamemode",
      "The gamemode of the player during verification"
        + LINE_SEPARATOR + "Possible types: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR"
        + LINE_SEPARATOR + "- SURVIVAL: all UI components are visible"
        + LINE_SEPARATOR + "- CREATIVE: health and hunger are hidden"
        + LINE_SEPARATOR + "- ADVENTURE: all UI components are visible");
    verification.gravity.gamemode = Verification.Gravity.Gamemode.valueOf(
      generalConfig.getString("verification.checks.gravity.gamemode",
        Verification.Gravity.Gamemode.ADVENTURE.name()).toUpperCase());

    generalConfig.getYaml().setComment("verification.checks.map-captcha",
      "Make the player type a code from a virtual map in chat");
    generalConfig.getYaml().setComment("verification.checks.map-captcha.timing",
      "When should Sonar make the player solve a captcha?"
        + LINE_SEPARATOR + "Possible types: ALWAYS, DURING_ATTACK, NEVER"
        + LINE_SEPARATOR + "- ALWAYS: New players will always receive a captcha"
        + LINE_SEPARATOR + "- DURING_ATTACK: New players will only receive a captcha during an attack"
        + LINE_SEPARATOR + "- NEVER: New players will never receive a captcha (Recommended)");
    verification.map.timing = Verification.Timing.valueOf(
      generalConfig.getString("verification.checks.map-captcha.timing",
        Verification.Timing.NEVER.name()).toUpperCase());

    generalConfig.getYaml().setComment("verification.checks.map-captcha.random-position",
      "Should Sonar randomize the X and Y position of the captcha?");
    verification.map.randomizePositions = generalConfig.getBoolean("verification.checks.map-captcha.random-position",
      true);

    generalConfig.getYaml().setComment("verification.checks.map-captcha.distortions-factor-x",
      "How much should Sonar distort characters (factor for randomization)?");
    verification.map.distortionsFactorX = clamp(generalConfig.getInt(
      "verification.checks.map-captcha.distortions-factor-x", 50), 0, 100) / 100D;

    generalConfig.getYaml().setComment("verification.checks.map-captcha.distortions-factor-y",
      "How much should Sonar distort characters (factor for randomization)?");
    verification.map.distortionsFactorY = clamp(generalConfig.getInt(
      "verification.checks.map-captcha.distortions-factor-y", 50), 0, 100) / 100D;

    generalConfig.getYaml().setComment("verification.checks.map-captcha.random-lines",
      "How many random lines behind the captcha should Sonar draw?");
    verification.map.randomLinesAmount = generalConfig.getInt("verification.checks.map-captcha.random-lines", 4);

    generalConfig.getYaml().setComment("verification.checks.map-captcha.random-ovals",
      "How many random ovals behind the captcha should Sonar draw?");
    verification.map.randomOvalsAmount = generalConfig.getInt("verification.checks.map-captcha.random-ovals", 1);

    generalConfig.getYaml().setComment("verification.checks.map-captcha.random-font-size",
      "Should Sonar randomize the size of the font used for rendering the captcha?");
    verification.map.randomizeFontSize = generalConfig.getBoolean("verification.checks.map-captcha.random-font-size",
      true);

    generalConfig.getYaml().setComment("verification.checks.map-captcha.precompute",
      "How many answers should Sonar precompute (prepare)?"
        + LINE_SEPARATOR + "This task happens asynchronously in the background;"
        + LINE_SEPARATOR + "Players are able to join once one captcha has been prepared");
    verification.map.precomputeAmount = generalConfig.getInt("verification.checks.map-captcha.precompute", 1000);

    generalConfig.getYaml().setComment("verification.checks.map-captcha.max-duration",
      "How long should Sonar wait until the player fails the captcha?");
    verification.map.maxDuration = generalConfig.getInt("verification.checks.map-captcha.max-duration", 45000);

    generalConfig.getYaml().setComment("verification.checks.map-captcha.max-tries",
      "How many times must a player fail the captcha before failing the verification?");
    verification.map.maxTries = generalConfig.getInt("verification.checks.map-captcha.max-tries", 3);

    generalConfig.getYaml().setComment("verification.checks.map-captcha.dictionary",
      "Characters (letters and numbers) that are allowed to appear in the answer to the captcha");
    verification.map.dictionary = generalConfig.getString("verification.checks.map-captcha.dictionary", "123456789");

    generalConfig.getYaml().setComment("verification.checks.map-captcha.fonts",
      "Which font types should Sonar use for the map captcha codes?");
    verification.map.fonts = generalConfig.getStringList("verification.checks.map-captcha.fonts",
      Arrays.asList(Font.DIALOG, Font.DIALOG_INPUT, Font.SERIF, Font.SANS_SERIF));

    generalConfig.getYaml().setComment("verification.checks.valid-name-regex",
      "Regex for validating usernames during verification");
    verification.validNameRegex = Pattern.compile(generalConfig.getString(
      "verification.checks.valid-name-regex", "^[a-zA-Z0-9_]+$"));

    generalConfig.getYaml().setComment("verification.checks.valid-brand-regex",
      "Regex for validating client brands during verification");
    verification.validBrandRegex = Pattern.compile(generalConfig.getString(
      "verification.checks.valid-brand-regex", "^[!-~ ]+$"));

    generalConfig.getYaml().setComment("verification.checks.valid-locale-regex",
      "Regex for validating client locale during verification");
    verification.validLocaleRegex = Pattern.compile(generalConfig.getString(
      "verification.checks.valid-locale-regex", "^[a-zA-Z_]+$"));

    generalConfig.getYaml().setComment("verification.checks.max-brand-length",
      "Maximum client brand length during verification");
    verification.maxBrandLength = generalConfig.getInt("verification.checks.max-brand-length", 64);

    generalConfig.getYaml().setComment("verification.checks.max-ping",
      "Ping (in milliseconds) a player has to have in order to timeout");
    verification.maxPing = clamp(generalConfig.getInt("verification.checks.max-ping", 10000), 500, 30000);

    generalConfig.getYaml().setComment("verification.checks.max-login-packets",
      "Maximum number of login packets the player has to send in order to be kicked");
    verification.maxLoginPackets = clamp(generalConfig.getInt("verification.checks.max-login-packets", 256), 128, 8192);

    generalConfig.getYaml().setComment("verification.log-connections",
      "Should Sonar log new verification attempts?");
    verification.logConnections = generalConfig.getBoolean("verification.log-connections", true);

    generalConfig.getYaml().setComment("verification.log-during-attack",
      "Should Sonar log new verification attempts during attacks?");
    verification.logDuringAttack = generalConfig.getBoolean("verification.log-during-attack", false);

    generalConfig.getYaml().setComment("verification.debug-xyz-positions",
      "Should Sonar log every single movement/position change during verification?"
        + LINE_SEPARATOR + "This is not recommended for production servers but can be helpful for spotting errors.");
    verification.debugXYZPositions = generalConfig.getBoolean("verification.debug-xyz-positions", false);

    generalConfig.getYaml().setComment("verification.read-timeout",
      "Amount of time that has to pass before a player times out");
    verification.readTimeout = clamp(generalConfig.getInt("verification.read-timeout", 3500), 500, 30000);

    generalConfig.getYaml().setComment("verification.rejoin-delay",
      "Minimum number of rejoin delay during verification");
    verification.reconnectDelay = clamp(generalConfig.getInt("verification.rejoin-delay", 8000), 0, 100000);

    generalConfig.getYaml().setComment("verification.whitelisted-protocols",
      "List of protocol IDs which are not checked by Sonar (verification bypass)"
        + LINE_SEPARATOR + "You can find the full list of all protocol IDs here:"
        + LINE_SEPARATOR + "https://wiki.vg/Protocol_version_numbers"
        + LINE_SEPARATOR + "For example, Minecraft 1.20 has the ID 763.");
    verification.whitelistedProtocols.clear();
    verification.whitelistedProtocols.addAll(generalConfig.getIntList("verification.whitelisted-protocols",
      new ArrayList<>(0)));

    generalConfig.getYaml().setComment("verification.blacklisted-protocols",
      "List of protocol IDs which are unable to join the server at all");
    verification.blacklistedProtocols.clear();
    verification.blacklistedProtocols.addAll(generalConfig.getIntList("verification.blacklisted-protocols",
      new ArrayList<>(0)));

    generalConfig.getYaml().setComment("webhook",
      "Bot attack notifications can also be sent to your Discord server using webhooks");
    generalConfig.getYaml().setComment("webhook.url",
      "URL of the Discord webhook (Set this to '' to disable webhooks)");
    webhook.url = generalConfig.getString("webhook.url", "");

    generalConfig.getYaml().setComment("webhook.username",
      "Username of the Discord webhook sender");
    webhook.username = generalConfig.getString("webhook.username", "Sonar");

    generalConfig.getYaml().setComment("webhook.avatar-url",
      "URL to the avatar of the Discord webhook sender (Set this to '' to disable)");
    webhook.avatarUrl = generalConfig.getString("webhook.avatar-url", "");

    generalConfig.getYaml().setComment("webhook.content",
      "Content of the Discord webhook message (Set this to '' to disable)"
        + LINE_SEPARATOR + "You can use this to e.g. ping staff members using <@userId>"
        + LINE_SEPARATOR + "If you want to ping roles, you will need to use <@&roleId>");
    webhook.content = generalConfig.getString("webhook.content", "");

    generalConfig.getYaml().setComment("webhook.embed.footer",
      "Small footer message of the Discord webhook embed");
    generalConfig.getYaml().setComment("webhook.embed.footer.text",
      "Content of the footer message of the Discord webhook embed");
    webhook.footer.text = generalConfig.getString("webhook.embed.footer.text",
      "© Jones Development and Sonar Contributors");

    generalConfig.getYaml().setComment("webhook.embed.footer.icon-url",
      "URL of the footer message icon of the Discord webhook embed");
    webhook.footer.iconUrl = generalConfig.getString("webhook.embed.footer.icon-url", "");

    final String realEmbedPath = "webhook.embed";
    final String embedPath = realEmbedPath + ".";

    generalConfig.getYaml().setComment(realEmbedPath,
      "Embed Discord webhook message that is sent when an attack has stopped");
    generalConfig.getYaml().setComment(embedPath + "title",
      "Title of the Discord webhook embed");
    webhook.embed.title = generalConfig.getString(embedPath + "title", ":white_check_mark: Attack mitigated");

    generalConfig.getYaml().setComment(embedPath + "title-url",
      "Clickable URL of the title of the Discord webhook embed");
    webhook.embed.titleUrl = generalConfig.getString(embedPath + "title-url", "");

    generalConfig.getYaml().setComment(embedPath + "description",
      "Description (content) of the Discord webhook embed");
    webhook.embed.description = fromList(generalConfig.getStringList(embedPath + "description", Arrays.asList(
      "The attack on your server has been mitigated.",
      "",
      "Attack start: <t:%start-timestamp%:T>",
      "Attack end: <t:%end-timestamp%:T>",
      "Attack duration: %duration%",
      "",
      "Peak process CPU usage during the attack: %peak-cpu%%",
      "Peak process memory usage during the attack: %peak-memory%",
      "Peak bots per second during the attack: %peak-bps%",
      "",
      "Blacklisted IP addresses during the attack: %total-blacklisted%",
      "Failed verifications during the attack: %total-failed%",
      "Successful verifications during the attack: %total-success%"
    )), LINE_SEPARATOR);

    generalConfig.getYaml().setComment(embedPath + "color",
      "RGB colors of the Discord webhook embed"
        + LINE_SEPARATOR + "Color picker: https://www.rapidtables.com/web/color/RGB_Color.html");
    webhook.embed.r = generalConfig.getInt(embedPath + "color.red", 0);
    webhook.embed.g = generalConfig.getInt(embedPath + "color.green", 255);
    webhook.embed.b = generalConfig.getInt(embedPath + "color.blue", 0);

    if (!webhook.url.isEmpty()) {
      if (webhook.username.isEmpty()) {
        throw new IllegalStateException("Webhook username cannot be empty");
      }
      discordWebhook = new DiscordWebhook(webhook.url);
    } else if (discordWebhook != null) {
      // Reset if Discord webhooks were disabled
      discordWebhook = null;
    }

    // load this here otherwise it could cause issues
    messagesConfig.getYaml().setComment("header",
      "Placeholder for every '%header%' in this configuration file");
    header = fromList(messagesConfig.getStringList("header",
      Arrays.asList(
        "<yellow><bold>Sonar<reset>",
        "<reset>"
      )));

    messagesConfig.getYaml().setComment("footer",
      "Placeholder for every '%footer%' in this configuration file");
    footer = fromList(messagesConfig.getStringList("footer",
      Arrays.asList("<gray>If you believe that this is an error, contact an administrator.")));

    messagesConfig.getYaml().setComment("too-many-online-per-ip",
      "Disconnect message that is shown when someone joins but there are too many online players with their IP " +
        "address");
    tooManyOnlinePerIp = deserialize(fromList(messagesConfig.getStringList("too-many-online-per-ip",
      Arrays.asList(
        "%header%",
        "<red>There are too many players online with your IP address.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("commands.no-permission",
      "Message that is shown when a player tries running /sonar without permission");
    noPermission = formatString(messagesConfig.getString("commands.no-permission",
      "%prefix%<red>You do not have permission to execute this command."));

    messagesConfig.getYaml().setComment("commands.incorrect-usage",
      "Message that is shown when someone uses a command incorrectly");
    commands.incorrectCommandUsage = formatString(messagesConfig.getString("commands.incorrect-usage",
      "%prefix%<red>Usage: /sonar %usage%"));

    messagesConfig.getYaml().setComment("commands.invalid-ip-address",
      "Message that is shown when someone provides an invalid IP address (Invalid format)");
    commands.incorrectIpAddress = formatString(messagesConfig.getString("commands.invalid-ip-address",
      "%prefix%The IP address you provided seems to be invalid."));

    messagesConfig.getYaml().setComment("commands.illegal-ip-address",
      "Message that is shown when someone provides an illegal IP address (Local IP)");
    commands.illegalIpAddress = formatString(messagesConfig.getString("commands.illegal-ip-address",
      "%prefix%The IP address you provided seems to be either a local or loopback IP."));

    messagesConfig.getYaml().setComment("commands.player-only",
      "Message that is shown when the console runs a command that is player-only");
    commands.playersOnly = formatString(messagesConfig.getString("commands.player-only",
      "%prefix%<red>You can only execute this command as a player."));

    messagesConfig.getYaml().setComment("commands.console-only",
      "Message that is shown when a player runs a command that is console-only");
    commands.consoleOnly = formatString(messagesConfig.getString("commands.console-only",
      "%prefix%<red>For security reasons, you can only execute this command through console."));

    messagesConfig.getYaml().setComment("commands.cool-down",
      "Message that is shown when a player executes Sonar commands too quickly");
    commands.commandCoolDown = formatString(messagesConfig.getString("commands.cool-down",
      "%prefix%<red>You can only execute this command every 0.5 seconds."));
    commands.commandCoolDownLeft = formatString(messagesConfig.getString("commands.cool-down-left",
      "%prefix%<red>Please wait another <bold>%time-left%s<reset><red>."));

    messagesConfig.getYaml().setComment("commands.subcommand-no-permission",
      "Message that is shown when a player does not have permission to execute a certain subcommand");
    commands.subCommandNoPerm = formatString(messagesConfig.getString("commands.subcommand-no-permission",
      "%prefix%<red>You do not have permission to execute this subcommand. <gray>(%permission%)"));

    messagesConfig.getYaml().setComment("commands.main",
      "Translations for '/sonar'");
    messagesConfig.getYaml().setComment("commands.main.header",
      "Informational message that is shown above everything when running the main command");
    commands.helpHeader = messagesConfig.getStringList("commands.main.header",
      Arrays.asList(
        "<yellow>Running Sonar %version% on %platform%.",
        "<yellow>(C) %copyright_year% Jones Development and Sonar Contributors",
        "<green><click:open_url:'https://github.com/jonesdevelopment/sonar'>https://github.com/jonesdevelopment/sonar",
        "",
        "<yellow>Need help or have any questions?",
        "<yellow><click:open_url:'https://jonesdev.xyz/discord/'><hover:show_text:'(Click to open Discord)'>Open a " +
          "ticket on the Discord </hover></click></yellow><yellow><click:open_url:'https://github" +
          ".com/jonesdevelopment/sonar/issues'><hover:show_text:'(Click to open GitHub)'>or open a new issue on " +
          "GitHub.",
        ""
      ));
    messagesConfig.getYaml().setComment("commands.main.subcommands",
      "Formatting of the list of subcommands shown when running the main command");
    commands.helpSubcommands = formatString(messagesConfig.getString("commands.main.subcommands",
      "<click:suggest_command:'/sonar %subcommand% '><hover:show_text:'<gray>Only players: " +
        "</gray>%only_players%<br><gray>Require console: </gray>%require_console%<br><gray>Permission: " +
        "</gray><white>%permission%<br><gray>Aliases: </gray>%aliases%'><gray> ▪ </gray><green>/sonar " +
        "%subcommand%</green><gray> - </gray><white>%description%"));

    SonarCommand.prepareCachedMessages();

    messagesConfig.getYaml().setComment("commands.reload",
      "Translations for '/sonar reload'");
    messagesConfig.getYaml().setComment("commands.reload.start",
      "Message that is shown when someone starts reloading Sonar");
    commands.reloading = formatString(messagesConfig.getString("commands.reload.start",
      "%prefix%Reloading Sonar..."));

    messagesConfig.getYaml().setComment("commands.reload.finish",
      "Message that is shown when Sonar has finished reloading");
    commands.reloaded = formatString(messagesConfig.getString("commands.reload.finish",
      "%prefix%<green>Successfully reloaded <gray>(%taken%ms)"));

    messagesConfig.getYaml().setComment("commands.verbose",
      "Translations for '/sonar verbose'");
    messagesConfig.getYaml().setComment("commands.verbose.subscribed",
      "Message that is shown when a player subscribes to Sonar verbose");
    commands.verboseSubscribed = formatString(messagesConfig.getString("commands.verbose.subscribed",
      "%prefix%You are now viewing Sonar verbose."));

    messagesConfig.getYaml().setComment("commands.verbose.unsubscribed",
      "Message that is shown when a player unsubscribes from Sonar verbose");
    commands.verboseUnsubscribed = formatString(messagesConfig.getString("commands.verbose.unsubscribed",
      "%prefix%You are no longer viewing Sonar verbose."));

    messagesConfig.getYaml().setComment("commands.blacklist",
      "Translations for '/sonar blacklist'");
    messagesConfig.getYaml().setComment("commands.blacklist.empty",
      "Message that is shown when someone tries clearing the blacklist but is is empty");
    commands.blacklistEmpty = formatString(messagesConfig.getString("commands.blacklist.empty",
      "%prefix%The blacklist is currently empty. Therefore, no IP addresses were removed from the blacklist."));

    messagesConfig.getYaml().setComment("commands.blacklist.cleared",
      "Message that is shown when someone clears the blacklist");
    commands.blacklistCleared = formatString(messagesConfig.getString("commands.blacklist.cleared",
      "%prefix%You successfully removed a total of %removed% IP address(es) from the blacklist."));

    messagesConfig.getYaml().setComment("commands.blacklist.size",
      "Message that is shown when someone checks the size of the blacklist");
    commands.blacklistSize = formatString(messagesConfig.getString("commands.blacklist.size",
      "%prefix%The blacklist currently contains %amount% IP address(es)."));

    messagesConfig.getYaml().setComment("commands.blacklist.added",
      "Message that is shown when someone adds an IP address to the blacklist");
    commands.blacklistAdd = formatString(messagesConfig.getString("commands.blacklist.added",
      "%prefix%Successfully added %ip% to the blacklist."));

    messagesConfig.getYaml().setComment("commands.blacklist.added-warning",
      "Message that is shown when someone adds an IP address to the blacklist that is verified");
    commands.blacklistAddWarning = formatString(messagesConfig.getString("commands.blacklist.added-warning",
      "%prefix%<red>Warning: <white>%ip% is currently whitelisted. " +
        "Consider removing the IP address from the list of verified players to avoid potential issues."));

    messagesConfig.getYaml().setComment("commands.blacklist.removed",
      "Message that is shown when someone removes an IP address from the blacklist");
    commands.blacklistRemove = formatString(messagesConfig.getString("commands.blacklist.removed",
      "%prefix%Successfully removed %ip% from the blacklist."));

    messagesConfig.getYaml().setComment("commands.blacklist.duplicate-ip",
      "Message that is shown when someone adds an IP address to the blacklist but it is already blacklisted");
    commands.blacklistDuplicate = formatString(messagesConfig.getString("commands.blacklist.duplicate-ip",
      "%prefix%The IP address you provided is already blacklisted."));

    messagesConfig.getYaml().setComment("commands.blacklist.ip-not-found",
      "Message that is shown when someone removes an IP address from the blacklist but it is not blacklisted");
    commands.blacklistNotFound = formatString(messagesConfig.getString("commands.blacklist.ip-not-found",
      "%prefix%The IP address you provided is not blacklisted."));

    messagesConfig.getYaml().setComment("commands.verified",
      "Translations for '/sonar verified'");
    messagesConfig.getYaml().setComment("commands.verified.empty",
      "Message that is shown when someone tries clearing the list of verified players but is is empty");
    commands.verifiedEmpty = formatString(messagesConfig.getString("commands.verified.empty",
      "%prefix%The list of verified players is currently empty. Therefore, no players were unverified."));

    messagesConfig.getYaml().setComment("commands.verified.cleared",
      "Message that is shown when someone clears the list of verified players");
    commands.verifiedCleared = formatString(messagesConfig.getString("commands.verified.cleared",
      "%prefix%You successfully unverified a total of %removed% unique player(s)."));

    messagesConfig.getYaml().setComment("commands.verified.size",
      "Message that is shown when someone checks the size of the list of verified players");
    commands.verifiedSize = formatString(messagesConfig.getString("commands.verified.size",
      "%prefix%There are currently %amount% unique player(s) verified."));

    messagesConfig.getYaml().setComment("commands.verified.removed",
      "Message that is shown when someone un-verifies an IP address");
    commands.verifiedRemove = formatString(messagesConfig.getString("commands.verified.removed",
      "%prefix%Successfully unverified %ip%."));

    messagesConfig.getYaml().setComment("commands.verified.ip-not-found",
      "Message that is shown when someone un-verifies an IP address but it is not verified");
    commands.verifiedNotFound = formatString(messagesConfig.getString("commands.verified.ip-not-found",
      "%prefix%The IP address you provided is not verified."));

    messagesConfig.getYaml().setComment("commands.verified.blocked",
      "Message that is shown when someone tries un-verifying the same IP address twice (double operation)");
    commands.verifiedBlocked = formatString(messagesConfig.getString("commands.verified.blocked",
      "%prefix%Please wait for the current operation to finish."));

    messagesConfig.getYaml().setComment("commands.statistics",
      "Translations for '/sonar statistics'");
    messagesConfig.getYaml().setComment("commands.statistics.header",
      "Informational message that is shown above everything when viewing the statistics");
    commands.statisticsHeader = formatString(messagesConfig.getString("commands.statistics.header",
      "%prefix%<yellow>Showing %type% statistics for this session:"));

    messagesConfig.getYaml().setComment("commands.statistics.unknown-type",
      "Message that is shown when a player tries viewing an unknown statistic");
    commands.unknownStatisticType = formatString(messagesConfig.getString("commands.statistics.unknown-type",
      "%prefix%<red>Unknown statistics type! Available statistics: <gray>%statistics%"));

    messagesConfig.getYaml().setComment("commands.statistics.general",
      "Format of the general statistics message");
    commands.generalStatistics = formatString(fromList(messagesConfig.getStringList("commands.statistics.general",
      Arrays.asList(
        " <gray>▪ <green>Verified IP addresses: <white>%verified%",
        " <gray>▪ <green>Verifying IP addresses: <white>%verifying%",
        " <gray>▪ <green>Blacklisted IP addresses: <white>%blacklisted%",
        " <gray>▪ <green>Currently queued logins: <white>%queued%",
        " <gray>▪ <green>Total non-unique joins: <white>%total_joins%",
        " <gray>▪ <green>Total verification attempts: <white>%total_attempts%",
        " <gray>▪ <green>Total failed verifications: <white>%total_failed%"
      ))));

    messagesConfig.getYaml().setComment("commands.statistics.cpu",
      "Format of the CPU statistics message");
    commands.cpuStatistics = formatString(fromList(messagesConfig.getStringList("commands.statistics.cpu",
      Arrays.asList(
        " <gray>▪ <green>Process CPU usage right now: <white>%process_cpu%%",
        " <gray>▪ <green>System CPU usage right now: <white>%system_cpu%%",
        " <gray>▪ <green>Per-core process CPU usage: <white>%average_process_cpu%%",
        " <gray>▪ <green>Per-core system CPU usage: <white>%average_system_cpu%%",
        " <gray>▪ <green>General system load average: <white>%load_average%%",
        " <gray>▪ <green>Total amount of virtual cpus: <white>%virtual_cores%"
      ))));

    messagesConfig.getYaml().setComment("commands.statistics.memory",
      "Format of the memory statistics message");
    commands.memoryStatistics = formatString(fromList(messagesConfig.getStringList("commands.statistics.memory",
      Arrays.asList(
        " <gray>▪ <green>Total free memory: <white>%free_memory%",
        " <gray>▪ <green>Total used memory: <white>%used_memory%",
        " <gray>▪ <green>Total maximum memory: <white>%max_memory%",
        " <gray>▪ <green>Total allocated memory: <white>%total_memory%"
      ))));

    messagesConfig.getYaml().setComment("commands.statistics.network",
      "Format of the network statistics message");
    commands.networkStatistics = formatString(fromList(messagesConfig.getStringList("commands.statistics.network",
      Arrays.asList(
        " <gray>▪ <green>Current incoming used bandwidth: <white>%incoming%",
        " <gray>▪ <green>Current outgoing used bandwidth: <white>%outgoing%",
        " <gray>▪ <green>Total incoming used bandwidth: <white>%ttl_incoming%",
        " <gray>▪ <green>Total outgoing used bandwidth: <white>%ttl_outgoing%"
      ))));

    messagesConfig.getYaml().setComment("verification",
      "Translations for all messages during the verification process");
    messagesConfig.getYaml().setComment("verification.logs.connection",
      "Message that is logged to console whenever a new player joins the server");
    verification.connectLog = formatString(messagesConfig.getString("verification.logs.connection",
      "%name%%ip% (%protocol%) has connected."));

    messagesConfig.getYaml().setComment("verification.logs",
      "Translations for all debug messages during the verification");
    messagesConfig.getYaml().setComment("verification.logs.failed",
      "Message that is logged to console whenever a player fails verification");
    verification.failedLog = formatString(messagesConfig.getString("verification.logs.failed",
      "%ip% (%protocol%) has failed the bot check for: %reason%"));

    messagesConfig.getYaml().setComment("verification.logs.blacklisted",
      "Message that is logged to console whenever a player is blacklisted");
    verification.blacklistLog = formatString(messagesConfig.getString("verification.logs.blacklisted",
      "%ip% (%protocol%) was blacklisted for too many failed attempts"));

    messagesConfig.getYaml().setComment("verification.logs.successful",
      "Message that is logged to console whenever a player is verified");
    verification.successLog = formatString(messagesConfig.getString("verification.logs.successful",
      "%name% has been verified successfully (%time%s!)."));

    messagesConfig.getYaml().setComment("verification.welcome",
      "Message that is shown to the player when they are being checked for valid gravity");
    verification.gravity.youAreBeingChecked = deserialize(formatString(messagesConfig.getString("verification.welcome",
      "%prefix%<gray>Please wait a moment for the verification to finish...")));

    messagesConfig.getYaml().setComment("verification.captcha.enter-code",
      "Message that is shown to the player when they have to enter the answer to the captcha");
    verification.map.enterCode = deserialize(formatString(messagesConfig.getString("verification.captcha.enter-code",
      "%prefix%<green>Please enter the code in chat that is displayed on the map.")));
    messagesConfig.getYaml().setComment("verification.captcha.action-bar",
      "Timer that is shown to the player when they have to enter the answer to the captcha"
        + LINE_SEPARATOR + "(Set this to '' to disable the action bar message)");
    verification.map.enterCodeActionBar = formatString(messagesConfig.getString("verification.captcha.action-bar",
      "%prefix%<green>You have %time-left% seconds left to enter the code in chat"));
    messagesConfig.getYaml().setComment("verification.captcha.incorrect",
      "Message that is shown to the player when they enter the wrong answer in chat");
    verification.map.failedCaptcha = deserialize(formatString(messagesConfig.getString("verification.captcha.incorrect",
      "%prefix%<red>You have entered the wrong code. Please try again.")));

    messagesConfig.getYaml().setComment("verification.currently-preparing",
      "Disconnect message that is shown when someone joins while the captcha hasn't been prepared yet");
    verification.currentlyPreparing = deserialize(fromList(messagesConfig.getStringList(
      "verification.currently-preparing",
      Arrays.asList(
        "%header%",
        "<yellow>Your anti-bot data has not been prepared yet.",
        "<gray>Please wait a few seconds before trying to verify again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.too-many-players",
      "Disconnect message that is shown when too many players are verifying at the same time");
    verification.tooManyPlayers = deserialize(fromList(messagesConfig.getStringList("verification.too-many-players",
      Arrays.asList(
        "%header%",
        "<gold>Too many players are currently trying to log in, try again later.",
        "<gray>Please wait a few seconds before trying to join again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.too-fast-reconnect",
      "Disconnect message that is shown when someone rejoins too fast during verification");
    verification.tooFastReconnect = deserialize(fromList(messagesConfig.getStringList("verification.too-fast-reconnect",
      Arrays.asList(
        "%header%",
        "<gold>You reconnected too fast, try again later.",
        "<gray>Please wait a few seconds before trying to verify again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.already-verifying",
      "Disconnect message that is shown when someone joins but is already verifying");
    verification.alreadyVerifying = deserialize(fromList(messagesConfig.getStringList("verification.already-verifying",
      Arrays.asList(
        "%header%",
        "<red>Your IP address is currently being verified.",
        "<red>Please wait a few seconds before trying to verify again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.already-queued",
      "Disconnect message that is shown when someone joins but is already queued for verification");
    verification.alreadyQueued = deserialize(fromList(messagesConfig.getStringList("verification.already-queued",
      Arrays.asList(
        "%header%",
        "<red>Your IP address is currently queued for verification.",
        "<red>Please wait a few minutes before trying to verify again.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.blacklisted",
      "Disconnect message that is shown when someone joins but is temporarily blacklisted");
    verification.blacklisted = deserialize(fromList(messagesConfig.getStringList("verification.blacklisted",
      Arrays.asList(
        "%header%",
        "<red>You are currently denied from entering the server.",
        "<red>Please wait a few minutes to be able to join the server again.",
        "<gold>False positive? <gray>%support-url%",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.invalid-username",
      "Disconnect message that is shown when someone joins with an invalid username");
    verification.invalidUsername = deserialize(fromList(messagesConfig.getStringList("verification.invalid-username",
      Arrays.asList(
        "%header%",
        "<red>Your username contains invalid characters.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.invalid-protocol",
      "Disconnect message that is shown when someone joins with a too new or too old version");
    verification.invalidProtocol = deserialize(fromList(messagesConfig.getStringList("verification.invalid-protocol",
      Arrays.asList(
        "%header%",
        "<red>Your protocol version is currently unsupported.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.blacklisted-protocol",
      "Disconnect message that is shown when someone joins with a blacklisted version");
    verification.protocolBlacklisted = deserialize(fromList(messagesConfig.getStringList("verification" +
        ".blacklisted-protocol",
      Arrays.asList(
        "%header%",
        "<red>You are using a version that is not allowed on our server.",
        "<gold>Need help logging in? <gray>%support-url%",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.already-online",
      "Disconnect message that is shown when someone tries verifying with an account that is online");
    verification.alreadyConnected = deserialize(fromList(messagesConfig.getStringList("verification.already-online",
      Arrays.asList(
        "%header%",
        "<red>There is someone already online with your account.",
        "<gray>Please wait a few seconds before trying to verify again.",
        "<gray>If this keeps occurring, try restarting your game or contact support.",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verification.success",
      "Disconnect message that is shown when someone verifies successfully");
    verification.verificationSuccess = deserialize(fromList(messagesConfig.getStringList("verification.success",
      Arrays.asList(
        "%header%",
        "<green>You have successfully passed the verification.",
        "<white>You are now able to play on the server when you reconnect."
      ))));

    messagesConfig.getYaml().setComment("verification.failed",
      "Disconnect message that is shown when someone fails verification");
    verification.verificationFailed = deserialize(fromList(messagesConfig.getStringList("verification.failed",
      Arrays.asList(
        "%header%",
        "<red>You have failed the verification.",
        "<gray>Please wait a few seconds before trying to verify again.",
        "<gold>Need help? <gray>%support-url%",
        "%footer%"
      ))));

    messagesConfig.getYaml().setComment("verbose",
      "Translations for all messages regarding Sonar's verbose output");
    messagesConfig.getYaml().setComment("verbose.layout.normal",
      "General layout for the verbose action-bar"
        + LINE_SEPARATOR + "Placeholders:"
        + LINE_SEPARATOR + "- '%queued%' Number of queued connections"
        + LINE_SEPARATOR + "- '%verifying%' Number of verifying connections"
        + LINE_SEPARATOR + "- '%blacklisted%' Number of blacklisted IP addresses"
        + LINE_SEPARATOR + "- '%total-joins%' Number of total attempted joins"
        + LINE_SEPARATOR + "- '%verify-total%' Number of total verification attempts"
        + LINE_SEPARATOR + "- '%verify-success%' Number of verified IP addresses"
        + LINE_SEPARATOR + "- '%verify-failed%' Number of failed verifications"
        + LINE_SEPARATOR + "- '%logins-per-second%' Number of logins per second"
        + LINE_SEPARATOR + "- '%connections-per-second%' Number of connections per second"
        + LINE_SEPARATOR + "- '%attack-duration%' Duration of the current attack"
        + LINE_SEPARATOR + "- '%incoming-traffic%' Incoming bandwidth usage per second"
        + LINE_SEPARATOR + "- '%outgoing-traffic%' Outgoing bandwidth usage per second"
        + LINE_SEPARATOR + "- '%incoming-traffic-ttl%' Total incoming bandwidth usage"
        + LINE_SEPARATOR + "- '%outgoing-traffic-ttl%' Total outgoing bandwidth usage"
        + LINE_SEPARATOR + "- '%used-memory%' Amount of used memory of the process"
        + LINE_SEPARATOR + "- '%total-memory%' Amount of total memory of the process"
        + LINE_SEPARATOR + "- '%max-memory%' Amount of max memory of the process"
        + LINE_SEPARATOR + "- '%free-memory%' Amount of free memory of the process"
        + LINE_SEPARATOR + "- '%animation%' Customizable animated symbol"
        + LINE_SEPARATOR + "Translations for Sonar's normal verbose output");
    verbose.actionBarLayout = formatString(messagesConfig.getString("verbose.layout.normal",
      String.join(" <dark_aqua>╺ ", Arrays.asList(
        "%prefix%<gray>CPS <white>%connections-per-second%",
        "<gray>Queued <white>%queued%",
        "<gray>Verifying <white>%verifying%",
        "<gray>Blacklisted <white>%blacklisted%" +
          " <dark_aqua>| <green>⬆ <white>%outgoing-traffic%/s <red>⬇ <white>%incoming-traffic%/s" +
          "  <green><bold>%animation%<reset>"
      ))));
    messagesConfig.getYaml().setComment("verbose.layout.attack",
      "Translations for Sonar's verbose output during an active attack");
    verbose.actionBarLayoutDuringAttack = formatString(messagesConfig.getString("verbose.layout.attack",
      String.join(" <dark_aqua>╺ ", Arrays.asList(
        "%prefix%<gray>CPS <white>%connections-per-second%",
        "<gray>Logins/s <white>%logins-per-second%",
        "<gray>Queued <white>%queued%",
        "<gray>Verifying <white>%verifying%",
        "<gray>Blacklisted <white>%blacklisted%",
        "<gray>Duration <white>%attack-duration%" +
          " <dark_aqua>| <green>⬆ <white>%outgoing-traffic%/s <red>⬇ <white>%incoming-traffic%/s" +
          "  <green><bold>%animation%<reset>"
      ))));
    messagesConfig.getYaml().setComment("verbose.animation", "Animation for the action bar"
      + LINE_SEPARATOR + "Alternatives:"
      + LINE_SEPARATOR + "- ▙, ▛, ▜, ▟"
      + LINE_SEPARATOR + "- ⬈, ⬊, ⬋, ⬉");
    verbose.animation = Collections.unmodifiableList(messagesConfig.getStringList("verbose.animation",
      Arrays.asList("◜", "◝", "◞", "◟")
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
    return fromList(list, "<newline>");
  }

  private @NotNull String fromList(final @NotNull Collection<String> list, final String newline) {
    return formatString(String.join(newline, list));
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
}
