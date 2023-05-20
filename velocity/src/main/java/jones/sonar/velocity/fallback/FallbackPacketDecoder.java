/*
 * Copyright (C) 2023, jones
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

package jones.sonar.velocity.fallback;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import jones.sonar.api.fallback.FallbackConnection;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;

@RequiredArgsConstructor
public final class FallbackPacketDecoder extends ChannelInboundHandlerAdapter {
    private final FallbackConnection<ConnectedPlayer, MinecraftConnection> fallbackPlayer;
    private final long startKeepAliveId;

    // TODO: make configurable
    private static final Component VERIFIED = Component.text("§e§lSonar §7» §aYou were successfully verified. §7Please reconnect to the server.");

    private boolean hasSentClientBrand, hasSentClientSettings, hasSentKeepAlive;

    @Override
    public void channelRead(final ChannelHandlerContext ctx,
                            final Object msg) throws Exception {
        if (msg instanceof MinecraftPacket packet) {
            final boolean legalPacket = packet instanceof ClientSettings
                    || packet instanceof PluginMessage || packet instanceof KeepAlive;

            checkFrame(legalPacket, "unexpected packet: " + packet.getClass().getSimpleName());

            if (packet instanceof ClientSettings && !hasSentClientSettings) {
                checkFrame(hasSentKeepAlive, "unexpected timing #1");
                checkFrame(!hasSentClientBrand, "unexpected timing #2");

                hasSentClientSettings = true;
            }

            if (packet instanceof PluginMessage payload) {
                checkFrame(hasSentKeepAlive, "unexpected timing #3");

                if (!payload.getChannel().equals("MC|Brand") && !payload.getChannel().equals("minecraft:brand")) return;

                val valid = fallbackPlayer.getProtocolVersion() >= ProtocolVersion.MINECRAFT_1_13.getProtocol();

                // MCStorm actually messes this up
                checkFrame(payload.getChannel().equals("MC|Brand") || valid, "invalid client brand");
                checkFrame(!hasSentClientBrand, "duplicate client brand");
                checkFrame(hasSentClientSettings, "unexpected timing #4");

                hasSentClientBrand = true;

                if (fallbackPlayer.getProtocolVersion() == ProtocolVersion.MINECRAFT_1_8.getProtocol()) return;

                finish(fallbackPlayer);
            }

            if (packet instanceof KeepAlive keepAlive
                    && keepAlive.getRandomId() == startKeepAliveId) {
                checkFrame(!hasSentKeepAlive, "duplicate keep alive");

                hasSentKeepAlive = true;

                fallbackPlayer.getConnection().write(getForVersion(fallbackPlayer.getProtocolVersion()));
            }

            // 1.8 clients send a KeepAlive packet with the id 0 every second
            // while being in the "Downloading terrain" gui
            if (packet instanceof KeepAlive keepAlive
                    && keepAlive.getRandomId() == 0
                    && fallbackPlayer.getProtocolVersion() == ProtocolVersion.MINECRAFT_1_8.getProtocol()) {

                // First, let's validate if the packet could actually be sent at this point
                checkFrame(hasSentKeepAlive, "unexpected keep alive (1.8)");
                checkFrame(hasSentClientBrand, "unexpected timing #5");
                checkFrame(hasSentClientSettings, "unexpected timing #6");

                // We already ran the other checks, let's verify the player
                finish(fallbackPlayer);
            }
        }
    }

    private static void finish(final FallbackConnection<ConnectedPlayer, MinecraftConnection> fallbackPlayer) {
        fallbackPlayer.getFallback().getVerified().add(fallbackPlayer.getInetAddress());

        var kickPacket = Disconnect.create(VERIFIED, fallbackPlayer.getPlayer().getProtocolVersion());

        fallbackPlayer.getConnection().closeWith(kickPacket);
    }

    private static JoinGame getForVersion(final int protocolVersion) {
        if (protocolVersion >= ProtocolVersion.MINECRAFT_1_19_4.getProtocol()) {
            return FallbackPackets.JOIN_GAME_1_19_4;
        } else if (protocolVersion >= ProtocolVersion.MINECRAFT_1_19_1.getProtocol()) {
            return FallbackPackets.JOIN_GAME_1_19_1;
        } else if (protocolVersion >= ProtocolVersion.MINECRAFT_1_18_2.getProtocol()) {
            return FallbackPackets.JOIN_GAME_1_18_2;
        } else if (protocolVersion >= ProtocolVersion.MINECRAFT_1_16_2.getProtocol()) {
            return FallbackPackets.JOIN_GAME_1_16_2;
        }
        return FallbackPackets.LEGACY_JOIN_GAME;
    }

    private static final CorruptedFrameException CORRUPTED_FRAME = new CorruptedFrameException();

    private void checkFrame(final boolean condition, final String message) {
        if (!condition) {
            fallbackPlayer.fail(message);
            throw CORRUPTED_FRAME;
        }
    }
}
