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
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.entity.EntityType;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;

import java.util.UUID;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_9;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

/**
 * Flow for this session handler
 *
 * <li>
 *   {@link SpawnEntityPacket} and {@link SetPassengersPacket} packets are sent to the client,
 *   therefore, making the player enter a boat.
 *   <br>
 *   If the user is on Minecraft: Java Edition, the boat is spawned at Y -63.5 since the client
 *   automatically "kills"/destroys entities when the Y coordinate is lower than -64.
 *   We can abuse this mechanic to create a simple check that makes sure the player automatically
 *   exists the vehicle after falling for some ticks.
 *   <br>
 *   See more: {@link FallbackVehicleSessionHandler}
 * </li>
 * <li>
 *   Then, we listen for incoming {@link PlayerInputPacket} and {@link PaddleBoatPacket} packets.
 * </li>
 */
public final class FallbackVehicleSessionHandler extends FallbackSessionHandler {

  public FallbackVehicleSessionHandler(final @NotNull FallbackUser user,
                                       final @NotNull String username,
                                       final @NotNull UUID uuid,
                                       final boolean forceCAPTCHA) {
    super(user, username, uuid);

    this.forceCAPTCHA = forceCAPTCHA;

    // Send the necessary packets to mount the player on the vehicle
    user.delayedWrite(new SpawnEntityPacket(VEHICLE_ENTITY_ID, EntityType.BOAT,
      SPAWN_X_POSITION, user.isGeyser() ? DEFAULT_Y_COLLIDE_POSITION : -63.5, SPAWN_Z_POSITION));
    user.delayedWrite(setPassengers);
    user.getChannel().flush();
  }

  private static final int MINIMUM_PACKETS = 3;

  private final boolean forceCAPTCHA;
  private int paddlePackets, inputPackets;

  private void markSuccess() {
    // Pass the player to the next best verification handler
    if (forceCAPTCHA || Sonar.get().getFallback().shouldPerformCaptcha()) {
      // Make sure the player exits the vehicle before sending the CAPTCHA
      // We only need to do this for Geyser players since Java players delete
      // the entity on the client-side.
      if (user.isGeyser()) {
        user.delayedWrite(removeEntities);
      }
      // Either send the player to the CAPTCHA or finish the verification.
      final var decoder = (FallbackPacketDecoder) user.getPipeline().get(FallbackPacketDecoder.class);
      // Send the player to the CAPTCHA handler
      decoder.setListener(new FallbackCAPTCHASessionHandler(user, username, uuid));
    } else {
      // The player has passed all checks
      finishVerification();
    }
  }

  // The entity will die when the y coordinate of it is <64
  // We can abuse this mechanic and check for position packets when the entity dies
  private void move(double y) {
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
      y -= 1.62f; // Account for 1.7 bounding box
    }
    // Check the Y position of the player
    checkState(y < -64, "invalid y position");
    // According to the boat motion, the number of packets sent by the client is 3 (minimum)
    checkState(inputPackets >= MINIMUM_PACKETS, "invalid input packet count " + inputPackets);
    checkState(paddlePackets >= MINIMUM_PACKETS, "invalid paddle packet count " + paddlePackets);
    // Mark this check as successful
    markSuccess();
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof SetPlayerPositionRotationPacket) {
      if (!user.isGeyser()) {
        final SetPlayerPositionRotationPacket posRot = (SetPlayerPositionRotationPacket) packet;
        move(posRot.getY());
      }
    } else if (packet instanceof SetPlayerPositionPacket) {
      if (!user.isGeyser()) {
        final SetPlayerPositionPacket position = (SetPlayerPositionPacket) packet;
        move(position.getY());
      }
    } else if (packet instanceof PaddleBoatPacket) {
      paddlePackets++;
    } else if (packet instanceof PlayerInputPacket) {
      final PlayerInputPacket playerInput = (PlayerInputPacket) packet;

      // Check if the player is sending invalid vehicle speed values
      checkState(Math.abs(playerInput.getForward()) <= 0.98f, "illegal vehicle speed (f)");
      checkState(Math.abs(playerInput.getSideways()) <= 0.98f, "illegal vehicle speed (s)");

      // Only mark this packet as correct if the player is not moving the vehicle
      if (playerInput.isJump() || playerInput.isUnmount()) {
        return;
      }

      // Geyser users handle entities differently; exempt them from the extra check
      if (user.isGeyser()
        && paddlePackets >= MINIMUM_PACKETS
        && inputPackets >= MINIMUM_PACKETS) {
        markSuccess();
        return;
      }

      // 1.8 and below do not have PaddleBoat packets,
      // so we simply exempt them from the PaddleBoat check.
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_9) < 0) {
        paddlePackets++;
      }
      inputPackets++;
    }
  }
}
