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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.buffer.ByteBuf;
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

/**
 * <h3>Concept</h3>
 * Player joining<br>
 * ↓<br>
 * Send a {@link com.velocitypowered.proxy.protocol.packet.KeepAlive} packet and check for a valid response<br>
 * ↓<br>
 * Send the JoinGame packet to the client to make them unable to disconnect<br>
 * ↓<br>
 * Wait and check if the client sends a {@link com.velocitypowered.proxy.protocol.packet.ClientSettings} packet
 * and then a {@link com.velocitypowered.proxy.protocol.packet.PluginMessage}<br>
 * ↓<br>
 * (for 1.7-1.8) Mojang decided to send a {@link com.velocitypowered.proxy.protocol.packet.KeepAlive} packet with the
 * ID 0 every 20 ticks (= one second) while the player is in the GuiDownloadTerrain screen.<br>
 * ↓<br>
 * (for 1.8+) Send a {@link com.velocitypowered.proxy.protocol.packet.ResourcePackRequest} to check if the client
 * responds correctly<br>
 */
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

    // The client sends the PluginMessage packet and then the ClientSettings packet.
    // The player cannot send the ClientSettings packet twice since the world hasn't
    // loaded yet, therefore, the player cannot change any in-game settings.
    // This can actually be false (for some odd reason) when the client reconnects
    // too fast, so we just kick the player for safety and not actually punish them.
    if (hasSentClientBrand || hasSentClientSettings) {
      player.getPlayer().disconnect0(FallbackListener.CachedMessages.UNEXPECTED_ERROR, true);
      return false;
    }

    hasSentClientSettings = true;
    return false;
  }

  private static boolean validateClientBrand(final ByteBuf content) {
    // No need to check for empty or too long client brands since
    // ProtocolUtils#readString already does exactly that.
    final String read = ProtocolUtils.readString(content, 64);
    return !read.equals("Vanilla") // The normal brand is always lowercase
      // We want to allow client brands that have a URL in them
      // (e.g., CheatBreaker)
      && read.matches("^[a-zA-Z0-9/.:_-]+$"); // Normal regex validation
  }

  @Override
  public boolean handle(final @NotNull PluginMessage pluginMessage) {
    // Only MC|Brand for 1.7-1.12.2 and minecraft:brand for 1.13+ are important.
    if (!pluginMessage.getChannel().equals("MC|Brand")
      && !pluginMessage.getChannel().equals("minecraft:brand")) {
      // TODO: No other channel should be possible?
      return false; // Ignore all other channels
    }

    // Check if the channel is correct - 1.13 uses the new namespace
    // system ('minecraft:' + channel) and anything below 1.13 uses
    // the legacy namespace system ('MC|' + channel).
    final boolean exempt = player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_13) >= 0;

    checkFrame(pluginMessage.getChannel().equals("MC|Brand") || exempt, "invalid channel");
    checkFrame(validateClientBrand(pluginMessage.content()), "invalid client brand");

    // Check for illegal packet timing
    checkFrame(!hasSentClientBrand, "unexpected timing (P1)");
    checkFrame(hasSentClientSettings, "unexpected timing (P2)");

    hasSentClientBrand = true;

    // Anything below 1.9 doesn't handle resource pack requests properly,
    // so we just want the client to send a KeepAlive packet with the id 0
    // since the client sends KeepAlive packets with the id 0 every 20 ticks.
    if (!v1_8or1_7) {
      sendResourcePackRequest();
    }
    return false;
  }

  @Override
  public boolean handle(final @NotNull KeepAlive keepAlive) {
    if (keepAlive.getRandomId() == 0 && v1_8or1_7) {

      // First, let's validate if the packet could actually be sent at this point.
      checkFrame(hasSentClientBrand, "unexpected timing (K1): " + keepAlive.getRandomId());
      checkFrame(hasSentClientSettings, "unexpected timing (K2): " + keepAlive.getRandomId());

      // Versions below 1.9 do not check the resource pack URL and hash.
      // We have to skip this check and verify the connection.
      finish();
    } else {

      // On non-1.8 clients, there is no KeepAlive packet that can be sent at this stage.
      player.fail("unexpected timing (K3): " + keepAlive.getRandomId());
    }
    return false;
  }

  private void sendResourcePackRequest() {
    final ResourcePackRequest resourcePackRequest = new ResourcePackRequest();

    // The hash and URL have to be invalid for this check to work
    // since we don't want to client to actually download a resource pack
    // or override the server resource packets option (prompt).
    //
    // Generate a random hash as a placeholder and method of verification.
    resourcePackHash = Integer.toHexString(random.nextInt());
    resourcePackRequest.setHash(resourcePackHash);
    resourcePackRequest.setUrl(resourcePackHash);

    // Send the ResourcePackRequest packet
    player.getConnection().write(resourcePackRequest);
  }

  @Override
  public boolean handle(final ResourcePackResponse resourcePackResponse) {
    checkFrame(resourcePackHash != null, "hash has not been set");

    // 1.10+ clients do not send the hash back to the server if the download fails.
    // That means that we can validate the hash sent by the client and compare
    // it with the hash we set on the server side.
    if (player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_10) >= 0) {
      // Check if the hash is empty
      checkFrame(resourcePackResponse.getHash().isEmpty(), "invalid hash (1.10+)");
    } else {
      // Check if the hash is the same as on the server
      checkFrame(Objects.equals(resourcePackResponse.getHash(), resourcePackHash), "invalid hash");
    }

    checkFrame( // The download will always fail because we never provided a real URL
      resourcePackResponse.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD,
      "invalid status"
    );

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
  private synchronized void finish() {

    // Sonar doesn't care about the player anymore
    player.getPipeline().remove(DECODER);
    player.getPipeline().remove(HANDLER);

    player.getFallback().getVerified().add(player.getInetAddress().toString());
    player.getFallback().getConnected().remove(player.getInetAddress().toString());

    // Replace timeout handler with the old one to let Velocity handle timeouts again
    player.getPipeline().replace(
      READ_TIMEOUT,
      READ_TIMEOUT,
      new ReadTimeoutHandler(
        player.getConnection().server.getConfiguration().getConnectTimeout(),
        TimeUnit.MILLISECONDS
      )
    );

    // Continue the initial connection to the backend server
    initialConnection(previousHandler);

    player.getFallback().getLogger().info("Successfully verified " + player.getPlayer().getUsername());
  }

  // Mostly taken from Velocity
  private void initialConnection(final MinecraftSessionHandler sessionHandler) {
    player.getConnection().server.getEventManager()
      .fire(new PermissionsSetupEvent(player.getPlayer(), DEFAULT_PERMISSION))
      .thenAcceptAsync(permissionEvent -> {
        if (!player.getConnection().isClosed()) {
          // wait for permissions to load, then set the player permission function
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
                  player.getPlayer(),
                  DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE
                );

                player.getConnection().server.getEventManager().fireAndForget(disconnectEvent);
                return;
              }

              event.getResult().getReasonComponent().ifPresentOrElse(reason -> {
                player.getPlayer().disconnect0(reason, true);
              }, () -> {
                if (player.getConnection().server.registerConnection(player.getPlayer())) {
                  try {
                    final var initialConnection =
                      (InitialConnectSessionHandler) CONNECT_SESSION_HANDLER
                        .invokeExact(player.getPlayer(),
                          (VelocityServer) player.getConnection().server);

                    player.getConnection().setSessionHandler(initialConnection);

                    player.getConnection().server.getEventManager()
                      .fire(new PostLoginEvent(player.getPlayer()))
                      .thenAcceptAsync(ignored -> {
                        try {
                          CONNECTION_FIELD.set(sessionHandler,
                            player.getConnection());

                          // It works. We'll leave it at that
                          player.getPipeline().addAfter(
                            MINECRAFT_ENCODER,
                            RESPAWN,
                            new FallbackRespawnHandler(player)
                          );

                          CONNECT_TO_INITIAL_SERVER.invoke(sessionHandler,
                            player.getPlayer());
                        } catch (Throwable throwable) {
                          throw new RuntimeException(throwable);
                        }
                      });
                  } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                  }
                } else {
                  player.getPlayer().disconnect0(
                    Component.translatable("velocity.error.already-connected-proxy"),
                    true
                  );
                }
              });
            }, player.getChannel().eventLoop()).exceptionally(throwable -> {
              player.getFallback().getLogger().error(
                "Exception while completing login initialisation phase for {}", player,
                throwable
              );
              return null;
            });
        }
      }, player.getConnection().eventLoop());
  }
}
