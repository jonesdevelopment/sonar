/*
 * Copyright (C) 2025 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.verification;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration.FinishConfigurationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginAcknowledgedPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.ClientInformationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.KeepAlivePacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.PluginMessagePacket;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackPreJoinHandler extends FallbackVerificationHandler {

  public FallbackPreJoinHandler(final @NotNull FallbackUser user) {
    super(user);

    // Start initializing the actual join process for pre-1.20.2 clients
    if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      // This trick helps in reducing unnecessary outgoing server traffic
      // by avoiding sending other packets to clients that are potentially bots.
      if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_8)) {
        user.channel().eventLoop().schedule(this::markSuccess, 100L, TimeUnit.MILLISECONDS);
      } else {
        /*
         * The purpose of this KeepAlive packet is to confirm that the connection
         * is active and legitimate, thereby preventing bot connections that
         * could flood the server with login attempts and other unwanted traffic.
         */
        user.write(PRE_JOIN_KEEP_ALIVE);
      }
    }
  }

  private boolean receivedClientInfo, receivedClientBrand, acknowledgedLogin;
  private int expectedKeepAliveId = PRE_JOIN_KEEP_ALIVE_ID;

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof KeepAlivePacket) {
      // This is the first packet we expect from the client
      final KeepAlivePacket keepAlive = (KeepAlivePacket) packet;

      // Check if the KeepAlive ID matches the expected ID
      final long keepAliveId = keepAlive.getId();
      checkState(keepAliveId == expectedKeepAliveId,
        "expected K ID " + expectedKeepAliveId + " but got " + keepAliveId);

      // Immediately verify the player if they do not need any configuration (pre-1.20.2)
      if (expectedKeepAliveId != 0) {
        if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
          markSuccess();
        } else {
          markAcknowledged();
        }

        // Disable the check for any further packets the client might send
        // while loading the world (or similar).
        expectedKeepAliveId = 0;
      }
    } else if (packet instanceof LoginAcknowledgedPacket) {
      // Prevent users from sending multiple LoginAcknowledged packets
      checkState(!acknowledgedLogin, "sent duplicate login ack");
      // Update state, so we're able to send/receive packets during the CONFIG state
      updateEncoderDecoderState(FallbackPacketRegistry.CONFIG);
      // Perform the KeepAlive check now (config, not pre-config)
      user.write(PRE_JOIN_KEEP_ALIVE);
    } else if (packet instanceof FinishConfigurationPacket) {
      // Update the encoder and decoder state because we're currently in the CONFIG state
      updateEncoderDecoderState(FallbackPacketRegistry.GAME);
      if (!user.isGeyser()) {
        validateClientInformation();
      }
      markSuccess();
    } else if (packet instanceof ClientInformationPacket) {
      final ClientInformationPacket clientInformation = (ClientInformationPacket) packet;

      if (!user.isGeyser()) {
        checkState(clientInformation.getViewDistance() >= 2,
          "view distance: " + clientInformation.getViewDistance());
      }

      receivedClientInfo = true;
    } else if (packet instanceof PluginMessagePacket) {
      final PluginMessagePacket pluginMessage = (PluginMessagePacket) packet;

      // TODO: Resolve as namespace (?)
      final boolean usingModernChannel = pluginMessage.getChannel().equals("minecraft:brand");
      final boolean usingLegacyChannel = pluginMessage.getChannel().equals("MC|Brand");

      // Skip this payload if it does not contain client brand information
      if (!usingModernChannel && !usingLegacyChannel) {
        return;
      }

      // Validate the client branding using a regex to filter unwanted characters.
      if (Sonar.get0().getConfig().getVerification().getBrand().isEnabled()) {
        // Make sure the player isn't sending the client brand multiple times
        checkState(!receivedClientBrand, "sent duplicate client brand");

        validateClientBrand(pluginMessage.getData());
      }

      receivedClientBrand = true;
    }
  }

  private void markAcknowledged() {
    acknowledgedLogin = true;
    // Write the new RegistrySync packets to the buffer
    for (final FallbackPacket packet : getRegistryPackets(user.getProtocolVersion())) {
      user.delayedWrite(packet);
    }
    // Write the FinishConfiguration packet to the buffer
    user.delayedWrite(FinishConfigurationPacket.INSTANCE);
    // Send all packets in one flush
    user.channel().flush();
  }

  private void markSuccess() {
    if (user.channel().isActive()) {
      if (!Sonar.get0().getConfig().getVerification().getValidNameRegex().matcher(user.getUsername()).matches()) {
        user.disconnect(Sonar.get0().getConfig().getVerification().getInvalidUsername());
        return;
      }

      // Pass the player to the next verification handler
      final FallbackGravityHandler gravityHandler = new FallbackGravityHandler(user, this);
      user.channel().pipeline().get(FallbackPacketDecoder.class).setListener(gravityHandler);
    }
  }

  void validateClientInformation() {
    checkState(receivedClientInfo, "didn't send client settings");
    checkState(receivedClientBrand, "didn't send client brand");
  }

  private void updateEncoderDecoderState(final @NotNull FallbackPacketRegistry registry) {
    // Update the packet registry state in the encoder and decoder pipelines
    user.channel().pipeline().get(FallbackPacketDecoder.class).updateRegistry(registry);
    user.channel().pipeline().get(FallbackPacketEncoder.class).updateRegistry(registry);
  }

  private void validateClientBrand(final byte @NotNull [] data) {
    // Check if the client brand is too short. It has to have at least 2 bytes.
    checkState(data.length > 1, "client brand is too short");
    // Check if the decoded client brand string is too long
    checkState(data.length < Sonar.get0().getConfig().getVerification().getBrand().getMaxLength(),
      "client brand contains too much data: " + data.length);
    // https://discord.com/channels/923308209769426994/1116066363887321199/1256929441053933608
    String brand = new String(data, StandardCharsets.UTF_8);
    // Remove the invalid character at the beginning of the client brand
    if (user.getProtocolVersion().greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_8) && brand.length() > 1) {
      brand = brand.substring(1);
    }
    // Regex pattern for validating client brands
    final Pattern pattern = Sonar.get0().getConfig().getVerification().getBrand().getValidRegex();
    checkState(pattern.matcher(brand).matches(), "client brand does not match pattern: " + brand);
  }
}
