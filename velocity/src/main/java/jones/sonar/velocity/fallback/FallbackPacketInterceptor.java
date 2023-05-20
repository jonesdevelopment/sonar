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

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import jones.sonar.api.fallback.FallbackConnection;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;

import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;

// TODO: small fall check
@AllArgsConstructor
public final class FallbackPacketInterceptor extends ChannelInboundHandlerAdapter {
    private final FallbackConnection<ConnectedPlayer, MinecraftConnection> fallbackPlayer;
    private long keepAliveId;

    // TODO: make configurable
    private static final Component SUCCESSFULLY_VERIFIED = Component.text(
            "§aYou have been verified. Please reconnect."
    );

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof ClientSettings) {
        }

        if (msg instanceof PluginMessage pluginMessage) {
            if (pluginMessage.getChannel().equals("MC|Brand")
                    || pluginMessage.getChannel().equals("minecraft:brand")) {
            }
        }

        if (msg instanceof KeepAlive keepAlive) {
            checkFrame(keepAlive.getRandomId() == keepAliveId, "Invalid KeepAlive id");

            System.out.println("[client → server] " + fallbackPlayer.getPlayer().getUsername() + " - KeepAlive → " + keepAlive.getRandomId());

            fallbackPlayer.getFallback().getVerified().add(fallbackPlayer.getInetAddress());

            fallbackPlayer.getPlayer().disconnect0(SUCCESSFULLY_VERIFIED, true);

            keepAliveId = 0L;
        }
    }
}
