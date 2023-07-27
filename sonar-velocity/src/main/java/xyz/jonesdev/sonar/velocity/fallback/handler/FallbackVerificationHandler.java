/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.velocity.fallback.handler;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketListener;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.*;
import xyz.jonesdev.sonar.velocity.fallback.FallbackPlayer;

import java.util.Random;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;
import static xyz.jonesdev.sonar.velocity.fallback.FallbackListener.CachedMessages.VERIFICATION_SUCCESS;

public final class FallbackVerificationHandler implements FallbackPacketListener, FallbackHandler {
  @Getter
  private final @NotNull FallbackPlayer player;
  private final short transactionId;
  private static final Random random = new Random();
  private boolean hasReceivedTransaction, hasSentBlockChange;
  private int movementTick;
  private double lastY;

  public FallbackVerificationHandler(final @NotNull FallbackPlayer player) {
    this.player = player;
    this.transactionId = (short) random.nextInt();

    player.getConnection().write(new Transaction(
      0, transactionId, false
    ));
  }

  @Override
  public void handle(final FallbackPacket packet) {
    if (packet instanceof Transaction) {
      checkFrame(!hasReceivedTransaction, "unexpected timing (T1)");
      hasReceivedTransaction = true;

      final Transaction transaction = (Transaction) packet;

      checkFrame(transaction.isAccepted(), "transaction not accepted?!");
      checkFrame(transaction.getId() == transactionId, "invalid transaction id");

      if (!player.getFallback().getSonar().getConfig().CHECK_GRAVITY) {
        finish();
        return;
      }

      // Teleport player into the fake lobby by sending an empty chunk
      player.getConnection().write(EMPTY_CHUNK_DATA);
      player.getConnection().write(SPAWN_TELEPORT);
      // Make sure the player is unable to fly (the player is in spectator mode)
      player.getConnection().write(DEFAULT_ABILITIES);
    }

    // Only check after the teleport packet was sent
    if (hasReceivedTransaction) {
      if (packet instanceof Position) {
        final Position position = (Position) packet;

        handlePositionUpdate(position.getY(), position.isOnGround());
      }

      if (packet instanceof PositionLook) {
        final PositionLook position = (PositionLook) packet;

        handlePositionUpdate(position.getY(), position.isOnGround());
      }

      if (packet instanceof Player) {
        final Player player = (Player) packet;

        // This packet does not send any position data, just reuse the last Y
        handlePositionUpdate(lastY, player.isOnGround());
      }
    }
  }

  private void handlePositionUpdate(final double y, final boolean isOnGround) {
    final double deltaY = lastY - y;
    lastY = y;

    // The onGround property can never be true when we aren't checking for collisions
    checkFrame(!isOnGround || hasSentBlockChange, "invalid ground state");

    // Skip teleport packets using this check
    if (deltaY > 0) {

      // Verify the player if they sent correct movement packets
      if (movementTick++ >= MAX_MOVEMENT_TICK) {
        if (player.getFallback().getSonar().getConfig().CHECK_COLLISIONS) {
          if (!hasSentBlockChange) {
            // Prevent the packet from flooding the traffic by limiting
            // the times the packet is sent to the player
            hasSentBlockChange = true;
            // Send a MultiBlockChange packet with our stone platform
            // for checking if the player has valid collisions
            player.getConnection().write(MULTI_BLOCK_CHANGE);
          } else {
            // Check if the player is colliding by performing a basic Y offset check
            if (isOnGround) {
              final double offset = DYNAMIC_COLLIDE_Y_POSITION - lastY;

              // The offset cannot be 0 or greater than 0 since the blocks will
              // not let the player fall through them
              checkFrame(offset < 0, "invalid y on ground");

              // The player is colliding, finish verification
              finish();
            }
          }
        } else {
          // Checking collisions is disabled, just finish verification
          finish();
        }
      } else {
        // This is a basic gravity check that predicts the next y position
        final double predictedY = PREPARED_MOVEMENT_PACKETS[movementTick];
        final double offsetY = Math.abs(deltaY - predictedY);

        // Check if the y motion is similar to the predicted value
        checkFrame(offsetY < 0.01, "too high y offset");
      }
    }
  }

  /**
   * Restore old pipelines and send the player to the actual server
   */
  private synchronized void finish() {
    player.getFallback().getVerified().add(player.getInetAddress().toString());
    player.getFallback().getConnected().remove(player.getPlayer().getUsername());

    final String serialized = ProtocolUtils.getJsonChatSerializer(player.getConnection().getProtocolVersion())
      .serialize(VERIFICATION_SUCCESS);
    player.getConnection().closeWith(Disconnect.create(serialized));

    player.getFallback().getLogger().info("Successfully verified " + player.getPlayer().getUsername());
  }
}
