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
import xyz.jonesdev.sonar.common.timer.DelayTimer;

import java.util.Random;
import java.util.UUID;
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
  private int movementTick, packets;
  private double lastX, lastY, lastZ;
  @Setter
  @Getter
  private State state;

  private final DelayTimer login = new DelayTimer();
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

  public FallbackVerificationHandler(final @NotNull FallbackConnection<?, ?> player,
                                     final @NotNull String username,
                                     final @NotNull UUID uuid) {
    this.player = player;
    this.username = username;
    this.transactionId = (short) random.nextInt();
    this.verifyKeepAliveId = random.nextInt();
    this.state = State.KEEP_ALIVE;

    // Send LoginSuccess packet to make the client think they are joining the server
    player.sendPacket(new ServerLoginSuccess(username, uuid));

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

  private static boolean validateClientLocale(final @NotNull FallbackConnection<?, ?> player, final String locale) {
    // Check the client locale by performing a simple regex check on it
    final Pattern pattern = player.getFallback().getSonar().getConfig().VALID_LOCALE_REGEX;
    return pattern.matcher(locale).matches(); // Disallow non-ascii characters (by default)
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
    // Check if the player is not sending a ton of packets to the server
    final int maxPackets = player.getFallback().getSonar().getConfig().MAXIMUM_LOGIN_PACKETS
      + player.getFallback().getSonar().getConfig().MAXIMUM_MOVEMENT_TICKS;
    checkFrame(++packets < maxPackets, "too many packets");

    // Check for timeout since the player could be sending packets but not important ones
    final long timeout = player.getFallback().getSonar().getConfig().VERIFICATION_TIMEOUT;
    // Check if the time limit has exceeded
    if (login.delay() > timeout) {
      player.getChannel().close();
      return;
    }

    if (packet instanceof KeepAlive) {
      assertState(State.KEEP_ALIVE);

      final KeepAlive keepAlive = (KeepAlive) packet;

      checkFrame(keepAlive.getId() == verifyKeepAliveId, "invalid KeepAlive ID");

      // The correct KeepAlive packet has been received
      sendJoinGamePacket();
    }

    if (packet instanceof ClientSettings) {
      final ClientSettings clientSettings = (ClientSettings) packet;

      // Validate the locale using a regex to filter unwanted characters.
      checkFrame(validateClientLocale(player, clientSettings.getLocale()), "invalid locale");

      state = State.PLUGIN_MESSAGE;
    }

    if (packet instanceof PluginMessage) {
      final PluginMessage pluginMessage = (PluginMessage) packet;

      // Only the brand channel is important
      if (pluginMessage.getChannel().equals("MC|Brand")
        || pluginMessage.getChannel().equals("minecraft:brand")) {

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
      assertState(State.TRANSACTION);

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

      // Make sure the player is unable to fly (the player is in spectator mode)
      player.sendPacket(DEFAULT_ABILITIES);
      // Teleport the player to the spawn position
      player.sendPacket(SPAWN_TELEPORT);

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
      assertState(State.TELEPORT);

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

        // Immediately handle new position update
        handlePositionUpdate(position.getX(), position.getY(), position.getZ(), position.isOnGround());
      }

      if (packet instanceof PositionLook) {
        final PositionLook position = (PositionLook) packet;

        // Immediately handle new position update
        handlePositionUpdate(position.getX(), position.getY(), position.getZ(), position.isOnGround());
      }

      if (packet instanceof Player) {
        final Player player = (Player) packet;

        // This packet does not send new position data, reuse the last Y
        handlePositionUpdate(lastX, lastY, lastZ, player.isOnGround());
      }
    }
  }

  private void handlePositionUpdate(final double x, final double y, final double z, final boolean ground) {
    final double deltaY = lastY - y;

    lastX = x;
    lastY = y;
    lastZ = z;

    // The player is not allowed to move away from the collision platform.
    // This should not happen unless the max movement tick is configured to a high number.
    checkFrame(Math.abs(x - BLOCKS_PER_ROW) < BLOCKS_PER_ROW, "moved too far (x)");
    checkFrame(Math.abs(z - BLOCKS_PER_ROW) < BLOCKS_PER_ROW, "moved too far (z)");

    // The onGround property can never be true when we aren't checking for collisions
    checkFrame(!ground || state == State.COLLISIONS, "invalid ground state");

    // Skip teleports using this small check
    if (deltaY > 0.07) {

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
            final double offsetY = DEFAULT_Y_COLLIDE_POSITION - lastY;

            // The offset cannot be 0 or greater than 0 since the blocks will
            // not let the player fall through them
            checkFrame(offsetY < 0, "no collisions: " + offsetY);

            // Check if the player is colliding by performing a basic Y offset check
            if (ground) {
              // The player is colliding, finish verification
              finish();
            }
          }
        } else {
          // Checking collisions is disabled, just finish verification
          finish();
        }
      } else if (y >= DEFAULT_Y_COLLIDE_POSITION) {
        // This is a basic gravity check that predicts the next y position
        final double predictedY = PREPARED_MOVEMENT_PACKETS[movementTick];
        final double offsetY = Math.abs(deltaY - predictedY);

        // Check if the y motion is roughly equal to the predicted value
        checkFrame(offsetY < 0.01, "invalid offset: " + y + ", " + offsetY);
      }
    }
  }

  private void assertState(final State state) {
    if (state != this.state) {
      player.fail("expected " + state + ", got " + this.state);
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

    player.getFallback().getLogger().info("{} has been verified successfully ({}s!).",
      username, login.formattedDelay());
  }
}
