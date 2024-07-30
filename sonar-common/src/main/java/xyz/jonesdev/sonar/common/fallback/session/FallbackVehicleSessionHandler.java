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
 *   The boat will be spawned at y 1337.
 *   <br>
 *   After 3 {@link PlayerInputPacket} and {@link PaddleBoatPacket} packets,
 *   the boat is teleported to y -64.
 *   <br>
 *   The boat is spawned at Y -63.5 since the client automatically destroys
 *   entities when the Y coordinate is lower than -64.
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
    user.delayedWrite(spawnEntity);
    user.delayedWrite(setPassengers);
    user.getChannel().flush();
  }

  private final boolean forceCAPTCHA;
  private int paddlePackets, inputPackets, positionPackets;
  private boolean expectMovement;

  private static final int MINIMUM_REQUIRED_PACKETS = 2;

  private void markSuccess() {
    // Pass the player to the next best verification handler
    if (forceCAPTCHA || Sonar.get().getFallback().shouldPerformCaptcha()) {
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
    checkState(y <= IN_AIR_Y_POSITION, "invalid y position");
    // Mark this check as successful if the player sent a few position packets
    if (positionPackets++ > MINIMUM_REQUIRED_PACKETS) {
      markSuccess();
    }
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof SetPlayerPositionRotationPacket) {
      if (expectMovement) {
        final SetPlayerPositionRotationPacket posRot = (SetPlayerPositionRotationPacket) packet;
        move(posRot.getY());
      }
    } else if (packet instanceof SetPlayerPositionPacket) {
      if (expectMovement) {
        final SetPlayerPositionPacket position = (SetPlayerPositionPacket) packet;
        move(position.getY());
      }
    } else if (packet instanceof PaddleBoatPacket) {
      paddlePackets++;
    } else if (packet instanceof PlayerInputPacket && !expectMovement) {
      final PlayerInputPacket playerInput = (PlayerInputPacket) packet;

      // Check if the player is sending invalid vehicle speed values
      checkState(Math.abs(playerInput.getForward()) <= 0.98, "illegal vehicle speed (f)");
      checkState(Math.abs(playerInput.getSideways()) <= 0.98, "illegal vehicle speed (s)");

      // Only mark this packet as correct if the player is not moving the vehicle
      if (playerInput.isJump() || playerInput.isUnmount()) {
        return;
      }

      // Once the player sent enough packets, go to the next stage
      if (paddlePackets > MINIMUM_REQUIRED_PACKETS && inputPackets > MINIMUM_REQUIRED_PACKETS) {
        // Teleport the entity to y -64 to and kill the entity
        // The next y coordinate the player will send is going
        // to be the vehicle spawn position (y 64 in this case).
        user.write(removeEntities);
        // Listen for next movement packet(s)
        expectMovement = true;
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
