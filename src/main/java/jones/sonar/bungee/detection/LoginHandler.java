package jones.sonar.bungee.detection;

import jones.sonar.bungee.config.Config;
import jones.sonar.universal.config.options.CustomRegexOptions;
import jones.sonar.universal.data.connection.ConnectionData;
import jones.sonar.universal.data.player.PlayerData;
import jones.sonar.universal.data.player.manager.PlayerDataManager;
import jones.sonar.universal.detection.Detection;
import jones.sonar.universal.detection.Detections;
import jones.sonar.universal.platform.bungee.SonarBungee;
import jones.sonar.universal.queue.PlayerQueue;
import jones.sonar.universal.util.Sensibility;
import jones.sonar.universal.whitelist.Whitelist;
import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public final class LoginHandler implements Detections {
    public Detection check(final ConnectionData connectionData) throws Exception {
        final boolean underAttack = Sensibility.isUnderAttackJoins();

        if (connectionData.username.length() > Config.Values.MAX_NAME_LENGTH
                || !connectionData.username.matches(Config.Values.NAME_VALIDATION_REGEX)) {
            if ((Config.Values.REGEX_BLACKLIST_MODE == CustomRegexOptions.DURING_ATTACK && underAttack)
                    || Config.Values.REGEX_BLACKLIST_MODE == CustomRegexOptions.ALWAYS) {
                return BLACKLIST;
            }

            connectionData.checked = 0;
            return INVALID_NAME;
        }

        final long timeStamp = System.currentTimeMillis();

        if (connectionData.checked == 0) {
            connectionData.checked = 1;
            connectionData.verifiedName = connectionData.username;

            /*connectionData.lastJoin = timeStamp;
            return FIRST_JOIN_KICK;*/
        }

        if (connectionData.checked == 1) {
            connectionData.checked = 2;

            if (!Objects.equals(connectionData.verifiedName, connectionData.username)
                    && !connectionData.allowedNames.contains(connectionData.username)
                    && Config.Values.ENABLE_RECONNECT_CHECK) {
                return BLACKLIST;
            }
        }

        if (timeStamp - connectionData.lastJoin <= Config.Values.REJOIN_DELAY) {
            connectionData.checked = 2;
            connectionData.failedReconnect++;

            connectionData.lastJoin = (timeStamp - (Config.Values.REJOIN_DELAY / 2L));
            return TOO_FAST_RECONNECT;
        } else if (connectionData.botLevel > 0) {
            connectionData.botLevel--;
        }

        connectionData.lastJoin = timeStamp;

        if (!connectionData.verifiedNames.contains(connectionData.username)
                && !Objects.equals(connectionData.verifiedName, connectionData.username)) {
            connectionData.verifiedNames.add(connectionData.username);

            connectionData.botLevel++;
            //return FIRST_JOIN_KICK;
        }

        if (Config.Values.CUSTOM_REGEXES.stream().anyMatch(connectionData.username::matches)) {
            if ((Config.Values.REGEX_BLACKLIST_MODE == CustomRegexOptions.DURING_ATTACK && underAttack)
                    || Config.Values.REGEX_BLACKLIST_MODE == CustomRegexOptions.ALWAYS) {
                return BLACKLIST;
            }

            connectionData.botLevel++;

            if ((Config.Values.REGEX_CHECK_MODE == CustomRegexOptions.DURING_ATTACK && underAttack)
                    || Config.Values.REGEX_CHECK_MODE == CustomRegexOptions.ALWAYS) {
                connectionData.checked = 0;
                return INVALID_NAME;
            }
        }

        if (underAttack && !Whitelist.isWhitelisted(connectionData.inetAddress)) {
            if (connectionData.checked == 2) {
                connectionData.checked = 3;
                connectionData.underAttackChecks++;

                if (connectionData.failedReconnect > 2
                        && connectionData.underAttackChecks < connectionData.failedReconnect) {
                    connectionData.botLevel++;
                }
                return DURING_ATTACK;
            }

            PlayerQueue.addToQueue(connectionData.username);

            if (PlayerQueue.getPosition(connectionData.username) > 1) {
                return PLAYER_IN_QUEUE;
            }
        } else {
            connectionData.underAttackChecks = 0;
            connectionData.failedReconnect = 0;
        }

        final long online = connectionData.getAccountsOnlineWithSameIP();

        if (online > Config.Values.MAXIMUM_ONLINE_PER_IP) {
            connectionData.checked = 2;
            return TOO_MANY_ONLINE;
        }

        // strong intelligent bot detection
        if (connectionData.botLevel > 0) {
            if (connectionData.botLevel > 4) {
                connectionData.botLevel = 3;
                return SUSPICIOUS;
            }

            if (connectionData.failedReconnect < 3) {
                connectionData.botLevel--;
            }
        }

        connectionData.allowedNames.add(connectionData.username);

        final PlayerData playerData = PlayerDataManager.create(connectionData.username);

        // handle the login to reset all variables within the player data object
        playerData.handleLogin();

        // don't let bots reconnect
        if (timeStamp - playerData.lastDetection < Config.Values.REJOIN_DELAY * 2L) {
            connectionData.botLevel++;
            return SUSPICIOUS;
        }

        if (Config.Values.ENABLE_PROXY_CHECK && SonarBungee.INSTANCE.selectedAntiProxyProvider != null) {
            if (SonarBungee.INSTANCE.selectedAntiProxyProvider.isUsingProxy(connectionData.inetAddress)) {
                return VPN_OR_PROXY;
            }
        }
        return ALLOW;
    }
}
