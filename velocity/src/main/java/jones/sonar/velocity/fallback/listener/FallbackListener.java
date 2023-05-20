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

package jones.sonar.velocity.fallback.listener;

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
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import jones.sonar.api.fallback.Fallback;
import jones.sonar.api.fallback.FallbackConnection;
import jones.sonar.velocity.fallback.dummy.DummyConnection;
import jones.sonar.velocity.fallback.limit.FallbackLimiter;
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
    private final Collection<String> premium = new Vector<>();

    private static final Component ALREADY_VERIFYING = Component.text(
            "§e§lSonar Fallback\n\n§7You are already being verified at the moment! Please wait."
    );
    private static final Component TOO_MANY_VERIFICATIONS = Component.text(
            "§e§lSonar Fallback\n\n§7Please wait a minute before trying to verify again."
    );

    private static final DummyConnection CLOSED_MINECRAFT_CONNECTION;

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodHandle INITIAL_CONNECTION;
    private static final MethodHandle CONNECTED_PLAYER;
    private static final Field CONNECTION_FIELD;

    // https://github.com/Elytrium/LimboAPI/blob/ca6eb7155740bf3ff32596412a48e537fe55606d/plugin/src/main/java/net/elytrium/limboapi/injection/login/LoginListener.java#L239
    static {
        CLOSED_MINECRAFT_CONNECTION = new DummyConnection(null);

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
        } catch (Exception exception) {
            throw new IllegalStateException();
        }
    }

    /**
     * If we don't handle online/offline mode players correctly,
     * many plugins (especially Auth-related) will have issues
     *
     * @param event PreLoginEvent
     */
    @Subscribe
    public void handle(final PreLoginEvent event) {
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
    @Subscribe
    public void handle(final GameProfileRequestEvent event) throws Throwable {
        var inboundConnection = (LoginInboundConnection) event.getConnection();
        var initialConnection = (InitialInboundConnection) INITIAL_CONNECTION.invokeExact(inboundConnection);

        var mcConnection = initialConnection.getConnection();
        var channel = mcConnection.getChannel();

        CONNECTION_FIELD.set(mcConnection.getSessionHandler(), CLOSED_MINECRAFT_CONNECTION);

        channel.eventLoop().execute(() -> {
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

                // Create an instance for the Fallback connection
                val fallbackPlayer = new FallbackConnection(
                        player.getUsername(),
                        channel,
                        channel.pipeline(),
                        player.getProtocolVersion().getProtocol()
                    );

                // Check if Fallback is already verifying a player
                if (!Fallback.connection(fallbackPlayer)) {
                    player.disconnect0(ALREADY_VERIFYING, true);
                    return;
                }

                // Check if the ip address had too many verifications
                if (FallbackLimiter.shouldDeny(fallbackPlayer)) {
                    player.disconnect0(TOO_MANY_VERIFICATIONS, true);
                    return;
                }

                logger.info("[Fallback] Initialized connection for: {} ({})", fallbackPlayer.getUsername(), fallbackPlayer.getProtocolVersion());

                // Check if the player is already connected to the proxy
                if (!server.canRegisterConnection(player)) {
                    player.disconnect0(Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED), true);
                    return;
                }

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
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }
}
