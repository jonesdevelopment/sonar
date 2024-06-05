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
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;

import java.util.UUID;

import static xyz.jonesdev.sonar.api.config.SonarConfiguration.Verification.Gravity.Gamemode.CREATIVE;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackGravitySessionHandler extends FallbackSessionHandler {

  public FallbackGravitySessionHandler(final FallbackUser user, final String username, final UUID uuid) {
    super(user, username, uuid);

    // We don't want to check Geyser players for valid gravity, as this might cause issues because of the protocol
    this.enableGravityCheck = !user.isGeyser() && Sonar.get().getConfig().getVerification().getGravity().isEnabled();
    this.enableCollisionsCheck = Sonar.get().getConfig().getVerification().getGravity().isCheckCollisions();

    // FIXME: 1.18.2-1.19.3 weird loading world issue
    // First, write the JoinGame packet to the buffer
    user.delayedWrite(joinGame);
    // Then, write the ClientAbilities packet to the buffer
    // This is only necessary if the player is in creative mode
    if (Sonar.get().getConfig().getVerification().getGravity().getGamemode() == CREATIVE) {
      user.delayedWrite(DEFAULT_ABILITIES);
    }
    // Write the PositionLook packet to the buffer
    user.delayedWrite(spawnPosition);
    // Write the DefaultSpawnPosition packet to the buffer
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_19_3) >= 0) {
      user.delayedWrite(defaultSpawnPosition);
    }
    // Write the PlayerInfo packet to the buffer
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_16_4) >= 0) {
      user.delayedWrite(PLAYER_INFO);
    }
    // 1.20.3+ introduced game events
    // Make sure the client knows that we're sending chunks next
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_20_3) >= 0) {
      user.delayedWrite(START_WRITING_CHUNKS);
    }
    // Teleport player into an empty world by sending an empty chunk packet
    user.delayedWrite(EMPTY_CHUNK_DATA);
    // Spawn the invisible platform below the player
    if (this.enableCollisionsCheck) {
      user.delayedWrite(updateSectionBlocks);
    }
    // Send all packets at once
    user.getChannel().flush();

    // 1.8 and below don't have TeleportConfirm packets, which is why we're skipping that check.
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_9) < 0) {
      markTeleported();
    }
  }

  private final boolean enableGravityCheck, enableCollisionsCheck;
  private boolean teleported, checkMovement;
  private short expectedTransactionId = 1;
  private double x, y, z, deltaY;
  private int movementTick;

  private void markTeleported() {
    // Activate the movement checks
    teleported = true;
  }

  private void markNextStage() {
    // Force-stop the movement checks
    teleported = false;

    // Check if the player sent all necessary packets
    checkState(user.isReceivedClientSettings(), "didn't send client settings");
    checkState(user.isReceivedPluginMessage(), "didn't send plugin message");
  }

  /**
   * Uses ping packets to check for an immediate, legitimate response from the client
   * <br>
   * <a href="https://wiki.vg/Protocol#Ping_.28configuration.29">Wiki.vg - Ping (configuration)</a>
   * <a href="https://wiki.vg/Protocol#Ping_.28play.29">Wiki.vg - Ping (play)</a>
   */
  private void sendTransaction() {
    // Interrupt any ongoing process/check
    markNextStage();

    // Send a Transaction (Ping) packet with a random ID
    expectedTransactionId = (short) -(RANDOM.nextInt(Short.MAX_VALUE));
    user.write(new TransactionPacket(0, expectedTransactionId, false));
  }

  private void forceCAPTCHA() {
    // Interrupt any ongoing process/check
    markNextStage();

    // Forcefully make the player solve a CAPTCHA
    val decoder = (FallbackPacketDecoder) user.getChannel().pipeline().get(FallbackPacketDecoder.class);
    decoder.setListener(new FallbackCAPTCHASessionHandler(user, username, uuid));
  }

  private void markSuccess() {
    // Either send the player to the vehicle check,
    // send the player to the CAPTCHA, or finish the verification.
    val decoder = (FallbackPacketDecoder) user.getChannel().pipeline().get(FallbackPacketDecoder.class);
    // Pass the player to the next best verification handler
    if (Sonar.get().getFallback().shouldPerformVehicleCheck()) {
      decoder.setListener(new FallbackVehicleSessionHandler(user, username, uuid, x, y, z));
    } else if (Sonar.get().getFallback().shouldPerformCaptcha()) {
      decoder.setListener(new FallbackCAPTCHASessionHandler(user, username, uuid));
    } else {
      // The player has passed all checks
      finishVerification();
    }
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof TransactionPacket) {
      final TransactionPacket transaction = (TransactionPacket) packet;

      // Make sure random transactions aren't counted
      checkState(expectedTransactionId <= 0, "unexpected transaction");
      // Make sure the transaction was accepted
      // This must - by vanilla protocol - always be accepted
      checkState(transaction.isAccepted(), "didn't accept transaction");
      // Also check if the transaction ID matches the expected ID
      final long transactionId = transaction.getTransactionId();
      checkState(transactionId == expectedTransactionId,
        "expected T ID " + expectedTransactionId + ", but got " + transactionId);

      markSuccess();
    } else if (packet instanceof PlayerPositionLookPacket) {
      // Make sure the player has teleported before checking for position packets
      if (teleported) {
        final PlayerPositionLookPacket positionLook = (PlayerPositionLookPacket) packet;
        handleMovement(positionLook.getX(), positionLook.getY(), positionLook.getZ(), positionLook.isOnGround());
      }
    } else if (packet instanceof PlayerPositionPacket) {
      // Make sure the player has teleported before checking for position packets
      if (teleported) {
        final PlayerPositionPacket position = (PlayerPositionPacket) packet;
        handleMovement(position.getX(), position.getY(), position.getZ(), position.isOnGround());
      }
    } else if (packet instanceof TeleportConfirmPacket) {
      final TeleportConfirmPacket teleportConfirm = (TeleportConfirmPacket) packet;

      // Only expect this packet to be sent once
      checkState(!teleported, "duplicate teleport confirm");
      // Check if the teleport ID matches the expected ID
      final int teleportId = teleportConfirm.getTeleportId();
      checkState(teleportId == TELEPORT_ID,
        "expected TP ID " + TELEPORT_ID + ", but got " + teleportId);

      markTeleported();
    }
    // Make sure to catch all ClientSettings and PluginMessage packets during the play phase.
    else if (packet instanceof ClientSettingsPacket) {
      // Let the session handler itself know about this packet
      checkClientSettings((ClientSettingsPacket) packet);
    } else if (packet instanceof PluginMessagePacket) {
      // Let the session handler itself know about this packet
      checkPluginMessage((PluginMessagePacket) packet);
    }
  }

  private void handleMovement(final double x, final double y, final double z, final boolean isOnGround) {
    // Check if the client hasn't moved before sending the first movement packet
    if (!checkMovement) {
      if (x == SPAWN_X_POSITION && z == SPAWN_Z_POSITION) {
        // Now, once we verified the X and Z position, we can safely check for gravity
        checkMovement = true;
        movementTick = 0;
        // Synchronize the Y coordinate
        this.y = dynamicSpawnYPosition;
      }
      return;
    }

    // Calculate/store all necessary positions
    final double lastDeltaY = this.deltaY;
    final double lastY = this.y;
    this.deltaY = y - lastY;
    this.x = x;
    this.y = y;
    this.z = z;

    // Log/debug position if enabled in the configuration
    if (Sonar.get().getConfig().getVerification().isDebugXYZPositions()) {
      Sonar.get().getFallback().getLogger().info("{}: {}/{}/{}, ly={}/dy={}, {}",
        username, x, y, z, lastY, deltaY, isOnGround);
    }

    // The player is not allowed to move away from the collision platform.
    // This should not happen unless the max movement tick is configured to a high number.
    checkState(Math.abs(x - BLOCKS_PER_ROW) < BLOCKS_PER_ROW, "moved too far (x)");
    checkState(Math.abs(z - BLOCKS_PER_ROW) < BLOCKS_PER_ROW, "moved too far (z)");

    if (!isOnGround) {
      // The deltaY is 0 whenever the player sends their first position packet.
      // We have to account for this or the player will fail the verification.
      if (deltaY == 0) {
        // Reset the movement tick as a safety measure
        movementTick = 0;
        return;
      }

      if (enableGravityCheck) {
        // Predict the player's current motion based on the last motion
        // https://minecraft.wiki/w/Entity#Motion_of_entities
        final double predicted = (lastDeltaY - 0.08) * 0.98f;
        final double difference = Math.abs(deltaY - predicted);

        // Check if the difference between the predicted motion and
        // the actual motion is greater than our minimum threshold.
        if (difference > 1e-7) {
          // Do not throw an exception if the user configured to display the CAPTCHA instead
          if (Sonar.get().getConfig().getVerification().getGravity().isCaptchaOnFail()) {
            forceCAPTCHA();
            return;
          }
          fail("incorrect gravity; predicted: " + predicted + " deltaY: " + deltaY + " y: " + y);
        }
      }

      // The player is obeying gravity, go on to the next stage if the collision check is disabled.
      if (++movementTick == maxMovementTick && !enableCollisionsCheck) {
        sendTransaction();
      }
    } else if (enableCollisionsCheck) {
      // Calculate the difference between the player's Y coordinate and the expected Y coordinate
      double collisionOffsetY = (DEFAULT_Y_COLLIDE_POSITION + blockType.getBlockHeight()) - y;
      // 1.7 sends the head position instead of the AABB minY
      // This little hack accounts for the offset of approximately 1.62
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
        collisionOffsetY += 1.62f;
      }
      // Make sure the player is actually colliding with the blocks and not only spoofing ground
      checkState(collisionOffsetY > -0.03, "illegal collision: " + collisionOffsetY);
      // The player has collided with the blocks, go on to the next stage
      sendTransaction();
    }
  }
}
