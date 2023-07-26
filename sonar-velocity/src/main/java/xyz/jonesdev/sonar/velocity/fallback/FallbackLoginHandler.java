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

package xyz.jonesdev.sonar.velocity.fallback;

import com.google.common.primitives.Longs;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.velocity.SonarVelocity;
import xyz.jonesdev.sonar.velocity.fallback.session.FallbackPlayer;
import xyz.jonesdev.sonar.velocity.fallback.session.FallbackSessionHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;
import static com.velocitypowered.proxy.crypto.EncryptionUtils.decryptRsa;
import static com.velocitypowered.proxy.crypto.EncryptionUtils.generateServerId;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.DECODER;

@Getter
public final class FallbackLoginHandler implements MinecraftSessionHandler {
  private final Fallback fallback;
  private final MinecraftConnection mcConnection;
  private final LoginInboundConnection inboundConnection;
  private final InitialLoginSessionHandler sessionHandler;
  private final VelocityServer server;
  private final String username;
  private final InetAddress inetAddress;
  private final boolean premium;
  private GameProfile gameProfile;

  private static final String MOJANG_HASJOINED_URL =
    System.getProperty("mojang.sessionserver",
        "https://sessionserver.mojang.com/session/minecraft/hasJoined")
      .concat("?username=%s&serverId=%s");
  private static final MethodHandle CONNECTED_PLAYER;
  private static final Field LOGIN_PACKET;

  static {
    try {
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

      LOGIN_PACKET = InitialLoginSessionHandler.class.getDeclaredField("login");
      LOGIN_PACKET.setAccessible(true);
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  private byte[] verify = EMPTY_BYTE_ARRAY;
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  private LoginState currentState = LoginState.LOGIN_PACKET_RECEIVED;

  private enum LoginState {
    LOGIN_PACKET_RECEIVED,
    ENCRYPTION_REQUEST_SENT,
    ENCRYPTION_RESPONSE_RECEIVED
  }

  public FallbackLoginHandler(final Fallback fallback,
                              final MinecraftConnection mcConnection,
                              final LoginInboundConnection inboundConnection,
                              final InitialLoginSessionHandler sessionHandler,
                              final String username,
                              final InetAddress inetAddress,
                              final boolean premium) {
    this.fallback = fallback;
    this.mcConnection = mcConnection;
    this.inboundConnection = inboundConnection;
    this.sessionHandler = sessionHandler;
    this.server = (VelocityServer) SonarVelocity.INSTANCE.getPlugin().getServer();
    this.username = username;
    this.inetAddress = inetAddress;
    this.premium = premium;

    if (premium) {
      final EncryptionRequest request = generateEncryptionRequest();
      this.verify = Arrays.copyOf(request.getVerifyToken(), 4);

      mcConnection.write(request);

      this.currentState = LoginState.ENCRYPTION_REQUEST_SENT;
    } else {
      this.gameProfile = GameProfile.forOfflinePlayer(username);
      handleInitialLogin(gameProfile);
    }
  }

  private void handleInitialLogin(final GameProfile gameProfile) {

    // Create an instance for the connected player
    final ConnectedPlayer player;
    try {
      player = (ConnectedPlayer) CONNECTED_PLAYER.invokeExact(
        mcConnection.server,
        gameProfile,
        mcConnection,
        inboundConnection.getVirtualHost().orElse(null),
        premium,
        inboundConnection.getIdentifiedKey()
      );
    } catch (Throwable throwable) {
      // This should not happen
      fallback.getLogger().error("Error while processing {}: {}", username, throwable);
      mcConnection.close(true);
      return;
    }

    // Check if the player is already connected to the proxy
    // We use the default Velocity method for this to avoid incompatibilities
    if (!mcConnection.server.canRegisterConnection(player)) {
      mcConnection.closeWith(Disconnect.create(
        Component.translatable("velocity.error.already-connected-proxy", NamedTextColor.RED),
        mcConnection.getProtocolVersion()
      ));
      return;
    }

    // Create an instance for the Fallback connection
    final FallbackPlayer fallbackPlayer = new FallbackPlayer(
      fallback, player, mcConnection, mcConnection.getChannel(),
      mcConnection.getChannel().pipeline(), inetAddress,
      player.getProtocolVersion().getProtocol()
    );

    if (fallback.getSonar().getConfig().LOG_CONNECTIONS) {
      // Only log the processing message if the server isn't under attack.
      // We let the user override this through the configuration.
      if (!fallback.isUnderAttack() || fallback.getSonar().getConfig().LOG_DURING_ATTACK) {
        fallback.getLogger().info("Processing: {}{} ({})",
          username, inetAddress, fallbackPlayer.getProtocolVersion());
      }
    }

    // Mark the player as connected → verifying players
    fallback.getConnected().put(username, inetAddress);

    // Set compression
    final int threshold = mcConnection.server.getConfiguration().getCompressionThreshold();
    if (threshold >= 0 && mcConnection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
      mcConnection.write(new SetCompression(threshold));
      mcConnection.setCompressionThreshold(threshold);
    }

    // Send LoginSuccess packet to spoof our fake lobby
    final ServerLoginSuccess success = new ServerLoginSuccess();

    success.setUsername(player.getUsername());
    success.setProperties(player.getGameProfileProperties());
    success.setUuid(player.getUniqueId());

    mcConnection.write(success);

    // Set the state to a custom one, so we can receive and send more packets
    mcConnection.setAssociation(player);
    mcConnection.setState(StateRegistry.PLAY);

    final long keepAliveId = ThreadLocalRandom.current().nextInt();

    // We have to add this pipeline to monitor all incoming traffic
    // We add the pipeline after the MinecraftDecoder since we want
    // the packets to be processed and decoded already
    fallbackPlayer.getPipeline().addAfter(
      MINECRAFT_DECODER,
      DECODER,
      new FallbackPacketDecoder(
        fallbackPlayer,
        this,
        keepAliveId
      )
    );

    // ==================================================================
    if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      // The first step of the verification is a simple KeepAlive packet
      // We don't want to waste resources by directly sending all packets to
      // the client, which is why we first send a KeepAlive packet and then
      // wait for a valid response to continue the verification process
      final KeepAlive keepAlive = new KeepAlive();

      keepAlive.setRandomId(keepAliveId);

      mcConnection.write(keepAlive);
    } else {
      // KeepAlive packets do not exist during the login process on 1.7.
      // We have to fall back to the regular method of verification

      // Set session handler to custom fallback handler to intercept all incoming packets
      mcConnection.setSessionHandler(new FallbackSessionHandler(this, fallbackPlayer));

      // Send JoinGame packet
      mcConnection.write(FallbackPackets.LEGACY_JOIN_GAME);
    }
    // ==================================================================
  }

  // Taken from Velocity
  private EncryptionRequest generateEncryptionRequest() {
    final byte[] verify = new byte[4];
    ThreadLocalRandom.current().nextBytes(verify);

    final EncryptionRequest request = new EncryptionRequest();
    request.setPublicKey(server.getServerKeyPair().getPublic().getEncoded());
    request.setVerifyToken(verify);
    return request;
  }

  // Taken from Velocity
  @Override
  public boolean handle(final EncryptionResponse packet) {
    if (currentState != LoginState.ENCRYPTION_REQUEST_SENT) {
      mcConnection.close(true);
    }
    currentState = LoginState.ENCRYPTION_RESPONSE_RECEIVED;

    final ServerLogin login;
    try {
      login = (ServerLogin) LOGIN_PACKET.get(sessionHandler);
    } catch (IllegalAccessException exception) {
      throw new IllegalStateException("Could not access ServerLogin field.");
    }

    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }

    if (verify.length == 0) {
      throw new IllegalStateException("No EncryptionRequest packet sent yet.");
    }

    try {
      final KeyPair serverKeyPair = server.getServerKeyPair();

      if (inboundConnection.getIdentifiedKey() != null) {
        final IdentifiedKey playerKey = inboundConnection.getIdentifiedKey();
        if (!playerKey.verifyDataSignature(packet.getVerifyToken(), verify,
          Longs.toByteArray(packet.getSalt()))) {
          throw new IllegalStateException("Invalid client public signature.");
        }
      } else {
        final byte[] decryptedVerifyToken = decryptRsa(serverKeyPair, packet.getVerifyToken());

        if (!MessageDigest.isEqual(verify, decryptedVerifyToken)) {
          throw new IllegalStateException("Unable to successfully decrypt the verification token.");
        }
      }

      final byte[] decryptedSharedSecret = decryptRsa(serverKeyPair, packet.getSharedSecret());
      final String serverId = generateServerId(decryptedSharedSecret, serverKeyPair.getPublic());

      String playerIp = ((InetSocketAddress) mcConnection.getRemoteAddress()).getHostString();
      String url = String.format(MOJANG_HASJOINED_URL,
        urlFormParameterEscaper().escape(login.getUsername()), serverId);

      if (server.getConfiguration().shouldPreventClientProxyConnections()) {
        url += "&ip=" + urlFormParameterEscaper().escape(playerIp);
      }

      final ListenableFuture<Response> hasJoinedResponse = server.getAsyncHttpClient()
        .prepareGet(url)
        .execute();

      hasJoinedResponse.addListener(() -> {
        if (mcConnection.isClosed()) {
          // The player disconnected after we authenticated them.
          return;
        }

        // Go ahead and enable encryption. Once the client sends EncryptionResponse, encryption
        // is enabled.
        try {
          mcConnection.enableEncryption(decryptedSharedSecret);
        } catch (GeneralSecurityException exception) {
          fallback.getLogger().error("Unable to enable encryption for connection", exception);
          // At this point, the connection is encrypted, but something's wrong on our side, and
          // we can't do anything about it.
          mcConnection.close(true);
          return;
        }

        try {
          final Response profileResponse = hasJoinedResponse.get();

          if (profileResponse.getStatusCode() == 200) {
            final GameProfile profile = GENERAL_GSON.fromJson(profileResponse.getResponseBody(),
              GameProfile.class);
            // Not so fast, now we verify the public key for 1.19.1+
            if (inboundConnection.getIdentifiedKey() != null
              && inboundConnection.getIdentifiedKey().getKeyRevision() == IdentifiedKey.Revision.LINKED_V2
              && inboundConnection.getIdentifiedKey() instanceof IdentifiedKeyImpl key) {
              if (!key.internalAddHolder(profile.getId())) {
                inboundConnection.disconnect(
                  Component.translatable("multiplayer.disconnect.invalid_public_key"));
              }
            }

            // All went well, initialize the session.
            this.gameProfile = profile;
            handleInitialLogin(profile);
          } else if (profileResponse.getStatusCode() == 204) {
            // Apparently, an offline-mode user logged onto this online-mode proxy.
            inboundConnection.disconnect(Component.translatable("velocity.error.online-mode-only",
              NamedTextColor.RED));
          } else {
            // Something else went wrong
            fallback.getLogger().error(
              "Got an unexpected error code {} whilst contacting Mojang to log in {} ({})",
              profileResponse.getStatusCode(), login.getUsername(), playerIp);
            inboundConnection.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
          }
        } catch (ExecutionException exception) {
          fallback.getLogger().error("Unable to authenticate with Mojang", exception);
          inboundConnection.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
        } catch (InterruptedException exception) {
          // not much we can do usefully
          Thread.currentThread().interrupt();
        }
      }, mcConnection.eventLoop());
    } catch (GeneralSecurityException exception) {
      fallback.getLogger().error("Unable to enable encryption", exception);
      mcConnection.close(true);
    }
    return true;
  }

  @Override
  public boolean handle(final LoginPluginResponse packet) {
    return sessionHandler.handle(packet);
  }

  @Override
  public void handleUnknown(final ByteBuf buf) {
    mcConnection.close(true);
  }

  @Override
  public void disconnected() {
    sessionHandler.disconnected();
  }
}
