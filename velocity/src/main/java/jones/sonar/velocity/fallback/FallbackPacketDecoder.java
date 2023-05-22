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

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jones.sonar.api.fallback.FallbackConnection;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.api.network.ProtocolVersion.*;
import static jones.sonar.api.fallback.FallbackPipelines.DECODER;
import static jones.sonar.api.fallback.FallbackPipelines.HANDLER;
import static jones.sonar.velocity.fallback.FallbackListener.CONNECTION_FIELD;

@RequiredArgsConstructor
public final class FallbackPacketDecoder extends ChannelInboundHandlerAdapter {
  private final @NotNull FallbackConnection<ConnectedPlayer, MinecraftConnection> player;
  private final long startKeepAliveId;

  private boolean hasSentClientBrand, hasSentClientSettings, hasSentKeepAlive;

  private static final MethodHandle CONNECT_TO_INITIAL_SERVER;
  private static final MethodHandle CONNECT_SESSION_HANDLER;
  private static final MethodHandle SET_PERMISSION_FUNCTION;
  private static final PermissionProvider DEFAULT_PERMISSION;

  static {
    try {
      CONNECT_TO_INITIAL_SERVER = MethodHandles.privateLookupIn(
        AuthSessionHandler.class, MethodHandles.lookup())
        .findVirtual(AuthSessionHandler.class,
          "connectToInitialServer",
          MethodType.methodType(CompletableFuture.class, ConnectedPlayer.class)
        );

      CONNECT_SESSION_HANDLER = MethodHandles.privateLookupIn(
          InitialConnectSessionHandler.class, MethodHandles.lookup())
        .findConstructor(InitialConnectSessionHandler.class,
          MethodType.methodType(void.class, ConnectedPlayer.class, VelocityServer.class)
        );

      final Field PERMISSIONS_FIELD = ConnectedPlayer.class.getDeclaredField("DEFAULT_PERMISSIONS");
      PERMISSIONS_FIELD.setAccessible(true);

      DEFAULT_PERMISSION = (PermissionProvider) PERMISSIONS_FIELD.get(PermissionProvider.class);

      SET_PERMISSION_FUNCTION = MethodHandles.privateLookupIn(
          AuthSessionHandler.class, MethodHandles.lookup())
        .findVirtual(ConnectedPlayer.class,
          "setPermissionFunction",
          MethodType.methodType(void.class, PermissionFunction.class)
        );
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  @Override
  public void channelRead(final @NotNull ChannelHandlerContext ctx,
                          final @NotNull Object msg) throws Exception {
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

        final boolean valid = player.getProtocolVersion() >= MINECRAFT_1_13.getProtocol();

        checkFrame(payload.getChannel().equals("MC|Brand") || valid, "invalid client brand");
        checkFrame(!hasSentClientBrand, "duplicate client brand");
        checkFrame(hasSentClientSettings, "unexpected timing #4");

        hasSentClientBrand = true;

        if (player.getProtocolVersion() == MINECRAFT_1_8.getProtocol()) return;

        finish();
      }

      if (packet instanceof KeepAlive keepAlive
        && keepAlive.getRandomId() == startKeepAliveId) {
        checkFrame(!hasSentKeepAlive, "duplicate keep alive");

        hasSentKeepAlive = true;

        player.getConnection().write(getForVersion(player.getProtocolVersion()));
      }

      // 1.8 clients send a KeepAlive packet with the id 0 every second
      // while being in the "Downloading terrain" gui
      if (packet instanceof KeepAlive keepAlive
        && keepAlive.getRandomId() == 0
        && player.getProtocolVersion() == MINECRAFT_1_8.getProtocol()) {

        // First, let's validate if the packet could actually be sent at this point
        checkFrame(hasSentKeepAlive, "unexpected keep alive (1.8)");
        checkFrame(hasSentClientBrand, "unexpected timing #5");
        checkFrame(hasSentClientSettings, "unexpected timing #6");

        // We already ran the other checks, let's verify the player
        finish();
      }
    } else {
      // We want the backend server to actually receive the packets
      ctx.fireChannelRead(msg);
    }
  }

  private void finish() {

    // Remove Sonar pipelines to avoid issues
    player.getPipeline().remove(DECODER);
    player.getPipeline().remove(HANDLER);

    // Add the player to the list of verified players
    player.getFallback().getVerified().add(player.getInetAddress());

    // Remove player from the list of connected players
    player.getFallback().getConnected().remove(player.getInetAddress());

    // Replace timeout handler with the old one to let Velocity handle timeouts again
    player.getPipeline().replace(Connections.READ_TIMEOUT, Connections.READ_TIMEOUT,
      new ReadTimeoutHandler(
        player.getConnection().server.getConfiguration().getConnectTimeout(),
        TimeUnit.MILLISECONDS
      ));

    // TODO: fix chunks not loading correctly
    initialConnection((AuthSessionHandler) player.getConnection().getSessionHandler());

    player.getFallback().getLogger().info("Successfully verified " + player.getPlayer().getUsername());
  }

  // Mostly taken from Velocity
  private void initialConnection(final AuthSessionHandler handler) {
    player.getConnection().server.getEventManager()
      .fire(new PermissionsSetupEvent(player.getPlayer(), DEFAULT_PERMISSION))
      .thenAcceptAsync(permissionEvent -> {
        if (!player.getConnection().isClosed()) {
          // wait for permissions to load, then set the players permission function
          final PermissionFunction function = permissionEvent.createFunction(player.getPlayer());

          if (function == null) {
            player.getFallback().getLogger().error(
              "A plugin permission provider {} provided an invalid permission function"
                + " for player {}. This is a bug in the plugin, not in Velocity. Falling"
                + " back to the default permission function.",
              permissionEvent.getProvider().getClass().getName(),
              player.getPlayer().getUsername());
          } else {
            try {
              SET_PERMISSION_FUNCTION.invokeExact(player.getPlayer(), function);
            } catch (Throwable throwable) {
              throwable.printStackTrace();
              throw new RuntimeException(throwable);
            }
          }

          player.getConnection().server.getEventManager()
            .fire(new LoginEvent(player.getPlayer()))
            .thenAcceptAsync(event -> {

              // The player was disconnected
              if (player.getConnection().isClosed()) {
                var disconnectEvent = new DisconnectEvent(
                  player.getPlayer(), DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE
                );

                player.getConnection().server.getEventManager().fireAndForget(disconnectEvent);
                return;
              }

              event.getResult().getReasonComponent().ifPresentOrElse(reason -> {
                player.getPlayer().disconnect0(reason, true);
              }, () -> {
                if (player.getConnection().server.registerConnection(player.getPlayer())) {
                  try {
                    var initialConnection = (InitialConnectSessionHandler) CONNECT_SESSION_HANDLER
                      .invokeExact(player.getPlayer(), (VelocityServer) player.getConnection().server);

                    player.getConnection().setSessionHandler(initialConnection);

                    player.getConnection().server.getEventManager()
                      .fire(new PostLoginEvent(player.getPlayer()))
                      .thenAccept(postLoginEvent -> {
                        try {
                          CONNECTION_FIELD.set(handler, player.getConnection());
                          CONNECT_TO_INITIAL_SERVER.invoke(handler, player.getPlayer());
                        } catch (Throwable throwable) {
                          throw new RuntimeException(throwable);
                        }
                      });
                  } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                  }
                } else {
                  player.getPlayer().disconnect0(
                    Component.translatable("velocity.error.already-connected-proxy"), true
                  );
                }
              });
            }, player.getChannel().eventLoop()).exceptionally(throwable -> {
              player.getFallback().getLogger().error(
                "Exception while completing login initialisation phase for {}", player, throwable
              );
              return null;
            });
        }
      }, player.getChannel().eventLoop());
  }

  private static JoinGame getForVersion(final int protocolVersion) {
    if (protocolVersion >= MINECRAFT_1_19_4.getProtocol()) {
      return FallbackPackets.JOIN_GAME_1_19_4;
    } else if (protocolVersion >= MINECRAFT_1_19_1.getProtocol()) {
      return FallbackPackets.JOIN_GAME_1_19_1;
    } else if (protocolVersion >= MINECRAFT_1_18_2.getProtocol()) {
      return FallbackPackets.JOIN_GAME_1_18_2;
    } else if (protocolVersion >= MINECRAFT_1_16_2.getProtocol()) {
      return FallbackPackets.JOIN_GAME_1_16_2;
    }
    return FallbackPackets.LEGACY_JOIN_GAME;
  }

  private static final CorruptedFrameException CORRUPTED_FRAME = new CorruptedFrameException();

  private void checkFrame(final boolean condition, final String message) {
    if (!condition) {
      player.fail(message);
      throw CORRUPTED_FRAME;
    }
  }
}
