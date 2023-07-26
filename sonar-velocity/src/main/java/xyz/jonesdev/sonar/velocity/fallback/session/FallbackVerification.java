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

package xyz.jonesdev.sonar.velocity.fallback.session;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.common.fallback.packets.Disconnect;
import xyz.jonesdev.sonar.common.fallback.packets.Transaction;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketListener;
import xyz.jonesdev.sonar.velocity.fallback.FallbackHandler;

import java.util.Random;

import static xyz.jonesdev.sonar.velocity.fallback.FallbackListener.CachedMessages.VERIFICATION_SUCCESS;

public final class FallbackVerification implements FallbackPacketListener, FallbackHandler {
  @Getter
  private final @NotNull FallbackPlayer player;
  private final short transactionId;
  private static final Random random = new Random();

  public FallbackVerification(final @NotNull FallbackPlayer player) {
    this.player = player;
    this.transactionId = (short) random.nextInt();

    player.getConnection().write(new Transaction(
      0, transactionId, false
    ));
  }

  @Override
  public void handle(final FallbackPacket packet) {
    if (packet instanceof Transaction) {
      final Transaction transaction = (Transaction) packet;

      checkFrame(transaction.isAccepted(), "transaction not accepted?!");
      checkFrame(transaction.getId() == transactionId, "invalid transaction id");

      finish();
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
