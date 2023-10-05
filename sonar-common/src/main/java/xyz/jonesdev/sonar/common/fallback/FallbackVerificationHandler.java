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
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserVerifySuccessEvent;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.model.VerifiedPlayer;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacketListener;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.*;
import xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil;

import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_13;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackVerificationHandler implements FallbackPacketListener {
  private final @NotNull FallbackUser<?, ?> user;
  private final String username;
  private final UUID playerUuid;
  private short expectedTransactionId;
  private long expectedKeepAliveId;
  private int expectedTeleportId = -1;
  private int tick, totalReceivedPackets;
  private int ignoredMovementTicks;
  private double posX, posY, posZ, lastY;
  @Setter
  private State state;
  private boolean listenForMovements;

  private final SystemTimer login = new SystemTimer();
  private static final Random RANDOM = new Random();

  public enum State {
    // PRE-JOIN
    KEEP_ALIVE,
    // JOIN
    CLIENT_SETTINGS, PLUGIN_MESSAGE,
    // POST-JOIN
    TRANSACTION,
    // PLAY
    TELEPORT, POSITION, COLLISIONS,
    // OTHER
    SUCCESS
  }

  public FallbackVerificationHandler(final @NotNull FallbackUser<?, ?> user,
                                     final @NotNull String username,
                                     final @NotNull UUID playerUuid) {
    this.user = user;
    this.username = username;
    this.playerUuid = playerUuid;
    this.state = State.KEEP_ALIVE;

    // Send LoginSuccess packet to make the client think they are joining the server
    user.write(new ServerLoginSuccess(username, playerUuid));

    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
      // 1.7 players don't have KeepAlive packets in the login process
      sendJoinGamePacket();
    } else {
      // Generate a random KeepAlive ID for the pre-join check
      expectedKeepAliveId = RANDOM.nextInt();
      // Send first KeepAlive to check if the connection is somewhat responsive
      user.write(new KeepAlive(expectedKeepAliveId));
    }
  }

  private void sendTransaction() {
    // Set the state to TRANSACTION to avoid false positives
    // and go on with the flow of the verification.
    state = State.TRANSACTION;
    // Generate a random transaction ID
    expectedTransactionId = (short) RANDOM.nextInt();
    // Send a transaction with the given ID
    user.write(new Transaction(0, expectedTransactionId, false));
  }

  private void sendJoinGamePacket() {
    // Set the state to CLIENT_SETTINGS to avoid false positives
    // and go on with the flow of the verification.
    state = State.CLIENT_SETTINGS;
    // Select the JoinGame packet for the respective protocol version
    user.write(getJoinPacketForVersion(user.getProtocolVersion()));
  }

  private void sendChunkData() {
    // Set the state to POSITION to avoid false positives
    // and go on with the flow of the verification.
    state = State.POSITION;
    // Teleport player into the fake lobby by sending an empty chunk
    user.delayedWrite(EMPTY_CHUNK_DATA);
    // Send an UpdateSectionBlocks packet with a platform of blocks
    // to check if the player collides with the solid platform.
    user.delayedWrite(UPDATE_SECTION_BLOCKS);
    // Send all packets in one flush
    user.getChannel().flush();
  }

  private void setAbilitiesAndTeleport() {
    // Make sure the player is unable to fly (the player is in spectator mode)
    user.write(DEFAULT_ABILITIES);
    // Generate the current teleport ID
    expectedTeleportId = RANDOM.nextInt(Short.MAX_VALUE);
    // Teleport the player to the spawn position
    user.write(new PositionLook(
      SPAWN_X_POSITION, DYNAMIC_SPAWN_Y_POSITION, SPAWN_Z_POSITION,
      0f, 0f, expectedTeleportId, false
    ));
  }

  private static boolean validateClientLocale(final @SuppressWarnings("unused") @NotNull FallbackUser<?, ?> user,
                                              final String locale) {
    // Check the client locale by performing a simple regex check on it
    final Pattern pattern = Sonar.get().getConfig().getValidLocaleRegex();
    return pattern.matcher(locale).matches(); // Disallow non-ascii characters (by default)
  }

  private static boolean validateClientBrand(final @NotNull FallbackUser<?, ?> user, final ByteBuf content) {
    // We have to catch every DecoderException, so we can fail and punish
    // the player instead of only disconnecting them due to an exception.
    try {
      // No need to check for empty brands since ProtocolUtil#readBrandMessage
      // already performs these checks by default.
      final String read = ProtocolUtil.readBrandMessage(content);
      // Check if the decoded client brand string is too long
      if (read.length() > Sonar.get().getConfig().getMaximumBrandLength()) {
        return false;
      }
      // Regex pattern for validating client brands
      final Pattern pattern = Sonar.get().getConfig().getValidBrandRegex();
      return !read.equals("Vanilla") // The normal brand is always lowercase
        && pattern.matcher(read).matches(); // Disallow non-ascii characters (by default)
    } catch (DecoderException exception) {
      // Fail if the string (client brand) could not be decoded properly
      user.fail("could not decode string");
      // Throw the exception so we don't continue checking
      throw exception;
    }
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    // The player has already been verified, drop all other packets
    if (state == State.SUCCESS) return;

    // Check if the player is not sending a ton of packets to the server
    final int maxPackets = Sonar.get().getConfig().getMaximumLoginPackets()
      + Sonar.get().getConfig().getMaximumMovementTicks();
    checkFrame(++totalReceivedPackets < maxPackets, "too many packets");

    // Check for timeout since the player could be sending packets but not important ones
    final long timeout = Sonar.get().getConfig().getVerificationTimeout();
    // Check if the time limit has exceeded
    if (login.elapsed(timeout)) {
      user.getChannel().close();
      return;
    }

    if (packet instanceof KeepAlive) {
      final KeepAlive keepAlive = (KeepAlive) packet;

      // 1.7-1.8.9 are sending a KeepAlive packet with the ID 0 every 20 ticks
      if (keepAlive.getId() == 0 && user.getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0) return;

      // Check if we are currently expecting a KeepAlive packet
      assertState(State.KEEP_ALIVE);

      checkFrame(keepAlive.getId() == expectedKeepAliveId, "invalid KeepAlive ID");

      // The correct KeepAlive packet has been received
      sendJoinGamePacket();
    }

    if (packet instanceof ClientSettings) {
      final ClientSettings clientSettings = (ClientSettings) packet;

      // Validate the locale using a regex to filter unwanted characters.
      checkFrame(validateClientLocale(user, clientSettings.getLocale()), "invalid locale");

      // Clients sometimes mess up the ClientSettings or PluginMessage packet.
      if (state == State.CLIENT_SETTINGS) {
        state = State.PLUGIN_MESSAGE;
      }
    }

    if (packet instanceof PluginMessage) {
      final PluginMessage pluginMessage = (PluginMessage) packet;

      // Only the brand channel is important
      if (pluginMessage.getChannel().equals("MC|Brand")
        || pluginMessage.getChannel().equals("minecraft:brand")) {

        // Check if the channel is correct - 1.13 uses the new namespace
        // system ('minecraft:' + channel) and anything below 1.13 uses
        // the legacy namespace system ('MC|' + channel).
        final boolean v1_13 = user.getProtocolVersion().compareTo(MINECRAFT_1_13) >= 0;
        checkFrame(pluginMessage.getChannel().equals("MC|Brand") || v1_13, "invalid channel");

        // Validate the client branding using a regex to filter unwanted characters.
        checkFrame(validateClientBrand(user, pluginMessage.content()), "invalid client brand");

        // Clients sometimes mess up the ClientSettings or PluginMessage packet.
        if (state == State.PLUGIN_MESSAGE) {
          // Send the transaction packet
          sendTransaction();
        }
      }
    }

    if (packet instanceof Transaction) {
      // Check if we are currently expecting a Transaction packet
      assertState(State.TRANSACTION);

      final Transaction transaction = (Transaction) packet;

      // The transaction should always be accepted
      checkFrame(transaction.isAccepted(), "transaction not accepted?!");
      // Check if the transaction ID is valid
      checkFrame(transaction.getId() == expectedTransactionId, "invalid transaction id");

      // Checking gravity is disabled, just finish verification
      if (!Sonar.get().getConfig().isCheckGravity()) {
        finish();
        return;
      }

      // First, send an abilities packet to the client to make
      // sure the player falls even in spectator mode.
      // Then, teleport the player to the spawn position.
      setAbilitiesAndTeleport();

      // 1.7-1.8 clients do not have a TeleportConfirm packet
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0) {
        // Immediately send the chunk data as 1.7-1.8 can't confirm teleports
        sendChunkData();
      } else {
        // Set the state to TELEPORT to avoid false positives
        // and go on with the flow of the verification.
        state = State.TELEPORT;
      }
    }

    if (packet instanceof TeleportConfirm) {
      // Check if we are currently expecting a TeleportConfirm packet
      assertState(State.TELEPORT);

      // Check if the teleport ID is correct
      final TeleportConfirm teleportConfirm = (TeleportConfirm) packet;
      checkFrame(teleportConfirm.getTeleportId() == expectedTeleportId, "invalid teleport ID");

      // Reset all values to ensure safety on teleport
      tick = 1;
      posY = lastY = -1;
      expectedTeleportId = -1;

      // Now we can send the chunk data
      sendChunkData();
    }

    if (packet instanceof Position) {
      final Position position = (Position) packet;
      handlePositionUpdate(position.getX(), position.getY(), position.getZ(), position.isOnGround());
    }

    if (packet instanceof PositionLook) {
      final PositionLook position = (PositionLook) packet;
      handlePositionUpdate(position.getX(), position.getY(), position.getZ(), position.isOnGround());
    }

    if (packet instanceof Player) {
      final Player player = (Player) packet;
      handlePositionUpdate(posX, posY, posZ, player.isOnGround());
    }
  }

  private void handlePositionUpdate(final double x, final double y, final double z, final boolean ground) {
    // 1.8 does not have a TeleportConfirm packet, so we need to handle the spawning differently
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0
      && expectedTeleportId != -1 // Check if the teleport ID is currently unset
      // Then, check if the position is equal to the spawn position
      && x == SPAWN_X_POSITION && y == DYNAMIC_SPAWN_Y_POSITION && z == SPAWN_Z_POSITION) {
      // Reset all values to ensure safety on teleport
      tick = 1;
      posY = -1;
      expectedTeleportId = -1;
    }

    posX = x;
    lastY = posY;
    posY = y;
    posZ = z;

    // The player is not allowed to move away from the collision platform.
    // This should not happen unless the max movement tick is configured to a high number.
    checkFrame(Math.abs(x - BLOCKS_PER_ROW) <= BLOCKS_PER_ROW, "moved too far (x)");
    checkFrame(Math.abs(z - BLOCKS_PER_ROW) <= BLOCKS_PER_ROW, "moved too far (z)");

    // The onGround property can never be true when we aren't checking for collisions
    checkFrame(!ground || state == State.COLLISIONS, "invalid ground state");

    // Check if the client hasn't moved before sending the first movement packet
    if (!listenForMovements) {
      if (posX == SPAWN_X_POSITION && posZ == SPAWN_Z_POSITION) {
        // Now, once we verified the X and Z position, we can safely check for gravity
        listenForMovements = true;
      }

      lastY = DYNAMIC_SPAWN_Y_POSITION;
      return;
    }

    final double deltaY = lastY - y;
    // The deltaY is 0 whenever the player sends their first position packet.
    // We have to account for this or the player will fail the verification.
    if (deltaY == 0) {
      // Check for too many ignored Y ticks
      final int maxIgnoredTicks = Sonar.get().getConfig().getMaximumIgnoredTicks();
      checkFrame(++ignoredMovementTicks < maxIgnoredTicks, "too many ignored ticks");
      return;
    }

    if (tick > MAX_MOVEMENT_TICK) {
      if (!Sonar.get().getConfig().isCheckCollisions()) {
        // Checking collisions is disabled, just finish verification
        finish();
        return;
      }

      if (state != State.COLLISIONS) {
        // Now we don't care about gravity anymore,
        // we just want the player to collide with the blocks.
        state = State.COLLISIONS;
      }

      // Calculate the difference between the player's Y coordinate and the expected Y coordinate
      final double offsetY = DEFAULT_Y_COLLIDE_POSITION - y;

      // Log/debug position if enabled in the configuration
      if (Sonar.get().getConfig().isDebugXYZPositions()) {
        user.getFallback().getLogger().info("{}: {}/{}/{} - offset: {}", username, x, y, z, offsetY);
      }

      // Check if the player is colliding by performing a basic Y offset check.
      // The offset cannot greater than 0 since the blocks will not let the player fall through them.
      checkFrame(offsetY <= 0, "no collisions: " + offsetY);

      if (ground) {
        // The player is colliding to blocks, finish verification
        finish();
        return;
      }
    }

    if (!ground && tick < MAX_PREDICTION_TICK) {
      final double predictedY = PREPARED_MOVEMENTS[tick];
      final double offsetY = Math.abs(deltaY - predictedY);

      // Log/debug position if enabled in the configuration
      if (Sonar.get().getConfig().isDebugXYZPositions()) {
        user.getFallback().getLogger().info("{}: {}/{}/{} - deltaY: {} - prediction: {} - offset: {}",
          username, x, y, z, deltaY, predictedY, offsetY);
      }

      // Check if the y motion is roughly equal to the predicted value
      final String verbose = String.format("%d: %.7f %.10f %.10f!%.10f", tick, y, offsetY, deltaY, predictedY);
      checkFrame(offsetY < 0.005, verbose);
    }
    tick++;
  }

  private void assertState(final @NotNull State expectedState) {
    if (expectedState != state) {
      user.fail("expected " + expectedState + ", got " + state);
    }
  }

  private void checkFrame(final boolean condition, final String message) {
    if (!condition) {
      user.fail(message);
      throw new CorruptedFrameException(message);
    }
  }

  private void finish() {
    state = State.SUCCESS;

    user.getFallback().getConnected().remove(username);

    // Add verified player to the database
    final VerifiedPlayer verifiedPlayer = new VerifiedPlayer(user.getInetAddress().toString(), playerUuid, login.getStart());
    Sonar.get().getVerifiedPlayerController().add(verifiedPlayer);

    // Call the VerifySuccessEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifySuccessEvent(username, playerUuid, user, login.delay()));

    // Disconnect player with the verification success message
    user.disconnect(Sonar.get().getConfig().getVerificationSuccess());

    user.getFallback().getLogger().info(Sonar.get().getConfig().getVerificationSuccessfulLog()
      .replace("%name%", username)
      .replace("%time%", login.toString()));
  }
}
