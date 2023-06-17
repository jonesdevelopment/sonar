/*
 * Copyright (C) 2023 jones
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

package jones.sonar.velocity.fallback.session;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jones.sonar.api.fallback.FallbackConnection;
import jones.sonar.velocity.fallback.FallbackListener;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.api.network.ProtocolVersion.*;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;
import static jones.sonar.api.fallback.FallbackPipelines.*;
import static jones.sonar.velocity.fallback.FallbackListener.CONNECTION_FIELD;

public final class FallbackSessionHandler implements MinecraftSessionHandler {
  private final @Nullable MinecraftSessionHandler previousHandler;
  private final @NotNull FallbackPlayer player;
  private final boolean v1_8or1_7;
  private @Nullable String resourcePackHash;
  private static final Random random = new SecureRandom();

  public FallbackSessionHandler(final @Nullable MinecraftSessionHandler previousHandler,
                                final @NotNull FallbackPlayer player) {
    this.previousHandler = previousHandler;
    this.player = player;
    this.v1_8or1_7 = player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0;
  }

  private boolean hasSentClientBrand, hasSentClientSettings;

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final MethodHandle CONNECT_TO_INITIAL_SERVER;
  private static final MethodHandle CONNECT_SESSION_HANDLER;
  private static final MethodHandle SET_PERMISSION_FUNCTION;
  private static final PermissionProvider DEFAULT_PERMISSION;

  static {
    try {
      CONNECT_TO_INITIAL_SERVER = MethodHandles.privateLookupIn(
          AuthSessionHandler.class, LOOKUP)
        .findVirtual(AuthSessionHandler.class,
          "connectToInitialServer",
          MethodType.methodType(CompletableFuture.class, ConnectedPlayer.class)
        );

      CONNECT_SESSION_HANDLER = MethodHandles.privateLookupIn(
          InitialConnectSessionHandler.class, LOOKUP)
        .findConstructor(InitialConnectSessionHandler.class,
          MethodType.methodType(void.class, ConnectedPlayer.class, VelocityServer.class)
        );

      final Field PERMISSIONS_FIELD = ConnectedPlayer.class.getDeclaredField("DEFAULT_PERMISSIONS");
      PERMISSIONS_FIELD.setAccessible(true);

      DEFAULT_PERMISSION = (PermissionProvider) PERMISSIONS_FIELD.get(PermissionProvider.class);

      SET_PERMISSION_FUNCTION = MethodHandles.privateLookupIn(AuthSessionHandler.class, LOOKUP)
        .findVirtual(ConnectedPlayer.class,
          "setPermissionFunction",
          MethodType.methodType(void.class, PermissionFunction.class)
        );
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  @Override
  public boolean handle(final ClientSettings clientSettings) {
    if (hasSentClientBrand || hasSentClientSettings) {
      player.getPlayer().disconnect0(FallbackListener.CachedMessages.UNEXPECTED_ERROR, true);
      return false;
    }

    hasSentClientSettings = true;
    return false;
  }

  @Override
  public boolean handle(final PluginMessage pluginMessage) {
    if (!pluginMessage.getChannel().equals("MC|Brand") && !pluginMessage.getChannel().equals("minecraft:brand")) {
      return false; // Ignore all other channels
    }

    final boolean valid = player.getProtocolVersion() >= MINECRAFT_1_13.getProtocol();

    checkFrame(pluginMessage.getChannel().equals("MC|Brand") || valid, "invalid channel");
    // TODO: actually implement a client brand check?
    checkFrame(pluginMessage.content().readableBytes() > 1, "invalid client brand");
    checkFrame(!hasSentClientBrand, "unexpected timing (P1)");
    checkFrame(hasSentClientSettings, "unexpected timing (P2)");

    hasSentClientBrand = true;

    // We use a different verification method for 1.7-1.8
    if (!v1_8or1_7) {
      sendResourcePackRequest();
    }
    return false;
  }

  @Override
  public boolean handle(final KeepAlive keepAlive) {
    if (keepAlive.getRandomId() == 0 && v1_8or1_7) {

      // First, let's validate if the packet could actually be sent at this point
      checkFrame(hasSentClientBrand, "unexpected timing (K1): " + keepAlive.getRandomId());
      checkFrame(hasSentClientSettings, "unexpected timing (K2): " + keepAlive.getRandomId());

      // 1.7 does not support resource pack prompts, verify the connection
      if (player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_7_6) <= 0) {
        finish();
      } else {
        // We already ran the other checks, let's go to the next stage
        sendResourcePackRequest();
      }
    } else {

      // On non-1.8 clients there isn't any other KeepAlive packet that can be sent now
      player.fail("unexpected timing (K3): " + keepAlive.getRandomId());
    }
    return false;
  }

  private void sendResourcePackRequest() {
    final ResourcePackRequest resourcePackRequest = new ResourcePackRequest();

    // The hash and URL have to be invalid for this check to work
    // since we don't want to client to actually download a resource pack
    // or override the server resource packets option (prompt)
    resourcePackHash = Integer.toHexString(random.nextInt());
    resourcePackRequest.setHash(resourcePackHash);
    resourcePackRequest.setUrl(resourcePackHash);

    // Send the ResourcePackRequest packet
    player.getConnection().write(resourcePackRequest);
  }

  @Override
  public boolean handle(final ResourcePackResponse resourcePackResponse) {
    checkFrame(resourcePackHash != null, "hash has not been set");

    // 1.10+ clients do not send the hash back to the server if the download fails
    if (player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_10) >= 0) {
      checkFrame(resourcePackResponse.getHash().isEmpty(), "invalid hash (1.9+)");
    } else {
      checkFrame(Objects.equals(resourcePackResponse.getHash(), resourcePackHash), "invalid hash");
    }

    checkFrame(resourcePackResponse.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD, "invalid status");

    finish();
    return false;
  }

  private static final CorruptedFrameException CORRUPTED_FRAME = new CorruptedFrameException();

  private void checkFrame(final boolean condition, final String message) {
    checkFrame(player, condition, message);
  }

  public static void checkFrame(final FallbackConnection<?, ?> player,
                                final boolean condition,
                                final String message) {
    if (!condition) {
      player.fail(message);
      throw CORRUPTED_FRAME;
    }
  }

  /**
   * Restore old pipelines and send the player to the actual server
   */
  private void finish() {

    // Remove Sonar pipelines to avoid issues
    player.getPipeline().remove(DECODER);
    player.getPipeline().remove(HANDLER);

    // Add the player to the list of verified players
    player.getFallback().getVerified().add(player.getInetAddress());

    // Remove player from the list of connected players
    player.getFallback().getConnected().remove(player.getInetAddress());

    // Replace timeout handler with the old one to let Velocity handle timeouts again
    player.getPipeline().replace(
      READ_TIMEOUT,
      READ_TIMEOUT,
      new ReadTimeoutHandler(
        player.getConnection().server.getConfiguration().getConnectTimeout(),
        TimeUnit.MILLISECONDS
      )
    );

    initialConnection((AuthSessionHandler) previousHandler);

    player.getFallback().getLogger().info("Successfully verified " + player.getPlayer().getUsername());
  }

  // Mostly taken from Velocity
  private void initialConnection(final AuthSessionHandler sessionHandler) {
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
                final var disconnectEvent = new DisconnectEvent(
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
                    final var initialConnection = (InitialConnectSessionHandler) CONNECT_SESSION_HANDLER
                      .invokeExact(player.getPlayer(), (VelocityServer) player.getConnection().server);

                    player.getConnection().setSessionHandler(initialConnection);

                    player.getConnection().server.getEventManager()
                      .fire(new PostLoginEvent(player.getPlayer()))
                      .thenAcceptAsync(ignored -> {
                        try {
                          CONNECTION_FIELD.set(sessionHandler, player.getConnection());

                          // It works. We'll leave it at that
                          player.getPipeline().addAfter(
                            MINECRAFT_ENCODER,
                            RESPAWN,
                            new FallbackRespawnHandler(player)
                          );

                          CONNECT_TO_INITIAL_SERVER.invoke(sessionHandler, player.getPlayer());
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
      }, player.getConnection().eventLoop());
  }
}
