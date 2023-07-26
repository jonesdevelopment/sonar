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

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.common.exception.ReflectionException;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.protocol.ProtocolUtil;
import xyz.jonesdev.sonar.velocity.fallback.FallbackHandler;
import xyz.jonesdev.sonar.velocity.fallback.FallbackListener;

import java.lang.reflect.Field;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_ENCODER;

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
 * Set handler to next, movement-validating, handler
 */
public final class FallbackSessionHandler implements MinecraftSessionHandler, FallbackHandler {
  @Getter
  private final @NotNull FallbackPlayer player;
  private final boolean v1_8or1_7;

  public FallbackSessionHandler(final @NotNull FallbackPlayer player) {
    this.player = player;
    this.v1_8or1_7 = player.getPlayer().getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0;
  }

  private boolean hasSentClientBrand, hasSentClientSettings, verified;

  private static final Field CONNECTION_FIELD;

  static {
    try {
      CONNECTION_FIELD = AuthSessionHandler.class.getDeclaredField("mcConnection");
      CONNECTION_FIELD.setAccessible(true);
    } catch (Throwable throwable) {
      throw new ReflectionException(throwable);
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
      nextStage();
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
      nextStage();
    } else {

      // On non-1.8 clients, there is no KeepAlive packet that can be sent at this stage.
      player.fail("unexpected timing (K3): " + keepAlive.getRandomId());
    }
    return false;
  }

  /**
   * Restore old pipelines and send the player to the actual server
   */
  private synchronized void nextStage() {
    // We need this to prevent some packets from flagging bad packet checks
    verified = true;

    player.getPipeline().replace(
      MINECRAFT_ENCODER,
      FALLBACK_PACKET_ENCODER,
      new FallbackPacketEncoder(player.getProtocolId())
    );
    player.getPipeline().replace(
      MINECRAFT_DECODER,
      FALLBACK_PACKET_DECODER,
      new FallbackPacketDecoder(
        player.getProtocolId(),
        new FallbackVerification(player)
      )
    );
  }
}
