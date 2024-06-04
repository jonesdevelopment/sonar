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

import lombok.val;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.entity.EntityType;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.PaddleBoatPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.PlayerInputPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.SpawnEntityPacket;

import java.util.UUID;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_9;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.VEHICLE_ENTITY_ID;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.setPassengers;

public final class FallbackVehicleSessionHandler extends FallbackSessionHandler {

  public FallbackVehicleSessionHandler(final @NotNull FallbackUser user,
                                       final @NotNull String username,
                                       final @NotNull UUID uuid,
                                       final double x, final double y, final double z) {
    super(user, username, uuid);

    // Send the necessary packets to mount the player on the vehicle
    final int vehicleType = EntityType.BOAT.getId(user.getProtocolVersion());
    user.delayedWrite(new SpawnEntityPacket(VEHICLE_ENTITY_ID, vehicleType, x, y, z));
    user.delayedWrite(setPassengers);
    user.getChannel().flush();
  }

  private boolean receivedPaddle, receivedInput, dropFurther;

  private void markSuccess() {
    // Force-stop the check
    dropFurther = true;

    // Either send the player to the CAPTCHA, or finish the verification.
    val decoder = (FallbackPacketDecoder) user.getChannel().pipeline().get(FallbackPacketDecoder.class);
    // Pass the player to the next best verification handler
    if (Sonar.get().getFallback().shouldPerformCaptcha()) {
      decoder.setListener(new FallbackCAPTCHASessionHandler(user, username, uuid));
    } else {
      // The player has passed all checks
      finishVerification();
    }
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof PaddleBoatPacket) {
      // Mark the PaddleBoat packet as received
      receivedPaddle = true;
    } else if (packet instanceof PlayerInputPacket) {
      final PlayerInputPacket playerInput = (PlayerInputPacket) packet;

      // Check if the player is sending invalid vehicle speed values
      checkState(Math.abs(playerInput.getForward()) <= 0.98f, "illegal vehicle speed (f)");
      checkState(Math.abs(playerInput.getSideways()) <= 0.98f, "illegal vehicle speed (s)");

      // Only mark this packet as correct if the player is not moving the vehicle
      if (playerInput.isJump() || playerInput.isUnmount()) {
        return;
      }

      // Mark this check as success if the player has sent
      // two PlayerInput and a minimum of one PaddleBoat packets.
      if (receivedInput && receivedPaddle && !dropFurther) {
        markSuccess();
      }

      // 1.8 and below do not have PaddleBoat packets,
      // so we simply exempt them from the PaddleBoat check.
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_9) < 0) {
        receivedPaddle = true;
      }
      // Mark the PlayerInput packet as received
      receivedInput = true;
    } else {
      // Fail the verification if the user sends any other packets
      user.fail("unexpected packet " + packet.getClass().getCanonicalName());
    }
  }
}
