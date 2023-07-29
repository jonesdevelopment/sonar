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

package xyz.jonesdev.sonar.common.fallback;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackConnection;
import xyz.jonesdev.sonar.api.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketListener;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.*;
import xyz.jonesdev.sonar.common.protocol.ProtocolUtil;

import java.util.Random;
import java.util.regex.Pattern;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_13;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackVerificationHandler implements FallbackPacketListener {
  @Getter
  private final @NotNull FallbackConnection<?, ?> player;
  private final String username;
  private final short transactionId;
  private final long verifyKeepAliveId;
  private int movementTick;
  private double lastY;
  @Setter
  @Getter
  private State state;

  private static final Random random = new Random();

  @RequiredArgsConstructor
  public enum State {
    // LOGIN
    KEEP_ALIVE(false),
    // PLAY
    CLIENT_SETTINGS(false),
    PLUGIN_MESSAGE(false),
    TRANSACTION(false),
    // IN-GAME
    TELEPORT(true),
    POSITION(true),
    COLLISIONS(true),
    // OTHER
    SUCCESS(false);

    private final boolean canMove;
  }

  public FallbackVerificationHandler(final @NotNull FallbackConnection<?, ?> player, final String username) {
    this.player = player;
    this.username = username;
    this.transactionId = (short) random.nextInt();
    this.verifyKeepAliveId = random.nextInt();
    this.state = State.KEEP_ALIVE;

    if (player.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
      // 1.7 players don't have KeepAlive packets in the login process
      sendJoinGamePacket();
    } else {
      // Send first KeepAlive to check if the connection is somewhat responsive
      player.sendPacket(new KeepAlive(verifyKeepAliveId));
    }
  }

  private void sendTransaction() {
    state = State.TRANSACTION;
    player.sendPacket(new Transaction(
      0, transactionId, false
    ));
  }

  private void sendJoinGamePacket() {
    state = State.CLIENT_SETTINGS;
    player.sendPacket(getJoinPacketForVersion(player.getProtocolVersion()));
  }

  private void sendChunkData() {
    state = State.POSITION;
    // Teleport player into the fake lobby by sending an empty chunk
    player.sendPacket(EMPTY_CHUNK_DATA);
  }

  private static boolean validateClientBrand(final @NotNull FallbackConnection<?, ?> player, final ByteBuf content) {
    // We have to catch every DecoderException, so we can fail and punish
    // the player instead of only disconnecting them due to an exception.
    try {
      // Regex pattern for validating client brands
      final Pattern pattern = player.getFallback().getSonar().getConfig().VALID_BRAND_REGEX;
      // 1.7 has some very weird issues when trying to decode the client brand
      final boolean legacy = player.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0;
      final int cap = player.getFallback().getSonar().getConfig().MAXIMUM_BRAND_LENGTH;
      // Read the client brand using our custom readString method that supports 1.7.
      // The legacy version of readString does not compare the string length
      // with the VarInt sent by the client.
      final String read = ProtocolUtil.readString(content, cap, legacy);
      // No need to check for empty or too long client brands since
      // ProtocolUtil#readString already does exactly that.
      return !read.equals("Vanilla") // The normal brand is always lowercase
        && pattern.matcher(read).matches(); // Disallow non-ascii characters (by default)
    } catch (DecoderException exception) {
      // Fail if the string (client brand) could not be decoded properly
      player.fail("could not decode string");
      // Throw the exception so we don't continue checking
      throw exception;
    }
  }

  @Override
  public void handle(final FallbackPacket packet) {
    if (packet instanceof KeepAlive) {
      final KeepAlive keepAlive = (KeepAlive) packet;

      checkFrame(state == State.KEEP_ALIVE, "wrong state: " + state);
      checkFrame(keepAlive.getId() == verifyKeepAliveId, "invalid KeepAlive ID");

      // The correct KeepAlive packet has been received
      sendJoinGamePacket();
    }

    if (packet instanceof ClientSettings) {
      // For some odd reason, the client rarely sends a ClientSettings packet twice?!
      if (state != State.CLIENT_SETTINGS) {
        player.disconnect(player.getFallback().getSonar().getConfig().UNEXPECTED_ERROR);
        return;
      }

      state = State.PLUGIN_MESSAGE;
    }

    if (packet instanceof PluginMessage) {
      final PluginMessage pluginMessage = (PluginMessage) packet;

      // Only the brand channel is important
      if (pluginMessage.getChannel().equals("MC|Brand")
        || pluginMessage.getChannel().equals("minecraft:brand")) {
        checkFrame(state == State.PLUGIN_MESSAGE, "wrong state: " + state);

        // Check if the channel is correct - 1.13 uses the new namespace
        // system ('minecraft:' + channel) and anything below 1.13 uses
        // the legacy namespace system ('MC|' + channel).
        final boolean v1_13 = player.getProtocolVersion().compareTo(MINECRAFT_1_13) >= 0;
        checkFrame(pluginMessage.getChannel().equals("MC|Brand") || v1_13, "invalid channel");

        // Validate the client branding using a regex to filter unwanted characters.
        checkFrame(validateClientBrand(player, pluginMessage.content()), "invalid client brand");

        // Send the transaction packet
        sendTransaction();
      }
    }

    if (packet instanceof Transaction) {
      checkFrame(state == State.TRANSACTION, "wrong state: " + state);

      final Transaction transaction = (Transaction) packet;

      // The transaction should always be accepted
      checkFrame(transaction.isAccepted(), "transaction not accepted?!");
      // Check if the transaction ID is valid
      checkFrame(transaction.getId() == transactionId, "invalid transaction id");

      // Checking gravity is disabled, just finish verification
      if (!player.getFallback().getSonar().getConfig().CHECK_GRAVITY) {
        finish();
        return;
      }

      // Teleport the player to the spawn position
      player.sendPacket(SPAWN_TELEPORT);
      // Make sure the player is unable to fly (the player is in spectator mode)
      player.sendPacket(DEFAULT_ABILITIES);

      // 1.7-1.8 clients do not have a TeleportConfirm packet
      if (player.getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0) {
        // Immediately send the chunk data as 1.7-1.8 can't confirm teleports
        sendChunkData();
      } else {
        state = State.TELEPORT;
      }
    }

    if (packet instanceof TeleportConfirm) {
      // Check if the player sent the TeleportConfirm packet twice
      checkFrame(state == State.TELEPORT, "wrong state: " + state);

      final TeleportConfirm teleportConfirm = (TeleportConfirm) packet;

      // Check if the teleport id is correct
      final boolean teleportIdCorrect = teleportConfirm.getTeleportId() == SPAWN_TELEPORT.getTeleportId();
      checkFrame(teleportIdCorrect, "invalid teleport ID");

      // Now we can send the chunk data
      sendChunkData();
    }

    if (state.canMove) {
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

        // This packet does not send new position data, reuse the last Y
        handlePositionUpdate(lastY, player.isOnGround());
      }
    }
  }

  private void handlePositionUpdate(final double y, final boolean isOnGround) {
    final double deltaY = lastY - y;
    lastY = y;

    // The onGround property can never be true when we aren't checking for collisions
    checkFrame(!isOnGround || state == State.COLLISIONS, "invalid ground state");

    // Skip teleport packets using this check
    if (deltaY > 0) {

      // Verify the player if they sent correct movement packets
      if (movementTick++ >= MAX_MOVEMENT_TICK) {
        if (player.getFallback().getSonar().getConfig().CHECK_COLLISIONS) {
          if (state != State.COLLISIONS) {
            // Prevent the packet from flooding the traffic by limiting
            // the times the packet is sent to the player
            state = State.COLLISIONS;
            // Send an UpdateSectionBlocks packet with a platform of blocks
            // to check if the player collides with the solid platform
            player.sendPacket(UPDATE_SECTION_BLOCKS);
          } else {
            final double offset = DEFAULT_Y_COLLIDE_POSITION - lastY;

            // The offset cannot be 0 or greater than 0 since the blocks will
            // not let the player fall through them
            checkFrame(offset < 0, "invalid y collision");

            // Check if the player is colliding by performing a basic Y offset check
            if (isOnGround) {
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

  private void checkFrame(final boolean condition, final String message) {
    if (!condition) {
      player.fail(message);
      throw new CorruptedFrameException(message);
    }
  }

  private void finish() {
    state = State.SUCCESS;

    player.getFallback().getVerified().add(player.getInetAddress().toString());
    player.getFallback().getConnected().remove(username);

    player.disconnect(Sonar.get().getConfig().VERIFICATION_SUCCESS);

    player.getFallback().getLogger().info("Successfully verified " + username);
  }
}
