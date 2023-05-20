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

import com.google.common.collect.ImmutableSet;
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
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import jones.sonar.api.Sonar;
import jones.sonar.api.fallback.FallbackConnection;
import jones.sonar.common.fallback.FallbackChannelHandler;
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

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;

@RequiredArgsConstructor
public final class FallbackListener {
    private final VelocityServer server;
    private final Logger logger;

    // We need to cache if the joining player is a premium player or not
    // If we don't do that, many authentication plugins can potentially break
    private final Collection<String> premium = new Vector<>();

    // TODO: make configurable
    private static final Component ALREADY_VERIFYING = Component.text(
            "§cYou are already being verified at the moment! Please try again later."
    );
    // TODO: make configurable
    private static final Component TOO_MANY_VERIFICATIONS = Component.text(
            "§cPlease wait a minute before trying to verify again."
    );

    private static final String DIMENSION = "minecraft:the_end"; // TODO: make configurable
    private static final JoinGame JOIN_GAME = new JoinGame();

    // TODO: make work for 1.9+
    static {
        JOIN_GAME.setIsHardcore(true);
        JOIN_GAME.setLevelType("flat");
        JOIN_GAME.setGamemode((short) 3);
        JOIN_GAME.setReducedDebugInfo(true);

        JOIN_GAME.setDimensionInfo(new DimensionInfo(DIMENSION, DIMENSION, false, false));

        try {
            MethodHandles.privateLookupIn(JoinGame.class, MethodHandles.lookup())
                    .findSetter(JoinGame.class, "levelNames", ImmutableSet.class)
                    .invokeExact(JOIN_GAME, ImmutableSet.of(DIMENSION));
        } catch (Throwable throwable) {
            throw new IllegalStateException();
        }
    }

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

        if (Sonar.get().getFallback().getVerified().contains(inetAddress)) return;

        // Check if Fallback is already verifying a player
        // → is another player with the same ip address connected to Fallback?
        if (Sonar.get().getFallback().getConnected().contains(inetAddress)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(ALREADY_VERIFYING));
            return;
        }

        // Check if the ip address had too many verifications
        if (!Sonar.get().getFallback().getFilter().allow(inetAddress)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(TOO_MANY_VERIFICATIONS));
            return;
        }

        if (event.getResult().isForceOfflineMode()) return;
        if (!event.getResult().isOnlineModeAllowed() && !server.getConfiguration().isOnlineMode()) return;

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

        if (Sonar.get().getFallback().getVerified().contains(inetAddress)) return;

        val inboundConnection = (LoginInboundConnection) event.getConnection();
        val initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

        val mcConnection = initialConnection.getConnection();
        val channel = mcConnection.getChannel();

        CONNECTION_FIELD.set(mcConnection.getSessionHandler(), CLOSED_MINECRAFT_CONNECTION);

        Sonar.get().getFallback().getQueue().queue(() -> channel.eventLoop().execute(() -> {
            if (mcConnection.isClosed()) return;

            try {

                // Create an instance for player
                val player = (ConnectedPlayer) CONNECTED_PLAYER.invokeExact(
                        server,
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

                // Check if the player is already connected to the proxy
                if (!server.canRegisterConnection(player)) {
                    player.disconnect0(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true);
                    return;
                }

                // Create an instance for the Fallback connection
                // TODO: simpler alternative?
                val fallbackPlayer = new FallbackConnection<>(
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

                Sonar.get().getFallback().getConnected().add(inetAddress);

                // We have to add this pipeline to monitor whenever the client disconnects
                // to remove them from the list of connected players
                fallbackPlayer.getPipeline().addFirst("sonar-handler", FallbackChannelHandler.INSTANCE);
                // ==================================================================

                // Set compression
                val threshold = server.getConfiguration().getCompressionThreshold();

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

                // Set the status to PLAY, so we can receive and send more packets
                mcConnection.setAssociation(player);
                mcConnection.setState(StateRegistry.PLAY);

                // We have to add this pipeline to monitor all incoming traffic (packets)
                fallbackPlayer.getPipeline().addAfter(
                        Connections.MINECRAFT_DECODER,
                        "sonar-interceptor",
                        new FallbackPacketInterceptor(fallbackPlayer)
                );

                mcConnection.write(JOIN_GAME);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }));
    }
}
