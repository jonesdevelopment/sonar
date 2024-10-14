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

package xyz.jonesdev.sonar.common.fallback.verification;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackVehicleHandler extends FallbackVerificationHandler {

  public FallbackVehicleHandler(final @NotNull FallbackUser user) {
    super(user);

    this.canTeleportPlayer = user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_15_2)
      || user.getProtocolVersion().greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_17);

    // Send the necessary packets to mount the player on the boat vehicle
    user.delayedWrite(SPAWN_BOAT_ENTITY);
    user.delayedWrite(SET_VEHICLE_PASSENGERS);
    user.channel().flush();
  }

  private final boolean canTeleportPlayer;
  private int expectedTransactionId, expectedTeleportId;
  private int paddles, inputs, positions, rotations, vehicleMoves, teleports, confirmed;
  private boolean expectMovement, expectTeleport, inVoid, inMinecart, waitingSpawnMinecart, waitingRemoveMinecart;
  private double minecartMotion, minecartY = IN_AIR_Y_POSITION;

  private void markSuccess() {
    // Pass the player to the next best verification handler
    if (user.isForceCaptcha() || Sonar.get().getFallback().shouldPerformCaptcha()) {
      // Send the player to the CAPTCHA handler
      user.channel().pipeline().get(FallbackPacketDecoder.class).setListener(new FallbackCaptchaHandler(user));
    } else {
      // The player has passed all checks
      finishVerification();
    }
  }

  // We can abuse the entity remove mechanic and check for position packets when the entity dies
  private void handleMovement(final double y, final boolean onGround) {
    // The player cannot be on the ground if the Y position is less than 0
    checkState(!onGround || !inVoid || waitingRemoveMinecart, "invalid ground state: " + y);
    // Check the Y position of the player
    final double minimumY = inVoid ? IN_VOID_Y_POSITION : IN_AIR_Y_POSITION;
    checkState(y <= minimumY, "illegal y position: " + y + "/" + minimumY);

    if (!waitingRemoveMinecart && !waitingSpawnMinecart
      && positions++ > Sonar.get().getConfig().getVerification().getVehicle().getMinimumPackets()) {
      if (user.isGeyser() || inVoid) {
        // Mark this check as successful if the player sent a few position packets
        markSuccess();
      } else if (!inMinecart) {
        // Continue checking the player by using Minecarts
        spawnMinecart();
      }
    }
  }

  private void handleRotation() {
    // Once the player sent enough packets, go to the next stage
    final int minimumPackets = Sonar.get().getConfig().getVerification().getVehicle().getMinimumPackets();
    if (paddles > minimumPackets && inputs > minimumPackets
      && rotations > minimumPackets && vehicleMoves > minimumPackets
      && (!canTeleportPlayer || (teleports > minimumPackets && confirmed > minimumPackets))) {
      // Check if the player has confirmed all teleport packets
      checkState(confirmed == teleports,
        "missing teleport confirmations: " + confirmed + "/" + teleports);
      // If the player is riding a Minecart, teleport the entity to < -100
      // to see how the player reacts to the invalid position of the entity.
      if (inMinecart) {
        // We're using transactions to avoid false positives with lag
        expectedTransactionId = (short) -RANDOM.nextInt(Short.MAX_VALUE);
        user.delayedWrite(TELEPORT_VEHICLE);
        user.delayedWrite(new TransactionPacket(0, expectedTransactionId, false));
        user.channel().flush();
        waitingRemoveMinecart = true;
      } else {
        // If the player is riding a boat, remove the entity
        // The next Y coordinate the player will send is going
        // to be the vehicle spawn position (64 in this case).
        user.write(REMOVE_VEHICLE);
      }
      // Listen for next movement packet(s)
      expectMovement = true;
    }
    rotations++;
  }

  private void spawnMinecart() {
    expectMovement = false;
    paddles = inputs = positions = rotations = 0;
    // Use transaction packets to confirm that the entity has spawned
    minecartMotion = 0.03999999910593033D * 2 * -1;
    expectedTransactionId = (short) -RANDOM.nextInt(Short.MAX_VALUE);
    user.delayedWrite(new TransactionPacket(0, expectedTransactionId, false));
    user.delayedWrite(SPAWN_MINECART_ENTITY);
    user.delayedWrite(SET_VEHICLE_PASSENGERS);
    user.channel().flush();
    waitingSpawnMinecart = true;
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof TransactionPacket) {
      final TransactionPacket transaction = (TransactionPacket) packet;
      // Make sure the window ID is valid
      checkState(transaction.getWindowId() == 0, "wrong window: " + transaction.getWindowId());
      // Make sure the transaction was accepted
      // This must - by vanilla protocol - always be accepted
      checkState(transaction.isAccepted(), "didn't accept transaction");
      // Also check if the transaction ID matches the expected ID
      checkState(expectedTransactionId == transaction.getTransactionId(),
        "expected T ID " + expectedTransactionId + ", but got " + transaction.getTransactionId());

      // We are lag compensating the Minecart to avoid false positives
      if (waitingSpawnMinecart) {
        waitingSpawnMinecart = false;
        inMinecart = true;
      } else if (waitingRemoveMinecart) {
        waitingRemoveMinecart = inMinecart = false;
        inVoid = true;
        positions = 0;
      }

      expectedTransactionId = 0;
    } else if (packet instanceof SetPlayerPositionRotationPacket) {
      final SetPlayerPositionRotationPacket posRot = (SetPlayerPositionRotationPacket) packet;

      if (expectTeleport) {
        checkState(!posRot.isOnGround(), "invalid ground state on teleport " + teleports);
        teleports++;
        // Expect 1.9+ players to send a ConfirmTeleportation packet after the teleport
        if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_9)) {
          confirmed++;
        }
        expectTeleport = false;
      } else if (expectMovement) {
        handleMovement(posRot.getY(), posRot.isOnGround());
      }
    } else if (packet instanceof ConfirmTeleportationPacket) {
      if (!expectMovement) {
        final ConfirmTeleportationPacket teleportConfirm = (ConfirmTeleportationPacket) packet;

        // Check if the teleport ID matches the expected ID
        checkState(teleportConfirm.getTeleportId() == expectedTeleportId,
          "expected TP ID " + expectedTeleportId + ", but got " + teleportConfirm.getTeleportId());

        confirmed++;
      }
    } else if (packet instanceof SetPlayerPositionPacket) {
      if (expectMovement) {
        final SetPlayerPositionPacket position = (SetPlayerPositionPacket) packet;
        handleMovement(position.getY(), position.isOnGround());
      }
    } else if (packet instanceof SetPlayerRotationPacket) {
      if (!expectMovement && !user.isGeyser()) {
        handleRotation();
      }
    } else if (packet instanceof PaddleBoatPacket) {
      checkState(!inMinecart, "invalid packet order (unexpected PaddleBoatPacket)");
      paddles++;
    } else if (packet instanceof VehicleMovePacket) {
      checkState(!inMinecart, "invalid packet order (unexpected VehicleMovePacket)");
      final VehicleMovePacket vehicleMove = (VehicleMovePacket) packet;
      if (!user.isGeyser()) {
        // Check the Y position of the vehicle
        checkState(vehicleMove.getY() <= IN_AIR_Y_POSITION,
          "invalid vehicle y position: " + vehicleMove.getY());
        // Predict the Y motion of the minecart
        final double lastMinecartMotion = minecartMotion;
        final double lastMinecartY = minecartY;
        minecartY = vehicleMove.getY();
        minecartMotion = minecartY - lastMinecartY;
        final double predicted = lastMinecartMotion - 0.03999999910593033D;
        final double difference = Math.abs(minecartMotion - predicted);
        // Check if the difference between the predicted and actual motion is too large
        checkState(difference < 1e-7,
          "invalid vehicle gravity: " + predicted + "/" + minecartMotion + "/" + difference);
      }

      // 1.21.2+ do not send PlayerInput packets when inside a vehicle
      if (user.getProtocolVersion().greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
        handlePlayerInput();
      }
      vehicleMoves++;
    } else if (packet instanceof PlayerInputPacket) {
      // 1.21.2+ send PlayerInput packets when the player starts sprinting, sneaking, etc.
      if (!expectMovement && user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        final PlayerInputPacket playerInput = (PlayerInputPacket) packet;

        // Check if the player is sending invalid vehicle speed values
        final float forward = Math.abs(playerInput.getForward());
        final float sideways = Math.abs(playerInput.getSideways());
        final float maxVehicleSpeed = user.isGeyser() ? 1 : 0.98f;
        checkState(forward <= maxVehicleSpeed, "illegal speed (f): " + forward);
        checkState(sideways <= maxVehicleSpeed, "illegal speed (s): " + sideways);

        // Only mark this packet as correct if the player is not moving the vehicle
        if (playerInput.isJump() || playerInput.isSneak()) {
          return;
        }

        handlePlayerInput();
      }
    }
  }

  private void handlePlayerInput() {
    // Bedrock users do not send SetPlayerPositionRotation and SetPlayerRotation packets
    // Don't ask me why; Microsoft is doing some *fascinating* things with Bedrock...
    if (user.isGeyser()) {
      handleRotation();
    } else {
      checkState(rotations >= inputs,
        "illegal packet order; r/i " + rotations + "/" + inputs);

      // This check does not work on 1.16-1.16.5 because Mojang did some stuff
      if (canTeleportPlayer && !expectTeleport) {
        // Teleport the player to see if they are bugged to the vehicle
        expectedTeleportId = RANDOM.nextInt();
        user.write(new SetPlayerPositionRotationPacket(
          SPAWN_X_POSITION, Short.MAX_VALUE + RANDOM.nextInt(Short.MAX_VALUE), SPAWN_Z_POSITION,
          0, -90, expectedTeleportId, 0, false, false));
        expectTeleport = true;
      }
    }

    // 1.8 and below do not have PaddleBoat packets, so we simply exempt them from the PaddleBoat check.
    // Clients don't send PaddleBoat & VehicleMovePacket packets while riding minecarts.
    if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_9) || inMinecart) {
      paddles++;
      vehicleMoves++;
    } else if (!user.isGeyser()) {
      checkState(paddles >= inputs,
        "illegal packet order; i/p " + inputs + "/" + paddles);
      checkState(vehicleMoves >= inputs,
        "illegal packet order; i/v " + inputs + "/" + vehicleMoves);
    }
    inputs++;
  }
}
