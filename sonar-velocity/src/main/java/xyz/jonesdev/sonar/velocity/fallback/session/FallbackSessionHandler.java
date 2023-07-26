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

package xyz.jonesdev.sonar.velocity.fallback.session;

import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackConnection;
import xyz.jonesdev.sonar.common.protocol.ProtocolUtil;
import xyz.jonesdev.sonar.velocity.fallback.FallbackListener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

import static com.velocitypowered.api.network.ProtocolVersion.*;
import static xyz.jonesdev.sonar.velocity.fallback.FallbackListener.CachedMessages.VERIFICATION_SUCCESS;

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
 * (for 1.9+) Send a {@link com.velocitypowered.proxy.protocol.packet.ResourcePackRequest} to check if the client
 * responds correctly<br>
 */
public final class FallbackSessionHandler implements MinecraftSessionHandler {
  private final @NotNull FallbackPlayer player;
  private final boolean v1_8or1_7;
  private @Nullable String resourcePackHash;
  private static final Random random = new SecureRandom();

  public FallbackSessionHandler(final @NotNull FallbackPlayer player) {
    this.player = player;
    this.v1_8or1_7 = player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0;
  }

  private boolean hasSentClientBrand, hasSentClientSettings, verified;

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final MethodHandle NEW_AUTH_HANDLER;
  private static final Field CONNECTION_FIELD;

  static {
    try {
      NEW_AUTH_HANDLER = MethodHandles.privateLookupIn(
          AuthSessionHandler.class, LOOKUP)
        .findConstructor(AuthSessionHandler.class,
          MethodType.methodType(
            void.class,
            VelocityServer.class,
            LoginInboundConnection.class,
            GameProfile.class,
            boolean.class
          )
        );

      CONNECTION_FIELD = AuthSessionHandler.class.getDeclaredField("mcConnection");
      CONNECTION_FIELD.setAccessible(true);
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  @Override
  public boolean handle(final ClientSettings clientSettings) {
    if (verified) return false; // Skip verified players so we don't run into issues

    // The client sends the PluginMessage packet and then the ClientSettings packet.
    // The player cannot send the ClientSettings packet twice since the world hasn't
    // loaded yet, therefore, the player cannot change any in-game settings.
    // This can actually be false (for some odd reason) when the client reconnects
    // too fast, so we just kick the player for safety and not actually punish them.
    if (hasSentClientBrand || hasSentClientSettings) {
      player.getConnection().closeWith(
        Disconnect.create(FallbackListener.CachedMessages.UNEXPECTED_ERROR, player.getPlayer().getProtocolVersion()
        ));

      // Log this incident to make sure an administrator knows what happened
      player.getFallback().getLogger().warn(
        "Disconnecting {} due to an unexpected error (lag?)", player.getPlayer().getUsername()
      );
      return false;
    }

    hasSentClientSettings = true;
    return false;
  }

  private static boolean validateClientBrand(final FallbackPlayer player, final ByteBuf content) {
    // We have to catch every DecoderException, so we can fail and punish
    // the player instead of only disconnecting them due to an exception.
    try {
      final boolean legacy = player.getConnection().getProtocolVersion().compareTo(MINECRAFT_1_8) < 0;
      // 1.7 has some very weird issues when trying to decode the client brand
      final int cap = player.getFallback().getSonar().getConfig().MAXIMUM_BRAND_LENGTH;
      // Read the client brand using our custom readString method that supports 1.7.
      // The legacy version of readString does not compare the string length
      // with the VarInt sent by the client.
      final String read = ProtocolUtil.readString(content, cap, legacy);
      // No need to check for empty or too long client brands since
      // ProtocolUtil#readString already does exactly that.
      return !read.equals("Vanilla") // The normal brand is always lowercase
        // We want to allow client brands that have a URL in them
        // (e.g., CheatBreaker)
        && Sonar.get().getConfig().VALID_BRAND_REGEX.matcher(read).matches(); // Normal regex validation
    } catch (DecoderException exception) {
      // Fail if the string (client brand) could not be decoded properly
      player.fail("could not decode string");
      // Throw the exception so we don't continue checking
      throw exception;
    }
  }

  @Override
  public boolean handle(final @NotNull PluginMessage pluginMessage) {
    if (verified) return false; // Skip verified players so we don't run into issues

    // Only 'MC|Brand' for 1.7-1.12.2 and 'minecraft:brand' for 1.13+ are important.
    if (!pluginMessage.getChannel().equals("MC|Brand")
      && !pluginMessage.getChannel().equals("minecraft:brand")) {
      return false; // Ignore all other channels
    }

    // Check if the channel is correct - 1.13 uses the new namespace
    // system ('minecraft:' + channel) and anything below 1.13 uses
    // the legacy namespace system ('MC|' + channel).
    final boolean v1_13 = player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_13) >= 0;
    checkFrame(pluginMessage.getChannel().equals("MC|Brand") || v1_13, "invalid channel");

    // Validate the client branding using a regex to filter unwanted characters.
    checkFrame(validateClientBrand(player, pluginMessage.content()), "invalid client brand");

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
    if (verified) return false; // Skip verified players so we don't run into issues

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
    if (verified) return false; // Skip verified players so we don't run into issues

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
      resourcePackResponse.getStatus() != PlayerResourcePackStatusEvent.Status.SUCCESSFUL,
      "invalid status: " + resourcePackResponse.getStatus()
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
    player.getFallback().getVerified().add(player.getInetAddress().toString());
    player.getFallback().getConnected().remove(player.getPlayer().getUsername());

    // We need this to prevent some packets from flagging bad packet checks
    verified = true;

    player.getConnection().closeWith(Disconnect.create(
      VERIFICATION_SUCCESS,
      player.getConnection().getProtocolVersion()
    ));

    player.getFallback().getLogger().info("Successfully verified " + player.getPlayer().getUsername());
  }
}
