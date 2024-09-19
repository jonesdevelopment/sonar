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
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;

import static xyz.jonesdev.sonar.api.config.SonarConfiguration.Verification.Gamemode.CREATIVE;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackGravityHandler extends FallbackVerificationHandler {

  public FallbackGravityHandler(final @NotNull FallbackUser user,
                                final @NotNull FallbackPreJoinHandler preJoinHandler) {
    super(user);

    this.preJoinHandler = preJoinHandler;
    // Bedrock users start falling immediately
    this.canFall = user.isGeyser();
    // We don't want to check Geyser players for valid gravity, as this might cause issues because of the protocol
    this.enableGravityCheck = !user.isGeyser() && Sonar.get().getConfig().getVerification().getGravity().isEnabled();
    this.enableCollisionsCheck = !user.isGeyser() && Sonar.get().getConfig().getVerification().getGravity().isCheckCollisions();

    // First, write the JoinGame packet to the buffer
    user.delayedWrite(joinGame);
    // Then, write the ClientAbilities packet to the buffer
    // This is only necessary if the player is in creative mode
    if (Sonar.get().getConfig().getVerification().getGamemode() == CREATIVE) {
      user.delayedWrite(DEFAULT_ABILITIES);
    }
    // Write the DefaultSpawnPosition packet to the buffer
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_19_3) >= 0) {
      user.delayedWrite(defaultSpawnPosition);
    }
    // Teleport the player to the position where we're starting to check them
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
      user.delayedWrite(spawnPosition);
      user.delayedWrite(fallStartPosition);
    } else {
      // 1.7.2-1.7.10 clients do not have relative teleports
      user.delayedWrite(fallStartPositionLegacy);
    }
    // 1.20.3+ introduced game events
    // Make sure the client knows that we're sending chunks next
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_20_3) >= 0) {
      user.delayedWrite(START_WRITING_CHUNKS);
    }
    // Teleport player into an empty world by sending an empty chunk packet
    user.delayedWrite(EMPTY_CHUNK_DATA);
    // Spawn the invisible platform below the player
    if (enableCollisionsCheck) {
      final int index = RANDOM.nextInt(BLOCKS_PACKETS.length);
      blockHeight = POSSIBLE_BLOCK_TYPES[index].getBlockHeight();
      user.delayedWrite(BLOCKS_PACKETS[index]);
    }
    // Send all packets at once
    user.getChannel().flush();

    // 1.8 and below don't have TeleportConfirm packets, which is why we're skipping that check.
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_9) < 0) {
      // Enable the movement checks
      teleported = true;
    }
  }

  private final FallbackPreJoinHandler preJoinHandler;
  private final boolean enableGravityCheck, enableCollisionsCheck;
  private boolean teleported, canFall, checkMovement;
  private double y, deltaY, blockHeight;
  private int movementTick, expectedTeleportId = FIRST_TELEPORT_ID;

  private void markSuccess() {
    // Force-stop the movement checks
    teleported = false;
    // Exempt pre-1.20.2 since they've already passed that check in the configuration phase
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_20_2) < 0) {
      preJoinHandler.checkClientInformation();
    }
    // Send the player to the next verification handler
    final var decoder = user.getPipeline().get(FallbackPacketDecoder.class);
    decoder.setListener(new FallbackProtocolHandler(user));
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof SetPlayerPositionRotationPacket) {
      // Make sure the player has teleported before checking for position packets
      if (teleported) {
        final SetPlayerPositionRotationPacket position = (SetPlayerPositionRotationPacket) packet;
        handleMovement(position.getX(), position.getY(), position.getZ(), position.isOnGround(), true);
      }
    } else if (packet instanceof SetPlayerPositionPacket) {
      // Make sure the player has teleported before checking for position packets
      if (teleported) {
        final SetPlayerPositionPacket position = (SetPlayerPositionPacket) packet;
        handleMovement(position.getX(), position.getY(), position.getZ(), position.isOnGround(), false);
      }
    } else if (packet instanceof ConfirmTeleportationPacket) {
      final ConfirmTeleportationPacket teleportConfirm = (ConfirmTeleportationPacket) packet;

      // Only expect this packet to be sent once
      checkState(!teleported, "duplicate teleport confirm");
      // Check if the teleport ID matches the expected ID
      final int teleportId = teleportConfirm.getTeleportId();
      checkState(teleportId == expectedTeleportId,
        "expected TP ID " + expectedTeleportId + ", but got " + teleportId);

      // The first teleport ID is not useful for us in this context, skip it
      if (expectedTeleportId == FIRST_TELEPORT_ID) {
        expectedTeleportId = SECOND_TELEPORT_ID;
      } else {
        // Enable the movement checks
        teleported = true;
      }
    } else if (packet instanceof ClientInformationPacket
      || packet instanceof PluginMessagePacket) {
      // Pass these packets back to the login handler
      preJoinHandler.handle(packet);
    }
  }

  private void handleMovement(final double x, final double y, final double z,
                              final boolean onGround, final boolean rotated) {
    if (!checkMovement) {
      // No need to continue checking if the gravity and collision checks are disabled
      if (!enableGravityCheck && !enableCollisionsCheck) {
        markSuccess();
        return;
      }

      // Check if the packet has characteristics of a packet after a teleport
      checkState(rotated, "illegal movement packet order");
      checkState(!onGround, "illegal ground state on teleport");
      checkState(x == SPAWN_X_POSITION, "invalid x: " + x);
      checkState(z == SPAWN_Z_POSITION, "invalid z: " + z);

      // Synchronize the Y coordinate
      this.y = dynamicSpawnYPosition;
      checkMovement = true;
      return;
    }

    // Calculate/store all necessary positions
    final double lastDeltaY = this.deltaY;
    final double lastY = this.y;
    this.deltaY = y - lastY;
    this.y = y;

    // Log/debug position if enabled in the configuration
    if (Sonar.get().getConfig().getVerification().isDebugXYZPositions()) {
      Sonar.get().getLogger().info("{}: {}/{}/{} ly={}, dy={}, h={}, g={}, r={}",
        user.getUsername(), x, y, z, lastY, deltaY, blockHeight, onGround, rotated);
    }

    // Ensure that the player's Y coordinate is above the collision platform
    checkState(y >= DEFAULT_Y_COLLIDE_POSITION,
      "fell through blocks: " + y + "/" + deltaY + "/" + movementTick);

    // The player is not allowed to move away from the collision platform.
    // This should not happen unless the max movement tick is configured to a high number.
    checkState(Math.abs(Math.abs(x) - BLOCKS_PER_ROW) < BLOCKS_PER_ROW, "illegal x offset: " + x);
    checkState(Math.abs(Math.abs(z) - BLOCKS_PER_ROW) < BLOCKS_PER_ROW, "illegal z offset: " + z);

    if (!onGround) {
      // The deltaY is 0 whenever the player sends their first position packet.
      // We have to account for this or the player will falsely fail the verification.
      if (deltaY == 0) {
        checkState(rotated, "illegal movement packet order: " + deltaY);
        checkState(movementTick == 0, "illegal y motion: " + movementTick);
        // 1.7 clients immediately start falling after this packet
        if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
          movementTick++;
        }
        // We've received the first position packet; the player will now start falling
        canFall = true;
        return;
      }

      // Make sure we're actually expecting the player to fall at this point in time
      checkState(canFall, "unexpected y motion: " + deltaY);

      // The movement tick should not be ignored when gravity check is disabled.
      movementTick++;

      if (enableGravityCheck) {
        // Predict the player's current motion based on the last motion
        // https://minecraft.wiki/w/Entity#Motion_of_entities
        final double predicted = (lastDeltaY - 0.08) * 0.98f;
        final double difference = Math.abs(deltaY - predicted);

        // Check if the difference between the predicted and actual motion is too large
        if (difference > 1e-7) {
          failOrShowCaptcha("incorrect gravity: " + predicted + "/ " + deltaY + "/" + y);
        }

        // The player is obeying gravity, go on to the next stage if the collision check is disabled.
        if (!enableCollisionsCheck && movementTick == maxMovementTick) {
          markSuccess();
        }
      }
    } else if (enableCollisionsCheck) {
      // Make sure the player has actually moved before reaching the platform
      if (enableGravityCheck && ++movementTick < maxMovementTick) {
        failOrShowCaptcha("illegal collision tick: " + movementTick + "/" + blockHeight);
      }
      // Calculate the difference between the player's Y coordinate and the expected Y coordinate
      final double collisionOffsetY = (DEFAULT_Y_COLLIDE_POSITION + blockHeight) - y;
      // Make sure the player is actually colliding with the blocks and not only spoofing ground
      if (collisionOffsetY != 0) {
        failOrShowCaptcha("illegal collision: " + collisionOffsetY + "/" + y + "/" + blockHeight);
      }
      // The player has collided with the blocks, go on to the next stage
      markSuccess();
    }
  }

  private void failOrShowCaptcha(final String debug) {
    // Do not throw an exception if the user configured to display the CAPTCHA instead
    if (Sonar.get().getConfig().getVerification().getGravity().isCaptchaOnFail()) {
      user.setForceCaptcha(true);
      markSuccess();
      return;
    }
    fail(debug);
  }
}
