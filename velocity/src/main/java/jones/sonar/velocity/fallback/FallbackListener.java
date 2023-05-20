/*
 *  Copyright (c) 2023, jones (https://jonesdev.xyz) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jones.sonar.velocity.fallback;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.api.fallback.FallbackConnection;
import jones.sonar.common.fallback.FallbackChannelHandler;
import jones.sonar.common.fallback.FallbackTimeoutHandler;
import jones.sonar.velocity.SonarVelocity;
import jones.sonar.velocity.fallback.dummy.DummyConnection;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;

@RequiredArgsConstructor
public final class FallbackListener {
    private final Logger logger;
    private final Fallback fallback;

    // We need to cache if the joining player is a premium player or not
    // If we don't do that, many authentication plugins can potentially break
    private final Collection<String> premium = new Vector<>();

    // TODO: make configurable
    private static final Component ALREADY_VERIFYING = Component.text(
            "§e§lSonar §7» §cYou are already being verified at the moment! Please try again later."
    );
    private static final PreLoginEvent.PreLoginComponentResult ALREADY_VERIFYING_RESULT = PreLoginEvent.PreLoginComponentResult.denied(ALREADY_VERIFYING);
    // TODO: make configurable
    private static final Component TOO_MANY_PLAYERS = Component.text(
            "§e§lSonar §7» §cToo many players are currently trying to log in. Please try again later."
    );
    private static final PreLoginEvent.PreLoginComponentResult TOO_MANY_PLAYERS_RESULT = PreLoginEvent.PreLoginComponentResult.denied(TOO_MANY_PLAYERS);
    // TODO: make configurable
    private static final Component TOO_MANY_VERIFICATIONS = Component.text(
            "§e§lSonar §7» §cPlease wait a minute before trying to verify again."
    );

    private static final DummyConnection CLOSED_MINECRAFT_CONNECTION;

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodHandle INITIAL_CONNECTION;
    private static final MethodHandle CONNECTED_PLAYER;
    private static final Field CONNECTION_FIELD;

    static {
        CLOSED_MINECRAFT_CONNECTION = new DummyConnection(null);

        // https://github.com/Elytrium/LimboAPI/blob/ca6eb7155740bf3ff32596412a48e537fe55606d/plugin/src/main/java/net/elytrium/limboapi/injection/login/LoginListener.java#L239
        try {
            CONNECTION_FIELD = AuthSessionHandler.class.getDeclaredField("mcConnection");
            CONNECTION_FIELD.setAccessible(true);

            CONNECTED_PLAYER = MethodHandles.privateLookupIn(ConnectedPlayer.class, MethodHandles.lookup())
                    .findConstructor(ConnectedPlayer.class,
                            MethodType.methodType(
                                    void.class,
                                    VelocityServer.class,
                                    GameProfile.class,
                                    MinecraftConnection.class,
                                    InetSocketAddress.class,
                                    boolean.class,
                                    IdentifiedKey.class
                            )
                    );

            INITIAL_CONNECTION = MethodHandles.privateLookupIn(LoginInboundConnection.class, LOOKUP)
                    .findGetter(LoginInboundConnection.class, "delegate", InitialInboundConnection.class);
        } catch (Throwable throwable) {
            throw new IllegalStateException();
        }
    }

    /**
     * If we don't handle online/offline mode players correctly,
     * many plugins (especially Auth-related) will have issues
     *
     * @param event PreLoginEvent
     */
    @Subscribe(order = PostOrder.LAST)
    public void handle(final PreLoginEvent event) {
        var inetAddress = event.getConnection().getRemoteAddress().getAddress();

        if (fallback.getVerified().contains(inetAddress)) return;

        // We cannot allow too many players on our Fallback server
        if (fallback.getQueue().getQueuedPlayers().size() > Short.MAX_VALUE) {
            event.setResult(TOO_MANY_PLAYERS_RESULT);
            return;
        }

        // Check if Fallback is already verifying a player
        // → is another player with the same ip address connected to Fallback?
        if (fallback.getConnected().contains(inetAddress)) {
            event.setResult(ALREADY_VERIFYING_RESULT);
            return;
        }

        if (event.getResult().isForceOfflineMode()) return;
        if (!SonarVelocity.INSTANCE.getPlugin().getServer().getConfiguration().isOnlineMode()
                && !event.getResult().isOnlineModeAllowed()) return;

        premium.add(event.getUsername());
    }

    /**
     * Handles inbound connections
     *
     * @param event GameProfileRequestEvent
     * @throws java.lang.Throwable Unexpected error
     */
    @Subscribe(order = PostOrder.LAST)
    public void handle(final GameProfileRequestEvent event) throws Throwable {
        val inetAddress = event.getConnection().getRemoteAddress().getAddress();

        if (fallback.getVerified().contains(inetAddress)) return;

        val inboundConnection = (LoginInboundConnection) event.getConnection();
        val initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

        val mcConnection = initialConnection.getConnection();
        val channel = mcConnection.getChannel();

        CONNECTION_FIELD.set(mcConnection.getSessionHandler(), CLOSED_MINECRAFT_CONNECTION);

        channel.eventLoop().execute(() -> {
            if (mcConnection.isClosed()) return;

            // Replace timeout handler to avoid issues
            channel.pipeline().replace(
                    Connections.READ_TIMEOUT,
                    Connections.READ_TIMEOUT,
                    new FallbackTimeoutHandler(5L, TimeUnit.SECONDS));

            fallback.getQueue().queue(() -> channel.eventLoop().execute(() -> {
                if (mcConnection.isClosed()) return;

                try {

                    // Create an instance for player
                    val player = (ConnectedPlayer) CONNECTED_PLAYER.invokeExact(
                            mcConnection.server,
                            event.getGameProfile(),
                            mcConnection,
                            inboundConnection.getVirtualHost().orElse(null),
                            premium.contains(event.getUsername()),
                            inboundConnection.getIdentifiedKey()
                    );

                    // Remove the player from the premium list in order to prevent memory leaks
                    // We cannot rely on the DisconnectEvent since the server will not call it
                    // -> we are intercepting the packets!
                    premium.remove(event.getUsername());

                    // Check if the ip address had too many verifications or is rejoining too quickly
                    if (!fallback.getAttemptLimiter().allow(inetAddress)) {
                        player.disconnect0(TOO_MANY_VERIFICATIONS, true);
                        return;
                    }

                    // Check if the player is already connected to the proxy
                    if (!mcConnection.server.canRegisterConnection(player)) {
                        player.disconnect0(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true);
                        return;
                    }

                    // Create an instance for the Fallback connection
                    // TODO: simpler alternative?
                    val fallbackPlayer = new FallbackConnection<>(
                            fallback,
                            player,
                            mcConnection,
                            channel,
                            channel.pipeline(),
                            inetAddress,
                            player.getProtocolVersion().getProtocol()
                    );

                    // ==================================================================
                    logger.info("[Fallback] Processing connection for: {} ({})",
                            event.getUsername(), fallbackPlayer.getProtocolVersion());

                    fallback.getConnected().add(inetAddress);

                    // We have to add this pipeline to monitor whenever the client disconnects
                    // to remove them from the list of connected players
                    fallbackPlayer.getPipeline().addFirst("sonar-handler", FallbackChannelHandler.INSTANCE);
                    // ==================================================================

                    // Set compression
                    val threshold = mcConnection.server.getConfiguration().getCompressionThreshold();

                    if (threshold >= 0 && mcConnection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
                        mcConnection.write(new SetCompression(threshold));
                        mcConnection.setCompressionThreshold(threshold);
                    }

                    // Send LoginSuccess packet to spoof our fake lobby
                    var success = new ServerLoginSuccess();

                    success.setUsername(player.getUsername());
                    success.setProperties(player.getGameProfileProperties());
                    success.setUuid(player.getUniqueId());

                    mcConnection.write(success);

                    // Set the state to a custom one, so we can receive and send more packets
                    mcConnection.setAssociation(player);
                    mcConnection.setState(StateRegistry.PLAY);

                    final KeepAlive keepAlive = new KeepAlive();
                    final long keepAliveId = ThreadLocalRandom.current().nextInt();

                    keepAlive.setRandomId(keepAliveId);

                    // We have to add this pipeline to monitor all incoming traffic
                    fallbackPlayer.getPipeline().addAfter(
                            Connections.MINECRAFT_DECODER,
                            "sonar-decoder",
                            new FallbackPacketDecoder(fallbackPlayer, keepAliveId)
                    );

                    mcConnection.write(keepAlive);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }));
        });
    }
}
