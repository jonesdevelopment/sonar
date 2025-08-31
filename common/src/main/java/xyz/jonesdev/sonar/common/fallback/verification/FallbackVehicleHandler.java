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

import lombok.RequiredArgsConstructor;
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

    spawnVehicle(State.IN_BOAT);
  }

  private boolean waitingForStateChange;
  private State state = State.WAITING;
  private State nextState;
  private int expectedKeepAliveId;
  private int rotations, inputs, paddles, vehicleMoves;
  private double boatMotion, boatY = IN_AIR_Y_POSITION;

  @RequiredArgsConstructor
  private enum State {
    WAITING(false),
    IN_BOAT(true),
    IN_AIR_AFTER_BOAT(false),
    IN_MINECART(true),
    IN_AIR_AFTER_MINECART(false);

    private final boolean inVehicle;
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof KeepAlivePacket) {
      final KeepAlivePacket keepAlivePacket = (KeepAlivePacket) packet;

      // Check if we are expecting a KeepAlive packet
      checkState(nextState != null, "invalid packet timing");
      // Also check if the KeepAlive ID matches the expected ID
      checkState(keepAlivePacket.getId() == expectedKeepAliveId,
        "expected K ID " + expectedKeepAliveId + ", but got " + keepAlivePacket.getId());

      state = nextState;
      nextState = null;
      waitingForStateChange = false;
    } else if (!waitingForStateChange) {
      if (packet instanceof PaddleBoatPacket) {
        if (state == State.IN_BOAT) {
          paddles++;
        }
      } else if (packet instanceof VehicleMovePacket) {
        if (state == State.IN_BOAT) {
          final VehicleMovePacket vehicleMove = (VehicleMovePacket) packet;
          // Check the Y position of the vehicle
          checkState(vehicleMove.getY() <= IN_AIR_Y_POSITION, "bad vehicle y: " + vehicleMove.getY());

          // Check the gravity of the vehicle
          final double lastBoatMotion = boatMotion;
          final double lastBoatY = boatY;
          boatY = vehicleMove.getY();
          boatMotion = boatY - lastBoatY;
          final double predicted = lastBoatMotion - 0.03999999910593033D;
          final double difference = Math.abs(boatMotion - predicted);
          // Check if the difference between the predicted and actual motion is too large
          checkState(difference < 1e-7, "bad vehicle gravity: " + predicted + "/" + boatMotion);

          vehicleMoves++;
        }
      } else if (packet instanceof SetPlayerRotationPacket) {
        if (state.inVehicle) {
          rotations++;

          // 1.21.2+ do not send PlayerInput packets when inside a vehicle.
          // Handle it after SetPlayerRotationPacket to simulate vanilla behavior.
          if (user.getProtocolVersion().greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
            handlePlayerInput();
          }
        }
      } else if (packet instanceof PlayerInputPacket) {
        // 1.21.2+ send PlayerInput packets when the player starts sprinting, sneaking, etc.
        if (state.inVehicle && user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
          final PlayerInputPacket playerInput = (PlayerInputPacket) packet;

          // Check if the player is sending invalid vehicle speed values
          final float forward = Math.abs(playerInput.getForward());
          final float sideways = Math.abs(playerInput.getSideways());
          final float maxVehicleSpeed = /*user.isGeyser() ? 1 :*/ 0.98f;
          checkState(forward <= maxVehicleSpeed, "illegal speed (f): " + forward);
          checkState(sideways <= maxVehicleSpeed, "illegal speed (s): " + sideways);

          handlePlayerInput();
        }
      } else if (packet instanceof SetPlayerPositionRotationPacket) {
        final SetPlayerPositionRotationPacket posRot = (SetPlayerPositionRotationPacket) packet;
        handleMovement(posRot.getY(), posRot.isOnGround());
      } else if (packet instanceof SetPlayerPositionPacket) {
        final SetPlayerPositionPacket position = (SetPlayerPositionPacket) packet;
        handleMovement(position.getY(), position.isOnGround());
      }
    }
  }

  private void spawnVehicle(final @NotNull State nextState) {
    user.delayedWrite(nextState == State.IN_BOAT ? SPAWN_BOAT_ENTITY : SPAWN_MINECART_ENTITY);
    user.delayedWrite(SET_VEHICLE_PASSENGERS);
    prepareForNextState(nextState);
  }

  private void prepareForNextState(final @NotNull State nextState) {
    this.nextState = nextState;
    waitingForStateChange = true;
    expectedKeepAliveId = RANDOM.nextInt();
    rotations = inputs = paddles = vehicleMoves = 0;
    user.delayedWrite(new KeepAlivePacket(expectedKeepAliveId));
    user.channel().flush();
  }

  private void markSuccess() {
    // Pass the player to the next best verification handler
    if (user.isForceCaptcha() || Sonar.get0().getFallback().shouldPerformCaptcha()) {
      user.channel().pipeline().get(FallbackPacketDecoder.class).setListener(new FallbackCaptchaHandler(user));
    } else {
      finishVerification();
    }
  }

  private void handleMovement(final double y, final boolean isOnGround) {
    // Make sure we're currently expecting movement
    if (state.inVehicle || state == State.WAITING) {
      return;
    }

    // Make sure the ground state and y position are correct
    checkState(y <= boatY, "invalid y: " + y);
    checkState(!isOnGround, "invalid ground state: " + y);

    if (state == State.IN_AIR_AFTER_BOAT) {
      spawnVehicle(State.IN_MINECART);
    } else {
      markSuccess();
    }
  }

  private void handlePlayerInput() {
    // 1.8 and below do not have PaddleBoat packets, so we simply exempt them from the PaddleBoat check.
    // Clients also don't send PaddleBoat & VehicleMovePacket packets while riding minecarts.
    if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_9) || state == State.IN_MINECART) {
      paddles++;
      vehicleMoves++;
    }

    // Check for packet order
    checkState(rotations >= inputs,
      "illegal packet order; i/r " + inputs + "/" + rotations);
    checkState(paddles >= inputs,
      "illegal packet order; i/p " + inputs + "/" + paddles);
    checkState(vehicleMoves >= inputs,
      "illegal packet order; i/v " + inputs + "/" + vehicleMoves);

    inputs++;

    // Check if we've received more than the minimum number of packets
    final int minimumPackets = Sonar.get0().getConfig().getVerification().getVehicle().getMinimumPackets();
    if (inputs > minimumPackets && rotations > minimumPackets
      && paddles > minimumPackets && vehicleMoves > minimumPackets) {
      // Move on to the next stage
      user.delayedWrite(REMOVE_VEHICLE);
      prepareForNextState(state == State.IN_BOAT ? State.IN_AIR_AFTER_BOAT : State.IN_AIR_AFTER_MINECART);
    }
  }
}
