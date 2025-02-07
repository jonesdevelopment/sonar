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

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketDecoder;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.AnimationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.EntityAnimationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.SetHeldItemPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.TransactionPacket;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.INVALID_HELD_ITEM_SLOT;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.PLAYER_ENTITY_ID;

public final class FallbackProtocolHandler extends FallbackVerificationHandler {

  public FallbackProtocolHandler(final @NotNull FallbackUser user) {
    super(user);

    // Immediately send the player the transaction packet
    sendTransaction();
  }

  private boolean waitingSwingArm, waitingSlotConfirm, waitingTransaction;
  private short expectedTransactionId;
  private int currentClientSlotId, expectedSlotId = -1;

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    if (packet instanceof TransactionPacket) {
      final TransactionPacket transaction = (TransactionPacket) packet;
      // Make sure we are expecting a transaction packet
      checkState(waitingTransaction, "unexpected transaction: " + transaction.getTransactionId());
      // Make sure the window ID is valid
      checkState(transaction.getWindowId() == 0, "wrong window: " + transaction.getWindowId());
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
      } else if (waitingSlotConfirm) {
        waitingSlotConfirm = false;
        expectedSlotId = -1;
        // The player did not send duplicate packets, so they pass this check
        if (user.isGeyser()) {
          markSuccess();
        } else {
          sendArmAnimation();
        }
      } else {
        sendSetHeldItem();
      }

      waitingTransaction = false;
    } else if (packet instanceof SetHeldItemPacket) {
      final SetHeldItemPacket heldItemPacket = (SetHeldItemPacket) packet;

      final int slotId = heldItemPacket.getSlot();
      // Also check if the player sent an invalid slot which is impossible by vanilla protocol
      checkState(slotId >= 0 && slotId <= 8, "slot out of range: " + slotId);
      // Check if the player sent a duplicate slot packet which is impossible by vanilla protocol
      checkState(slotId != currentClientSlotId, "duplicate slot: " + slotId);

      // Only continue checking if we're actually expecting a SetHeldItem packet
      if (expectedSlotId != -1
        // Check if the slot ID matches the expected slot ID
        // This can false flag if a player spams these packets, which is why we don't fail for this
        && slotId == expectedSlotId
        // Make sure we actually want to send a transaction at this point in time
        && !waitingSlotConfirm) {
        sendTransaction();
        waitingSlotConfirm = true;
      }

      currentClientSlotId = slotId;
    } else if (packet instanceof AnimationPacket) {
      // Make sure we are awaiting an AnimationPacket packet
      if (waitingSwingArm) {
        final AnimationPacket animationPacket = (AnimationPacket) packet;

        if (user.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_8)) {
          // Check if the entity ID is the player itself
          checkState(animationPacket.getEntityId() == PLAYER_ENTITY_ID,
            "invalid entity ID " + animationPacket.getEntityId());
          // Check if the player is sending the correct animation type
          if (animationPacket.getType() == AnimationPacket.LegacyAnimationType.SWING_ARM) {
            markSuccess();
            waitingSwingArm = false;
          }
        } else {
          checkState(animationPacket.getHand() == AnimationPacket.MAIN_HAND,
            "invalid hand " + animationPacket.getHand());
          markSuccess();
          waitingSwingArm = false;
        }
      }
    }
  }

  /**
   * Uses Transaction (ping) packets to check for an immediate, legitimate response from the client
   * <br>
   * <a href="https://wiki.vg/Protocol#Ping_.28configuration.29">Wiki.vg - Ping (configuration)</a>
   * <a href="https://wiki.vg/Protocol#Ping_.28play.29">Wiki.vg - Ping (play)</a>
   */
  private void sendTransaction() {
    // Send a Transaction (Ping) packet with a random ID
    expectedTransactionId = (short) -RANDOM.nextInt(Short.MAX_VALUE);
    user.write(new TransactionPacket(0, expectedTransactionId, false));
    waitingTransaction = true;
  }

  /**
   * Uses SetHeldItem packets to check for a legitimate response from the client
   * <br>
   * <a href="https://wiki.vg/Protocol#Set_Held_Item_.28serverbound.29">Wiki.vg - SetHeldItem (play)</a>
   */
  private void sendSetHeldItem() {
    // Send an invalid HeldItemChange packet to the player to see if the player responds at all
    user.delayedWrite(INVALID_HELD_ITEM_SLOT);
    // Increment the player's slot by a random slot, and then modulo it by the maximum slot (8)
    expectedSlotId = (currentClientSlotId + 1 + RANDOM.nextInt(7)) % 8;
    // Send two SetHeldItem packets with the same slot to check if the player responds with the correct slot.
    // By vanilla protocol, the client does not respond to duplicate SetHeldItem packets.
    // We can take advantage of this by sending two packets with the same content to check for a valid response.
    final SetHeldItemPacket heldItemPacket = new SetHeldItemPacket(expectedSlotId);
    user.delayedWrite(heldItemPacket);
    user.delayedWrite(heldItemPacket);
    user.channel().flush();
  }

  /**
   * Uses EntityAnimation packets to check for the excepted Animation (SwingArm) from the client.
   * <br>
   * <a href="https://wiki.vg/Protocol#Entity_Animation">Wiki.vg - EntityAnimation</a>
   * <a href="https://wiki.vg/Protocol#Swing_Arm">Wiki.vg - SwingArm</a>
   */
  private void sendArmAnimation() {
    user.write(new EntityAnimationPacket(PLAYER_ENTITY_ID, EntityAnimationPacket.Type.SWING_MAIN_ARM));
    waitingSwingArm = true;
  }

  private void markSuccess() {
    // Pass the player to the next best verification handler
    if (!user.isGeyser() && Sonar.get0().getConfig().getVerification().getVehicle().isEnabled()) {
      user.channel().pipeline().get(FallbackPacketDecoder.class).setListener(new FallbackVehicleHandler(user));
    } else if (user.isForceCaptcha() || Sonar.get0().getFallback().shouldPerformCaptcha()) {
      user.channel().pipeline().get(FallbackPacketDecoder.class).setListener(new FallbackCaptchaHandler(user));
    } else {
      // The player has passed all checks
      finishVerification();
    }
  }
}
