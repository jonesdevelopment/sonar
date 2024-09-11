/*
 * Copyright (C) 2024 Sonar Contributors
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
 *   the boat is removed.
 *   <br>
 *   Now we can check if the player sends the correct position after the entity is removed.
 *   Please note that we don't tell the client to exit the vehicle, therefore, possibly confusing some bots.
 *   Removing the boat results in the player exiting the vehicle on vanilla clients.
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
                                       final boolean forceCAPTCHA) {
    super(user, username);

    this.forceCAPTCHA = forceCAPTCHA;

    // Send the necessary packets to mount the player on the boat vehicle
    user.delayedWrite(spawnBoatEntity);
    user.delayedWrite(setBoatPassengers);
    user.getChannel().flush();
  }

  private final boolean forceCAPTCHA;
  private int expectedTransactionId;
  private int paddlePackets, inputPackets, positionPackets, rotationPackets, vehicleMovePackets;
  private boolean expectMovement, inMinecart, waitingSpawnMinecartTransaction;
  private @NotNull FallbackPacket removeEntitiesPacket = removeBoatEntities;

  private void markSuccess() {
    // Pass the player to the next best verification handler
    if (forceCAPTCHA || Sonar.get().getFallback().shouldPerformCaptcha()) {
      // Either send the player to the CAPTCHA or finish the verification.
      final var decoder = user.getPipeline().get(FallbackPacketDecoder.class);
      // Send the player to the CAPTCHA handler
      decoder.setListener(new FallbackCAPTCHASessionHandler(user, username));
    } else {
      // The player has passed all checks
      finishVerification();
    }
  }

  // We can abuse the entity remove mechanic and check for position packets when the entity dies
  private void handleMovement(double y) {
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
      y -= 1.62f; // Account for 1.7 bounding box
    }
    // Check the Y position of the player
    checkState(y <= IN_AIR_Y_POSITION, "invalid y position");
    // Mark this check as successful if the player sent a few position packets
    if (positionPackets++ > Sonar.get().getConfig().getVerification().getVehicle().getMinimumPackets()) {
      if (user.isGeyser() || inMinecart) {
        markSuccess();
      } else {
        spawnMinecart();
      }
    }
  }

  private void handleRotation() {
    // Once the player sent enough packets, go to the next stage
    final int minimumPackets = Sonar.get().getConfig().getVerification().getVehicle().getMinimumPackets();
    if (paddlePackets > minimumPackets
      && inputPackets > minimumPackets
      && rotationPackets > minimumPackets
      && vehicleMovePackets > minimumPackets) {
      // Remove the entity
      // The next y coordinate the player will send is going
      // to be the vehicle spawn position (y 64 in this case).
      user.write(removeEntitiesPacket);
      // Listen for next movement packet(s)
      expectMovement = true;
    }
    rotationPackets++;
  }

  private void spawnMinecart() {
    expectMovement = false;
    paddlePackets = inputPackets = positionPackets = rotationPackets = 0;
    waitingSpawnMinecartTransaction = true;
    expectedTransactionId = (short) -RANDOM.nextInt(Short.MAX_VALUE);
    // Use transaction packets to confirm that the entity has spawned
    user.delayedWrite(new TransactionPacket(0, expectedTransactionId, false));
    user.delayedWrite(spawnMinecartEntity);
    user.delayedWrite(setMinecartPassengers);
    user.getChannel().flush();
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof TransactionPacket) {
      // Waiting for transaction to make sure the player is riding on the minecart and not on the boat
      if (waitingSpawnMinecartTransaction) {
        final TransactionPacket transaction = (TransactionPacket) packet;
        // Make sure random transactions aren't counted
        checkState(expectedTransactionId <= 0, "unexpected transaction");
        // Make sure the transaction was accepted
        // This must - by vanilla protocol - always be accepted
        checkState(transaction.isAccepted(), "didn't accept transaction");
        // Also check if the transaction ID matches the expected ID
        checkState(expectedTransactionId == transaction.getTransactionId(),
          "expected T ID " + expectedTransactionId + ", but got " + transaction.getTransactionId());

        waitingSpawnMinecartTransaction = false;
        expectedTransactionId = 0;
        removeEntitiesPacket = removeMinecartEntities;
        inMinecart = true;
      }
    } else if (packet instanceof SetPlayerPositionRotationPacket) {
      if (expectMovement) {
        final SetPlayerPositionRotationPacket posRot = (SetPlayerPositionRotationPacket) packet;
        handleMovement(posRot.getY());
      }
    } else if (packet instanceof SetPlayerPositionPacket) {
      if (expectMovement) {
        final SetPlayerPositionPacket position = (SetPlayerPositionPacket) packet;
        handleMovement(position.getY());
      }
    } else if (packet instanceof SetPlayerRotationPacket) {
      if (!expectMovement && !user.isGeyser()) {
        handleRotation();
      }
    } else if (packet instanceof PaddleBoatPacket) {
      checkState(!inMinecart, "invalid packet order (unexpected PaddleBoatPacket)");
      paddlePackets++;
    } else if (packet instanceof VehicleMovePacket) {
      checkState(!inMinecart, "invalid packet order (unexpected VehicleMovePacket)");
      vehicleMovePackets++;
    } else if (packet instanceof PlayerInputPacket && !expectMovement) {
      final PlayerInputPacket playerInput = (PlayerInputPacket) packet;

      // Check if the player is sending invalid vehicle speed values
      final float forward = Math.abs(playerInput.getForward());
      final float sideways = Math.abs(playerInput.getSideways());
      final float maxVehicleSpeed = user.isGeyser() ? 1 : 0.98f;
      checkState(forward <= maxVehicleSpeed, "illegal speed (f): " + forward);
      checkState(sideways <= maxVehicleSpeed, "illegal speed (s): " + sideways);

      // Only mark this packet as correct if the player is not moving the vehicle
      if (playerInput.isJump() || playerInput.isUnmount()) {
        return;
      }

      // Bedrock users do not send SetPlayerPositionRotation and SetPlayerRotation packets
      // Don't ask me why; Microsoft is doing some *fascinating* things with Bedrock...
      if (user.isGeyser()) {
        handleRotation();
      } else {
        checkState(rotationPackets >= inputPackets,
          "illegal packet order; r/i " + rotationPackets + "/" + inputPackets);
      }

      // 1.8 and below do not have PaddleBoat packets, so we simply exempt them from the PaddleBoat check.
      // Clients don't send PaddleBoat & VehicleMovePacket packets while riding minecarts.
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_9) < 0 || inMinecart) {
        paddlePackets++;
        vehicleMovePackets++;
      } else {
        checkState(paddlePackets >= inputPackets,
          "illegal packet order; i/p " + inputPackets + "/" + paddlePackets);
        checkState(vehicleMovePackets == inputPackets,
          "illegal packet order; i/v " + inputPackets + "/" + vehicleMovePackets);
      }
      inputPackets++;
    }
  }
}
