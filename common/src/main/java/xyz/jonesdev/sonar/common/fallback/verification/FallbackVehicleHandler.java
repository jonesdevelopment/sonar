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
import org.jetbrains.annotations.Nullable;
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
  private int checkedRound;
  private double boatMotion, boatY = IN_AIR_Y_POSITION;
  private @Nullable ExpectVehiclePacket expectVehiclePacket = null;

  @RequiredArgsConstructor
  private enum State {
    WAITING(false),
    IN_BOAT(true),
    IN_AIR_AFTER_BOAT(false),
    IN_MINECART(true),
    IN_AIR_AFTER_MINECART(false);

    private final boolean inVehicle;
  }

  private enum ExpectVehiclePacket {
    PADDLE_BOAT,
    PLAYER_ROTATION,
    PLAYER_INPUT,
    VEHICLE_MOVE,
    CLIENT_TICK_END;

    public @NotNull String formattedName() {
      return name().toLowerCase().replace('_', ' ');
    }
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
      if (!state.inVehicle) {
        expectVehiclePacket = null;
      }
    } else if (packet instanceof PaddleBoatPacket) {
      onPaddleBoat((PaddleBoatPacket) packet);
    } else if (packet instanceof VehicleMovePacket) {
      onVehicleMove((VehicleMovePacket) packet);
    } else if (packet instanceof SetPlayerRotationPacket) {
      onPlayerRotation((SetPlayerRotationPacket) packet);
    } else if (packet instanceof PlayerInputPacket) {
      onPlayerInput((PlayerInputPacket) packet);
    } else if (packet instanceof ClientTickEndPacket) {
      onClientTickEnd((ClientTickEndPacket) packet);
    } else if (packet instanceof SetPlayerPositionRotationPacket) {
      final SetPlayerPositionRotationPacket posRot = (SetPlayerPositionRotationPacket) packet;
      handleMovement(posRot.getY(), posRot.isOnGround());
    } else if (packet instanceof SetPlayerPositionPacket) {
      final SetPlayerPositionPacket position = (SetPlayerPositionPacket) packet;
      handleMovement(position.getY(), position.isOnGround());
    }
  }

  private void spawnVehicle(final @NotNull State nextState) {
    user.delayedWrite(nextState == State.IN_BOAT ? SPAWN_BOAT_ENTITY : SPAWN_MINECART_ENTITY);
    user.delayedWrite(SET_VEHICLE_PASSENGERS);
    prepareForNextState(nextState);
    expectVehiclePacket = isClientMovingVehicle() ? ExpectVehiclePacket.PADDLE_BOAT : ExpectVehiclePacket.PLAYER_ROTATION;
  }

  private void prepareForNextState(final @NotNull State nextState) {
    this.nextState = nextState;
    waitingForStateChange = true;
    expectedKeepAliveId = RANDOM.nextInt();
    checkedRound = 0;
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
    if (state == State.WAITING) {
      return;
    }
    if (!state.inVehicle && !waitingForStateChange) {
      expectVehiclePacket = null;

      // Make sure the ground state and y position are correct
      checkState(y <= boatY, "invalid y: " + y);
      checkState(!isOnGround, "invalid ground state: " + y);

      if (state == State.IN_AIR_AFTER_BOAT) {
        spawnVehicle(State.IN_MINECART);
      } else {
        markSuccess();
      }
    }
  }

  private void onPaddleBoat(final @NotNull PaddleBoatPacket packet) {
    checkState(isClientMovingVehicle(), "received unexpected paddle boat");
    checkPacketOrder(ExpectVehiclePacket.PADDLE_BOAT);
    expectVehiclePacket = ExpectVehiclePacket.PLAYER_ROTATION;
  }

  private void onPlayerRotation(final @NotNull SetPlayerRotationPacket packet) {
    checkPacketOrder(ExpectVehiclePacket.PLAYER_ROTATION);

    // Check yaw/pitch?

    if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      expectVehiclePacket = ExpectVehiclePacket.PLAYER_INPUT;
    } else if (isClientMovingVehicle()) {
      expectVehiclePacket = ExpectVehiclePacket.VEHICLE_MOVE;
    } else {
      expectVehiclePacket = ExpectVehiclePacket.CLIENT_TICK_END;
    }
  }

  private void onPlayerInput(final @NotNull PlayerInputPacket packet) {
    // 1.21.2+ send PlayerInput packets when the player starts sprinting, sneaking, etc.
    if (user.getProtocolVersion().greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
      return;
    }
    checkPacketOrder(ExpectVehiclePacket.PLAYER_INPUT);

    // Check if the player is sending invalid vehicle speed values
    final float forward = Math.abs(packet.getForward());
    final float sideways = Math.abs(packet.getSideways());
    final float maxVehicleSpeed = /*user.isGeyser() ? 1 :*/ 0.98f;
    checkState(forward <= maxVehicleSpeed, "illegal speed (f): " + forward);
    checkState(sideways <= maxVehicleSpeed, "illegal speed (s): " + sideways);

    if (isClientMovingVehicle()) {
      expectVehiclePacket = ExpectVehiclePacket.VEHICLE_MOVE;
    } else {
      finishRound();
    }
  }

  private void onVehicleMove(final @NotNull VehicleMovePacket packet) {
    checkState(isClientMovingVehicle(), "received unexpected vehicle move");
    checkPacketOrder(ExpectVehiclePacket.VEHICLE_MOVE);

    // Check the Y position of the vehicle
    checkState(packet.getY() <= IN_AIR_Y_POSITION, "bad vehicle y: " + packet.getY());

    // Check the gravity of the vehicle
    final double lastBoatMotion = boatMotion;
    final double lastBoatY = boatY;
    boatY = packet.getY();
    boatMotion = boatY - lastBoatY;
    final double predicted = lastBoatMotion - 0.03999999910593033D;
    final double difference = Math.abs(boatMotion - predicted);
    // Check if the difference between the predicted and actual motion is too large
    checkState(difference < 1e-7, "bad vehicle gravity: " + predicted + "/" + boatMotion);

    if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      this.finishRound();
    } else {
      expectVehiclePacket = ExpectVehiclePacket.CLIENT_TICK_END;
    }
  }

  private void onClientTickEnd(final @NotNull ClientTickEndPacket packet) {
    if (expectVehiclePacket == ExpectVehiclePacket.CLIENT_TICK_END) {
      finishRound();
    } else if (!waitingForStateChange && state.inVehicle) {
      // Client can send end tick while not in vehicle
      checkPacketOrder(ExpectVehiclePacket.CLIENT_TICK_END);
    }
  }

  private void finishRound() {
    final int minimumPackets = Sonar.get0().getConfig().getVerification().getVehicle().getMinimumPackets();
    if (++checkedRound > minimumPackets) {
      // Move on to the next stage
      expectVehiclePacket = null;
      user.delayedWrite(REMOVE_VEHICLE);
      prepareForNextState(state == State.IN_BOAT ? State.IN_AIR_AFTER_BOAT : State.IN_AIR_AFTER_MINECART);
    } else {
      expectVehiclePacket = isClientMovingVehicle() ? ExpectVehiclePacket.PADDLE_BOAT : ExpectVehiclePacket.PLAYER_ROTATION;
    }
  }

  private boolean isClientMovingVehicle() {
    return (waitingForStateChange && nextState.inVehicle ? nextState : state) == State.IN_BOAT
      && user.getProtocolVersion().greaterThan(ProtocolVersion.MINECRAFT_1_8);
  }

  private void checkPacketOrder(final @NotNull ExpectVehiclePacket current) {
    checkState(expectVehiclePacket == current, expectVehiclePacket == null
      ? "sending vehicle packet while not in vehicle (" + current.formattedName() + ")"
      : "expected " + expectVehiclePacket.formattedName() + " but got " + current.formattedName());
  }
}
