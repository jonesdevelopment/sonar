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

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.SetHeldItemPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.TransactionPacket;

import java.util.UUID;

public final class FallbackProtocolSessionHandler extends FallbackSessionHandler {

  public FallbackProtocolSessionHandler(final @NotNull FallbackUser user,
                                        final @NotNull String username,
                                        final @NotNull UUID uuid,
                                        final boolean forceCAPTCHA) {
    super(user, username, uuid);

    this.forceCAPTCHA = forceCAPTCHA;

    // Immediately send the player the transaction packet
    sendTransaction();
  }

  private final boolean forceCAPTCHA;
  private short expectedTransactionId;
  private int currentClientSlotId, expectedSlotId = -1;

  /**
   * Uses Transaction (ping) packets to check for an immediate, legitimate response from the client
   * <br>
   * <a href="https://wiki.vg/Protocol#Ping_.28configuration.29">Wiki.vg - Ping (configuration)</a>
   * <a href="https://wiki.vg/Protocol#Ping_.28play.29">Wiki.vg - Ping (play)</a>
   */
  private void sendTransaction() {
    // Send a Transaction (Ping) packet with a random ID
    expectedTransactionId = (short) -(RANDOM.nextInt(Short.MAX_VALUE));
    user.write(new TransactionPacket(0, expectedTransactionId, false));
  }

  /**
   * Uses SetHeldItem packets to check for a legitimate response from the client
   * <br>
   * <a href="https://wiki.vg/Protocol#Set_Held_Item_.28serverbound.29">Wiki.vg - SetHeldItem (play)</a>
   */
  private void sendSetHeldItem() {
    // Move the player's slot by 4 (slot limit divided by 2),
    // and then modulo it by the slot limit (8) to ensure that we don't send invalid slot IDs.
    expectedSlotId = (currentClientSlotId + 4) % 8;
    // Send two SetHeldItem packets with the same slot to check if the player responds with the correct slot.
    // By vanilla protocol, the client does not respond to duplicate SetHeldItem packets.
    // We can take advantage of this by sending two packets with the same content to check for a valid response.
    final SetHeldItemPacket heldItemPacket = new SetHeldItemPacket(expectedSlotId);
    user.delayedWrite(heldItemPacket);
    user.delayedWrite(heldItemPacket);
    user.getChannel().flush();
  }

  private void markSuccess() {
    // Either send the player to the vehicle check,
    // send the player to the CAPTCHA, or finish the verification.
    final var decoder = (FallbackPacketDecoder) user.getPipeline().get(FallbackPacketDecoder.class);
    // Pass the player to the next best verification handler
    if (Sonar.get().getFallback().shouldPerformVehicleCheck()) {
      decoder.setListener(new FallbackVehicleSessionHandler(user, username, uuid, forceCAPTCHA));
    } else if (forceCAPTCHA || Sonar.get().getFallback().shouldPerformCaptcha()) {
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

      // Only perform the SetHeldItem check on Java players since the Bedrock protocol breaks this check.
      // I don't really know why this doesn't work on Bedrock,
      // but I think it has to do with how Geyser/floodgate translates this packet:
      // https://wiki.vg/Bedrock_Protocol#Player_Hotbar
      if (user.isGeyser()) {
        markSuccess();
      } else {
        sendSetHeldItem();
      }
    } else if (packet instanceof SetHeldItemPacket) {
      final SetHeldItemPacket heldItemPacket = (SetHeldItemPacket) packet;

      final int slotId = heldItemPacket.getSlot();
      // Also check if the player sent an invalid slot which is impossible by vanilla protocol
      checkState(slotId >= 0 && slotId <= 8, "slot out of range: " + slotId);
      // Check if the player sent a duplicate slot packet which is impossible by vanilla protocol
      checkState(slotId != currentClientSlotId, "invalid slot: " + slotId);

      // Only continue checking if we're actually expecting a SetHeldItem packet
      // The player can send a SetHeldItem packet by themselves -> exempt
      if (expectedSlotId != -1
        // Check if the slot ID matches the expected slot ID
        // This can false flag if a player spams these packets, which is why we don't fail for this
        && slotId == expectedSlotId) {
        markSuccess();
      }

      currentClientSlotId = slotId;
    }
  }
}
