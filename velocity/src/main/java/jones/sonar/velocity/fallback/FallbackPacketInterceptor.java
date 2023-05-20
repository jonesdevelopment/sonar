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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import jones.sonar.api.Sonar;
import jones.sonar.api.fallback.FallbackConnection;
import jones.sonar.velocity.SonarVelocity;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.concurrent.ThreadLocalRandom;

import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;

// TODO: small fall check
@RequiredArgsConstructor
public final class FallbackPacketInterceptor extends ChannelInboundHandlerAdapter {
    private final FallbackConnection<ConnectedPlayer, MinecraftConnection> fallbackPlayer;
    private State state = State.SETTINGS;
    private enum State {
        SETTINGS,
        BRAND,
        KEEP_ALIVE,
        FINISHED
    }
    private long keepAliveId;

    // TODO: make configurable
    private static final Component SUCCESSFULLY_VERIFIED = Component.text(
            "§aYou have been verified. Please reconnect."
    );

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof ClientSettings) {
            checkFrame(state == State.SETTINGS, "Did not expect " + state);

            state = State.BRAND;
        }

        if (msg instanceof PluginMessage pluginMessage) {
            if (pluginMessage.getChannel().equals("MC|Brand")
                    || pluginMessage.getChannel().equals("minecraft:brand")) {
                checkFrame(state == State.BRAND, "Did not expect " + state);

                keepAliveId = ThreadLocalRandom.current().nextInt();
                state = State.KEEP_ALIVE;

                var keepAlive = new KeepAlive();
                keepAlive.setRandomId(keepAliveId);
                fallbackPlayer.getConnection().write(keepAlive);
            }
        }

        // TODO: check for 1.9+
        if (msg instanceof KeepAlive keepAlive && state != State.FINISHED) {
            checkFrame(state == State.KEEP_ALIVE, "Did not expect " + state);
            checkFrame(keepAlive.getRandomId() == keepAliveId, "Invalid KeepAlive id");

            if (keepAliveId == 0L
                    || fallbackPlayer.getProtocolVersion() >= ProtocolVersion.MINECRAFT_1_8.getProtocol()) {
                state = State.FINISHED;

                SonarVelocity.INSTANCE.getPlugin().getLogger().info("[Fallback] Verified: {} ({})",
                        fallbackPlayer.getPlayer().getUsername(), fallbackPlayer.getProtocolVersion());

                Sonar.get().getFallback().getVerified().add(fallbackPlayer.getInetAddress());

                fallbackPlayer.getPlayer().disconnect0(SUCCESSFULLY_VERIFIED, true);
                return;
            }

            keepAliveId = 0L;
        }
    }
}
