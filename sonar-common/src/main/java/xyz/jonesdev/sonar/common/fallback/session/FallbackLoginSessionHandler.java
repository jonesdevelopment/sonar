/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.session;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketEncoder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration.FinishConfigurationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginAcknowledgedPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.ClientInformationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.KeepAlivePacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.PluginMessagePacket;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

/**
 * Flow for this session handler
 *
 * <li>
 *   A {@link KeepAlivePacket} packet is sent to the client*
 *   <br>
 *   See more: {@link FallbackLoginSessionHandler#initialize18()}
 * </li>
 * <li>
 *   After the KeepAlive packet is validated, the client enters the configuration phase.**
 * </li>
 * <li>
 *   Then, the session handler is set to the {@link FallbackGravitySessionHandler}.
 *   <br>
 *   See more: {@link FallbackLoginSessionHandler#markSuccess()}
 * </li>
 * <br>
 * * The KeepAlive check is skipped on 1.7, as KeepAlive packets don't exist during the LOGIN state.
 * <br>
 * ** The internals of the configuration phase were not mentioned.
 * Find out more about the client configuration and registry synchronization:
 * {@link #synchronizeClientRegistry()}, {@link #updateEncoderDecoderState(FallbackPacketRegistry)}
 */
public final class FallbackLoginSessionHandler extends FallbackSessionHandler {

  public FallbackLoginSessionHandler(final @NotNull FallbackUser user,
                                     final @NotNull String username,
                                     final @NotNull UUID uuid) {
    super(user, username, uuid);

    // Start initializing the actual join process for pre-1.20.2 clients
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_20_2) < 0) {
      // This trick helps in reducing unnecessary outgoing server traffic
      // by avoiding sending other packets to clients that are potentially bots.
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
        user.getChannel().eventLoop().schedule(this::markSuccess, 100L, TimeUnit.MILLISECONDS);
      } else {
        initialize18();
      }
    }
  }

  private boolean acknowledgedLogin;
  private int expectedKeepAliveId;

  /**
   * The purpose of these KeepAlive packets is to confirm that the connection
   * is active and legitimate, thereby preventing bot connections that
   * could flood the server with login attempts and other unwanted traffic.
   */
  private void initialize18() {
    // Send a KeepAlive packet with a random ID
    expectedKeepAliveId = RANDOM.nextInt();
    user.write(new KeepAlivePacket(expectedKeepAliveId));
  }

  private void markSuccess() {
    // Make sure we can actually switch over to the next check
    updateEncoderDecoderState(FallbackPacketRegistry.GAME);
    // Pass the player to the next verification handler
    final FallbackGravitySessionHandler gravitySessionHandler = new FallbackGravitySessionHandler(user, username, uuid);
    final var decoder = (FallbackPacketDecoder) user.getPipeline().get(FallbackPacketDecoder.class);
    decoder.setListener(gravitySessionHandler);
  }

  private void markAcknowledged() {
    acknowledgedLogin = true;

    synchronizeClientRegistry();
    // Write the FinishConfiguration packet to the buffer
    user.delayedWrite(FINISH_CONFIGURATION);
    // Send all packets in one flush
    user.getChannel().flush();
    // Set decoder state to actually catch all packets
    updateEncoderDecoderState(FallbackPacketRegistry.CONFIG);
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof KeepAlivePacket) {
      // This is the first packet we expect from the client
      final KeepAlivePacket keepAlive = (KeepAlivePacket) packet;

      // Check if the KeepAlive ID matches the expected ID
      final long keepAliveId = keepAlive.getId();
      checkState(keepAliveId == expectedKeepAliveId,
        "expected K ID " + expectedKeepAliveId + " but got " + keepAliveId);

      // 1.8 clients send KeepAlive packets with the ID 0 every second
      // while the player is in the "Downloading terrain" screen.
      expectedKeepAliveId = 0;

      // Spawn the player in the virtual world if the client does not need
      // any configuration (pre-1.20.2).
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_20_2) < 0) {
        markSuccess();
      }
    } else if (packet instanceof LoginAcknowledgedPacket) {
      // Prevent users from sending multiple LoginAcknowledged packets
      checkState(!acknowledgedLogin, "sent duplicate login ack");
      markAcknowledged();
    } else if (packet instanceof FinishConfigurationPacket) {
      markSuccess();
    }
    // Make sure to catch all ClientSettings and PluginMessage packets during the configuration phase.
    else if (packet instanceof ClientInformationPacket) {
      // Let the session handler itself know about this packet
      checkClientInformation((ClientInformationPacket) packet);
    } else if (packet instanceof PluginMessagePacket) {
      // Let the session handler itself know about this packet
      checkPluginMessage((PluginMessagePacket) packet);
    }
  }

  private void updateEncoderDecoderState(final @NotNull FallbackPacketRegistry registry) {
    final var decoder = (FallbackPacketDecoder) user.getPipeline().get(FallbackPacketDecoder.class);
    final var encoder = (FallbackPacketEncoder) user.getPipeline().get(FallbackPacketEncoder.class);
    // Update the packet registry state in the encoder and decoder pipelines
    decoder.updateRegistry(registry);
    encoder.updateRegistry(registry);
  }

  private void synchronizeClientRegistry() {
    // 1.20.5+ adds new "game bundle features" which overcomplicate all of this...
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_20_5) >= 0) {
      // Write the new RegistrySync packets to the buffer
      for (final FallbackPacket syncPacket : user.getProtocolVersion().compareTo(MINECRAFT_1_21) < 0
        ? REGISTRY_SYNC_1_20_5 : REGISTRY_SYNC_1_21) {
        user.delayedWrite(syncPacket);
      }
    } else {
      // Write the old RegistrySync packet to the buffer
      user.delayedWrite(REGISTRY_SYNC_LEGACY);
    }
  }
}
