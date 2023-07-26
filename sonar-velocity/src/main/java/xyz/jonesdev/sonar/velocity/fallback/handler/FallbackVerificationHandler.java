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
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.common.fallback.packets.Disconnect;
import xyz.jonesdev.sonar.common.fallback.packets.Position;
import xyz.jonesdev.sonar.common.fallback.packets.PositionLook;
import xyz.jonesdev.sonar.common.fallback.packets.Transaction;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketListener;
import xyz.jonesdev.sonar.velocity.fallback.FallbackPlayer;

import java.util.Random;

import static xyz.jonesdev.sonar.velocity.fallback.FallbackListener.CachedMessages.VERIFICATION_SUCCESS;
import static xyz.jonesdev.sonar.velocity.fallback.FallbackPackets.*;

public final class FallbackVerificationHandler implements FallbackPacketListener, FallbackHandler {
  @Getter
  private final @NotNull FallbackPlayer player;
  private final short transactionId;
  private static final Random random = new Random();
  private boolean hasReceivedTransaction;
  private int movementTick;
  private double lastY;

  private static int MAX_MOVEMENT_TICK = 5;
  private static double[] PREPARED_MOVEMENT_PACKETS;

  public static void update() {
    MAX_MOVEMENT_TICK = Sonar.get().getConfig().MAXIMUM_MOVEMENT_TICKS;
    PREPARED_MOVEMENT_PACKETS = new double[MAX_MOVEMENT_TICK + 2];

    for (int i = 0; i < MAX_MOVEMENT_TICK + 1; i++) {
      PREPARED_MOVEMENT_PACKETS[i] = -((Math.pow(0.98, i) - 1) * 3.92);
    }
  }

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

      // Teleport player into the fake lobby by sending an empty chunk
      player.getConnection().write(EMPTY_CHUNK_DATA);
      player.getConnection().write(SPAWN_TELEPORT);
      // Make sure the player is unable to fly (the player is in spectator mode)
      player.getConnection().write(DEFAULT_ABILITIES);
    }

    if (packet instanceof Position) {
      final Position position = (Position) packet;

      // The onGround property can never be true
      checkFrame(!position.isOnGround(), "invalid ground state");

      // Only check after the teleport packet was sent
      if (hasReceivedTransaction) {
        // Now check the new position
        handlePositionUpdate(position.getY());
      }
    }

    if (packet instanceof PositionLook) {
      final PositionLook position = (PositionLook) packet;

      // The onGround property can never be true
      checkFrame(!position.isOnGround(), "invalid ground state");

      // Only check after the teleport packet was sent
      if (hasReceivedTransaction) {
        // Now check the new position
        handlePositionUpdate(position.getY());
      }
    }
  }

  private void handlePositionUpdate(final double y) {
    final double deltaY = lastY - y;
    lastY = y;

    // Skip teleport packets using this check
    if (deltaY > 0) {
      // Predict the next y motion
      final double predicted = PREPARED_MOVEMENT_PACKETS[++movementTick];
      final double offset = Math.abs(deltaY - predicted);

      // Check if the y motion is similar to the predicted value
      checkFrame(offset < 0.01, "too high y offset");

      // Verify the player if they sent correct movement packets
      if (movementTick >= MAX_MOVEMENT_TICK) {
        finish();
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
