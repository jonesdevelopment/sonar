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

package xyz.jonesdev.sonar.common.fallback;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import lombok.val;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.event.impl.UserVerifySuccessEvent;
import xyz.jonesdev.sonar.api.fallback.Fallback;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.api.fallback.FallbackUserState;
import xyz.jonesdev.sonar.api.model.VerifiedPlayer;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.common.fallback.protocol.*;
import xyz.jonesdev.sonar.common.fallback.protocol.map.ItemType;
import xyz.jonesdev.sonar.common.fallback.protocol.map.MapCaptchaInfo;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.config.FinishConfigurationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginAcknowledgedPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;
import xyz.jonesdev.sonar.common.fallback.protocol.vehicle.EntityType;
import xyz.jonesdev.sonar.common.statistics.GlobalSonarStatistics;
import xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import static xyz.jonesdev.sonar.api.config.SonarConfiguration.Verification.Gravity.Gamemode.CREATIVE;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_ENCODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackUserState.*;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

public final class FallbackVerificationHandler implements FallbackPacketListener {
  private static final Random RANDOM = new Random();
  private static final Fallback FALLBACK = Sonar.get().getFallback();

  private final SystemTimer login = new SystemTimer();
  private final @NotNull FallbackUser user;
  private final String username;
  private final UUID playerUuid;

  // Checks
  private short expectedTransactionId;
  private int expectedKeepAliveId, expectedTeleportId = -1;
  private int tick, totalReceivedPackets;
  private double posX, posY, posZ, lastY, spawnYPosition;
  private boolean resolvedClientBrand, resolvedClientSettings;
  private boolean listenForMovements;
  private boolean receivedSteerBoat, receivedPlayerInput;
  private boolean bedrockSpawned;

  // CAPTCHA
  private final SystemTimer keepAlive = new SystemTimer();
  private final SystemTimer actionBar = new SystemTimer();
  private @Nullable MapCaptchaInfo captcha;
  private int captchaTriesLeft;

  // Cached options to make sure the original values don't change throughout the verification
  private final boolean performVehicle, performCaptcha, performGravity, performCollisions, transfer;

  public FallbackVerificationHandler(final @NotNull FallbackUser user,
                                     final @NotNull String username,
                                     final @NotNull UUID playerUuid) {
    this.user = user;
    this.username = username;
    this.playerUuid = playerUuid;
    this.performGravity = Sonar.get().getConfig().getVerification().getGravity().isEnabled();
    this.performCollisions = Sonar.get().getConfig().getVerification().getGravity().isCheckCollisions();
    this.performCaptcha = FALLBACK.shouldPerformCaptcha();
    this.performVehicle = FALLBACK.shouldPerformVehicleCheck();
    this.transfer = Sonar.get().getConfig().getVerification().getTransfer().isEnabled()
      && user.getProtocolVersion().compareTo(MINECRAFT_1_20_5) >= 0;
  }

  private void configure() {
    // Set the state to CONFIGURE to avoid false positives
    user.setState(CONFIGURE);
    // Send the necessary configuration packets to the client
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_20_5) >= 0) {
      for (final FallbackPacket packet : REGISTRY_SYNC_1_20_5) {
        user.delayedWrite(packet);
      }
    } else {
      user.delayedWrite(REGISTRY_SYNC_LEGACY);
    }
    user.delayedWrite(FINISH_CONFIGURATION);
    // Send all packets in one flush
    user.getChannel().flush();
    // Set decoder state to actually catch all packets
    updateEncoderDecoderState(FallbackPacketRegistry.CONFIG);
  }

  private void updateEncoderDecoderState(final @NotNull FallbackPacketRegistry registry) {
    val decoder = (FallbackPacketDecoder) user.getChannel().pipeline().get(FALLBACK_PACKET_DECODER);
    val encoder = (FallbackPacketEncoder) user.getChannel().pipeline().get(FALLBACK_PACKET_ENCODER);
    if (decoder != null && encoder != null) {
      // Update the packet registry state to be able to listen for CONFIG packets
      decoder.updateRegistry(registry);
      encoder.updateRegistry(registry);
    } else {
      // Something went wrong - the decoder should not be null
      FALLBACK.getLogger().warn("Necessary pipelines for {} not found", username);
      // Close the channel to prevent bypasses or other potential exploits
      user.getChannel().close();
    }
  }

  public void initialJoinProcess() {
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
      // 1.7 players don't have KeepAlive packets in the login process
      sendJoinGamePacket();
    } else {
      // Set the state to KEEP_ALIVE to avoid false positives
      user.setState(KEEP_ALIVE);
      // Generate a random KeepAlive ID for the pre-join check
      expectedKeepAliveId = RANDOM.nextInt();
      // Send first KeepAlive to check if the connection is somewhat responsive
      user.write(new KeepAlivePacket(expectedKeepAliveId));
    }
  }

  private void sendTransaction() {
    // Set the state to TRANSACTION to avoid false positives
    // and go on with the flow of the verification.
    user.setState(TRANSACTION);
    // Generate a random transaction ID
    expectedTransactionId = (short) RANDOM.nextInt();
    // Send a transaction with the given ID
    user.write(new TransactionPacket(0, expectedTransactionId, false));
  }

  private void sendJoinGamePacket() {
    final boolean v1_20_2 = user.getProtocolVersion().compareTo(MINECRAFT_1_20_2) >= 0;
    if (!v1_20_2) {
      // Set the state to CLIENT_SETTINGS to avoid false positives
      // (1.20.2 needs the LOGIN_ACK state) and go on with
      // the flow of the verification.
      user.setState(CLIENT_SETTINGS);
    }
    // Send the JoinGame packet so we can continue the verification
    user.write(joinGame);
    if (v1_20_2) {
      // Perform the transaction check since the ClientSettings
      // and PluginMessage packets have already been validated.
      sendTransaction();
    }
  }

  private void sendAbilitiesAndTeleport() {
    // Set the state to TELEPORT to avoid false positives
    // and go on with the flow of the verification.
    user.setState(TELEPORT);
    // Make sure the player is unable to fly (e.g. if the player is in creative mode)
    if (Sonar.get().getConfig().getVerification().getGravity().getGamemode() == CREATIVE) {
      user.delayedWrite(DEFAULT_ABILITIES);
    }
    // Generate the current teleport ID
    expectedTeleportId = RANDOM.nextInt();
    // Add a little randomization to the spawn y coordinate
    spawnYPosition = dynamicSpawnYPosition + RANDOM.nextDouble(0.4);
    // Teleport the player to the spawn position
    user.delayedWrite(new PlayerPositionLookPacket(
      SPAWN_X_POSITION, spawnYPosition, SPAWN_Z_POSITION,
      0f, -90f, expectedTeleportId, false));
    // Make sure the player escapes the 1.18.2+ "Loading terrain" screen
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_18_2) >= 0) {
      user.delayedWrite(dynamicSpawnPosition);
    }
  }

  private void sendChunkData() {
    // If we don't have any checks, simply finish verification
    // This is *hopefully* a very rare case though...
    if (!performGravity && !performCaptcha && !performVehicle) {
      // Save some work by finishing before sending even more packets
      finish();
      return;
    }
    // Set the state to POSITION to avoid false positives
    // and go on with the flow of the verification.
    user.setState(POSITION);
    // 1.20.3 packets introduced game events
    if (user.getProtocolVersion().compareTo(MINECRAFT_1_20_3) >= 0) {
      // Make sure the client knows that we're sending chunks next
      user.delayedWrite(START_WRITING_CHUNKS);
    }
    // Teleport player into the fake lobby by sending an empty chunk
    user.delayedWrite(EMPTY_CHUNK_DATA);
    // Checking gravity is disabled, just finish verification
    if (!performGravity) {
      // Switch to captcha state if needed
      vehicleCheckOrNext();
      return;
    }
    // Send an UpdateSectionBlocks packet with a platform of blocks
    // to check if the player collides with the solid platform.
    user.delayedWrite(updateSectionBlocks);
    // Send all packets in one flush
    user.getChannel().flush();
  }

  private static boolean validateClientLocale(final @SuppressWarnings("unused") @NotNull FallbackUser user,
                                              final String locale) {
    // Check the client locale by performing a simple regex check on it
    final Pattern pattern = Sonar.get().getConfig().getVerification().getValidLocaleRegex();
    return pattern.matcher(locale).matches(); // Disallow non-ascii characters (by default)
  }

  private static boolean validateClientBrand(final @NotNull FallbackUser user, final ByteBuf content) {
    // We have to catch every DecoderException, so we can fail and punish
    // the player instead of only disconnecting them due to an exception.
    try {
      // No need to check for empty brands since ProtocolUtil#readBrandMessage
      // already performs these checks by default.
      final String read = ProtocolUtil.readBrandMessage(content);
      // Check if the decoded client brand string is too long
      if (read.length() > Sonar.get().getConfig().getVerification().getBrand().getMaxLength()) {
        return false;
      }
      // Regex pattern for validating client brands
      final Pattern pattern = Sonar.get().getConfig().getVerification().getBrand().getValidRegex();
      return !read.equals("Vanilla") // The normal brand is always lowercase
        && pattern.matcher(read).matches(); // Disallow non-ascii characters (by default)
    } catch (DecoderException exception) {
      // Fail if the string (client brand) could not be decoded properly
      user.fail("could not decode string");
      // Throw the exception so we don't continue checking
      throw exception;
    }
  }

  private void handlePacketsCAPTCHA(final @NotNull FallbackPacket packet) {
    // Check if the player took too long to enter the captcha
    final int maxDuration = Sonar.get().getConfig().getVerification().getMap().getMaxDuration();
    checkFrame(!login.elapsed(maxDuration), "took too long to enter captcha");

    // Handle incoming chat messages
    if (packet instanceof UniversalChatPacket) {
      checkFrame(captcha != null, "Captcha not sent yet");
      final UniversalChatPacket chat = (UniversalChatPacket) packet;
      if (!chat.getMessage().equals(captcha.getAnswer())) {
        // Captcha is incorrect
        checkFrame(captchaTriesLeft-- > 0, "failed captcha too often");
        user.write(incorrectCaptcha);
        return;
      }
      // Captcha is correct
      finish();
      return;
    }

    // Every second
    if (actionBar.elapsed(user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0 ? 10_000L : 1_000L)) {
      final String actionBarMessage = Sonar.get().getConfig().getVerification().getMap().getEnterCodeActionBar();
      // Only send action bar if the message is actually supposed to be sent
      if (!actionBarMessage.isEmpty()) {
        final String timeLeft = String.format("%.0f", (maxDuration - login.delay()) / 1000D);
        user.write(new UniversalChatPacket(MiniMessage.miniMessage().deserialize(
          actionBarMessage.replace("%time-left%", timeLeft)), UniversalChatPacket.GAME_INFO_TYPE));
        // Make sure to reset the timer
        actionBar.reset();
      }
    }

    // Every 10 seconds
    if (keepAlive.elapsed(10_000L)) {
      // Send a KeepAlive packet to prevent timeout
      user.write(CAPTCHA_KEEP_ALIVE);
      // Make sure to reset the timer
      keepAlive.reset();
    }
  }

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    // The player has already been verified or failed the verification -> drop packet
    if (!user.getState().canReceivePackets()) return;

    // Calculate maximum amount of packets sent by the client to the server
    final int maxPackets = Sonar.get().getConfig().getVerification().getMaxLoginPackets()
      + maxMovementTick // Account for positions/fall check
      + Sonar.get().getConfig().getVerification().getMap().getMaxDuration()
      + Sonar.get().getConfig().getVerification().getMap().getMaxTries();
    checkFrame(++totalReceivedPackets < maxPackets, "too many packets");

    // We are expecting a code in chat, drop all other packets
    if (user.getState() == CAPTCHA) {
      handlePacketsCAPTCHA(packet);
      return;
    }

    if (packet instanceof PaddleBoatPacket) {
      // Check if we are currently expecting a PaddleBoatPacket packet
      assertState(VEHICLE);

      // Only pass the player if both checks are passed
      if (receivedPlayerInput && receivedSteerBoat) {
        captchaOrNext();
      }

      // Mark paddle boat check as passed
      receivedSteerBoat = true;
    }

    if (packet instanceof PlayerInputPacket) {
      // Check if we are currently expecting a PlayerInputPacket packet
      assertState(VEHICLE);

      final PlayerInputPacket inputPacket = (PlayerInputPacket) packet;

      // Make sure the values sent by the client are legitimate
      checkFrame(inputPacket.getForward() <= 0.98f, "invalid vehicle speed (f)");
      checkFrame(inputPacket.getSideways() <= 0.98f, "invalid vehicle speed (s)");

      // Don't verify if the player tries unmounting or jumping
      checkFrame(!inputPacket.isJump(), "tried to jump in vehicle");
      checkFrame(!inputPacket.isUnmount(), "tried to escape the vehicle");

      // Only pass the player if both checks are passed
      if (receivedPlayerInput && receivedSteerBoat) {
        captchaOrNext();
      }

      // pre-1.9 clients do not have a PaddleBoat packet
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_9) < 0) {
        receivedSteerBoat = true;
      } else {
        // PaddleBoat → PlayerInput → PaddleBoat
        checkFrame(receivedSteerBoat, "invalid vehicle steering order");
      }
      // Mark player input check as passed
      receivedPlayerInput = true;
      return;
    }

    if (packet instanceof LoginAcknowledgedPacket) {
      // Check if we are currently expecting a LoginAcknowledged packet
      assertState(LOGIN_ACK);

      // Start the configuration process for 1.20.2 clients
      configure();
      return;
    }

    if (packet instanceof FinishConfigurationPacket) {
      // Check if we are currently expecting a FinishConfiguration packet
      assertState(CONFIGURE);

      // Check if the client has already sent valid ClientSettings and PluginMessage packets
      if (!user.isGeyser()) {
        checkFrame(resolvedClientBrand, "did not resolve client brand");
        checkFrame(resolvedClientSettings, "did not resolve client settings");
      }

      // Start initializing the actual join process
      updateEncoderDecoderState(FallbackPacketRegistry.GAME);
      initialJoinProcess();
      return;
    }

    if (packet instanceof KeepAlivePacket) {
      final KeepAlivePacket keepAlive = (KeepAlivePacket) packet;

      // 1.7-1.8.9 are sending a KeepAlive packet with the ID 0 every 20 ticks
      if (keepAlive.getId() == 0 && user.getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0) return;

      // Check if we are currently expecting a KeepAlive packet
      assertState(KEEP_ALIVE);

      checkFrame(keepAlive.getId() == expectedKeepAliveId, "invalid KeepAlive ID");

      // The correct KeepAlive packet has been received
      sendJoinGamePacket();
      return;
    }

    if (packet instanceof ClientSettingsPacket) {
      final ClientSettingsPacket clientSettings = (ClientSettingsPacket) packet;

      // Validate the locale using a regex to filter unwanted characters.
      checkFrame(validateClientLocale(user, clientSettings.getLocale()), "invalid locale");

      // Clients sometimes mess up the ClientSettings or PluginMessage packet.
      // Probably caused by some weird race condition in Minecraft 1.13+
      if (user.getState() == CLIENT_SETTINGS) {
        user.setState(PLUGIN_MESSAGE);
      }

      // Make sure we mark the ClientSettings as valid
      resolvedClientSettings = true;
      return;
    }

    if (packet instanceof PluginMessagePacket) {
      final PluginMessagePacket pluginMessage = (PluginMessagePacket) packet;

      // Only the brand channel is important, drop the rest
      if (pluginMessage.getChannel().equals("MC|Brand")
        || pluginMessage.getChannel().equals("minecraft:brand")) {
        // Check if the brand packet was sent twice,
        // which is not possible when using a vanilla Minecraft client.
        checkFrame(!resolvedClientBrand, "duplicate client brand packet");

        // Check if the channel is correct - 1.13 uses the new namespace
        // system ('minecraft:' + channel) and anything below 1.13 uses
        // the legacy namespace system ('MC|' + channel).
        final boolean v1_13 = user.getProtocolVersion().compareTo(MINECRAFT_1_13) >= 0;
        checkFrame(pluginMessage.getChannel().equals("MC|Brand") || v1_13, "invalid channel");

        // Validate the client branding using a regex to filter unwanted characters.
        if (Sonar.get().getConfig().getVerification().getBrand().isEnabled()) {
          checkFrame(validateClientBrand(user, pluginMessage.getSlicedBuffer()), "invalid client brand");
        }

        // Clients sometimes mess up the ClientSettings or PluginMessage packet.
        if (user.getState() == PLUGIN_MESSAGE) {
          // Send the transaction packet
          sendTransaction();
        }

        // Make sure we mark the PluginMessage as valid
        resolvedClientBrand = true;
      }
      return;
    }

    if (packet instanceof TransactionPacket) {
      // Check if we are currently expecting a Transaction packet
      assertState(TRANSACTION);

      final TransactionPacket transaction = (TransactionPacket) packet;

      // The transaction should always be accepted
      checkFrame(transaction.isAccepted(), "transaction not accepted?!");
      // Check if the transaction ID is valid
      checkFrame(transaction.getId() == expectedTransactionId, "invalid transaction id");

      // First, send an Abilities packet to the client to make
      // sure the player falls even in spectator mode.
      // Then, teleport the player to the spawn position.
      sendAbilitiesAndTeleport();

      // 1.7-1.8 clients do not have a TeleportConfirm packet
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0) {
        // Immediately send the chunk data as 1.7-1.8 can't confirm teleport packets
        sendChunkData();
      } else {
        // Send all previously sent packets in one flush since we didn't flush
        // the channel earlier when we teleported the player.
        user.getChannel().flush();
      }
      return;
    }

    if (packet instanceof TeleportConfirmPacket) {
      // Check if we are currently expecting a TeleportConfirm packet
      assertState(TELEPORT);

      // Check if the teleport ID is correct
      final TeleportConfirmPacket teleportConfirm = (TeleportConfirmPacket) packet;
      checkFrame(teleportConfirm.getTeleportId() == expectedTeleportId, "invalid teleport ID");

      // Reset all values to ensure safety on teleport
      tick = 1;
      posY = lastY = -1;
      expectedTeleportId = -1;

      // Now we can send the chunk data
      sendChunkData();
      return;
    }

    if (user.getState() != LOGIN_ACK) {
      if (packet instanceof PlayerPositionPacket) {
        final PlayerPositionPacket position = (PlayerPositionPacket) packet;
        handlePositionUpdate(position.getX(), position.getY(), position.getZ(), position.isOnGround());
        return;
      }

      if (packet instanceof PlayerPositionLookPacket) {
        final PlayerPositionLookPacket position = (PlayerPositionLookPacket) packet;
        handlePositionUpdate(position.getX(), position.getY(), position.getZ(), position.isOnGround());
        return;
      }

      if (packet instanceof PlayerTickPacket) {
        final PlayerTickPacket player = (PlayerTickPacket) packet;
        handlePositionUpdate(posX, posY, posZ, player.isOnGround());
      }
    }
  }

  private void handlePositionUpdate(final double x, final double y, final double z, final boolean ground) {
    try {
      // 1.8 does not have a TeleportConfirm packet, so we need to handle the spawning differently
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) <= 0
        && expectedTeleportId != -1 // Check if the teleport ID is currently unset
        // Then, check if the position is equal to the spawn position
        && x == SPAWN_X_POSITION && y == spawnYPosition && z == SPAWN_Z_POSITION) {
        // Reset all values to ensure safety on teleport
        tick = 1;
        posY = -1;
        expectedTeleportId = -1;
        // Check for ground state in the first packet
        checkFrame(!ground, "invalid ground state");
      }

      posX = x;
      lastY = posY;
      posY = y;
      posZ = z;

      // The player is not allowed to move away from the collision platform.
      // This should not happen unless the max movement tick is configured to a high number.
      checkFrame(Math.abs(x - BLOCKS_PER_ROW) < BLOCKS_PER_ROW, "moved too far (x)");
      checkFrame(Math.abs(z - BLOCKS_PER_ROW) < BLOCKS_PER_ROW, "moved too far (z)");

      // Check if the client hasn't moved before sending the first movement packet
      if (!listenForMovements) {
        if (posX == SPAWN_X_POSITION && posZ == SPAWN_Z_POSITION) {
          // Now, once we verified the X and Z position, we can safely check for gravity
          listenForMovements = true;
        }

        lastY = spawnYPosition;
        return;
      }

      final double deltaY = lastY - y;
      // The deltaY is 0 whenever the player sends their first position packet.
      // We have to account for this or the player will fail the verification.
      if (deltaY == 0) return;

      // Calculate the difference between the player's Y coordinate and the expected Y coordinate
      double collisionOffsetY = (DEFAULT_Y_COLLIDE_POSITION + blockType.getBlockHeight()) - y;
      if (user.getProtocolVersion().compareTo(MINECRAFT_1_8) < 0) {
        collisionOffsetY += 1.62f; // 1.7 is weird and sends the head position instead of the AABB minY
      }

      // Check if the player is colliding by performing a basic Y offset check.
      // The offset cannot be greater than 0 since the blocks will not let the player fall through them.
      checkFrame(collisionOffsetY <= 0, "fell through blocks: " + collisionOffsetY);

      if (tick > maxMovementTick) {
        // Log/debug position if enabled in the configuration
        if (Sonar.get().getConfig().getVerification().isDebugXYZPositions()) {
          FALLBACK.getLogger().info("{}: {}/{}/{} - deltaY: {} - ground: {} - collision: {}",
            username, x, y, z, deltaY, ground, collisionOffsetY);
        }

        // If the collision check is disabled, finish verification
        if (!performCollisions) {
          vehicleCheckOrNext();
          return;
        }

        // Perform the collision check
        if (ground) {
          // Make sure the player is actually colliding with the blocks and not only spoofing ground
          checkFrame(collisionOffsetY > -0.03, "illegal collision: " + collisionOffsetY);
          // The player is colliding to blocks, finish verification
          vehicleCheckOrNext();
          return;
        } else {
          // Make sure the player is colliding with blocks but is sending an invalid ground state
          checkFrame(collisionOffsetY != 0, "no ground: " + collisionOffsetY);
        }
      } else {
        // Check if the player is spoofing the ground state
        checkFrame(!ground, "spoofed ground state");
      }

      // Make sure we don't run out of predicted Y motions
      checkFrame(tick < preparedCachedYMotions.length, "too many movements");

      if (!ground && performGravity) {
        final double predictedY = preparedCachedYMotions[tick];
        final double offsetY = Math.abs(deltaY - predictedY);

        // Log/debug position if enabled in the configuration
        if (Sonar.get().getConfig().getVerification().isDebugXYZPositions()) {
          FALLBACK.getLogger().info("{}: {}/{}/{} - deltaY: {} - prediction: {} - offset: {}",
            username, x, y, z, deltaY, predictedY, offsetY);
        }

        boolean correctMotion = offsetY < 5e-3; // 0.005

        // TODO: make this actually somewhat readable
        if (!correctMotion && user.isGeyser() && !bedrockSpawned) {
          // Use 0.0001 as some sort of threshold to check
          // if the player spawned with the wrong position.
          if (Math.abs(deltaY) < 1e-4) {
            // Reset ticks
            tick = 0;
            correctMotion = true;

            if (Sonar.get().getConfig().getVerification().isDebugXYZPositions()) {
              FALLBACK.getLogger().info("[geyser] Ignoring first position (deltaY: {})", deltaY);
            }
          } else {
            final int previousTick = tick;
            // This may result in an instant bypass of the gravity check.
            // Perhaps ignorable ticks should be restricted.
            final long maxIgnoreTick = (System.currentTimeMillis() - login.getStart() + 100L) / 50L;
            for (int i = (tick + 1); (i < preparedCachedYMotions.length && (i - tick) <= maxIgnoreTick); i++) {
              final double newPredictedY = preparedCachedYMotions[i];
              if ((deltaY - newPredictedY) < 0.005) {
                tick = i;
                break;
              }
            }

            // checkFrame(oldTick != tick, "too many movements");
            correctMotion = tick > previousTick;

            if (correctMotion && Sonar.get().getConfig().getVerification().isDebugXYZPositions()) {
              FALLBACK.getLogger().info("[geyser] Skipping {} ticks (deltaY: {} - predicted: {})",
                tick - previousTick, deltaY, preparedCachedYMotions[tick]);
            }
          }
        }

        // Check if the y motion is roughly equal to the predicted value
        checkFrame(correctMotion, String.format("invalid gravity: %d, %.7f, %.10f, %.10f != %.10f",
          tick, y, offsetY, deltaY, predictedY));

        // First correct gravity tick → Geyser player has spawned
        if (user.isGeyser()) {
          bedrockSpawned = true;
        }
      }
      tick++;
    } catch (CorruptedFrameException exception) {
      // Do not throw the exception if the user configured to display the CAPTCHA instead
      if (Sonar.get().getConfig().getVerification().getGravity().isCaptchaOnFail()) {
        handleCAPTCHA();
        return;
      }
      // Interrupt the netty eventLoop to immediately stop further processing
      throw new DecoderException(exception);
    }
  }

  // TODO: Maybe recode the flow with a better state system?
  private void vehicleCheckOrNext() {
    if (performVehicle) {
      // Initialize the vehicle check
      handleVehicleCheck();
      return;
    }
    captchaOrNext();
  }

  private void captchaOrNext() {
    if (performCaptcha) {
      // Initialize the map captcha
      handleCAPTCHA();
      return;
    }
    // Finish the verification
    finish();
  }

  private void handleVehicleCheck() {
    // Set the state to VEHICLE, so we don't handle any unnecessary packets
    user.setState(VEHICLE);
    // Send the necessary packets to mount the player on the vehicle
    final int vehicleType = EntityType.BOAT.getId(user.getProtocolVersion());
    user.delayedWrite(new SpawnEntityPacket(VEHICLE_ENTITY_ID, vehicleType, posX, posY, posZ));
    user.delayedWrite(setPassengers);
    user.getChannel().flush();
  }

  private void handleCAPTCHA() {
    // Set the state to CAPTCHA, so we don't handle any unnecessary packets
    user.setState(CAPTCHA);
    if (!MAP_INFO_PREPARER.isCaptchaAvailable()) {
      // This should not happen, but we have to return if there is no captcha prepared
      user.disconnect(Sonar.get().getConfig().getVerification().getCurrentlyPreparing());
      return;
    }
    if (!performGravity
      && user.getProtocolVersion().compareTo(MINECRAFT_1_18_2) >= 0) {
      // Make sure the player escapes the 1.18.2+ "Loading terrain" screen
      user.delayedWrite(CAPTCHA_SPAWN_POSITION);
    }
    if (performVehicle) {
      // Make sure the player exits the vehicle before sending the CAPTCHA
      user.delayedWrite(removeEntities);
    }
    // Reset max tries
    captchaTriesLeft = Sonar.get().getConfig().getVerification().getMap().getMaxTries();

    // Set slot to map
    user.delayedWrite(new SetSlotPacket(36, 1,
      ItemType.FILLED_MAP.getId(user.getProtocolVersion()), SetSlotPacket.MAP_NBT));
    // Send random captcha to the player
    captcha = MAP_INFO_PREPARER.getRandomCaptcha();
    Objects.requireNonNull(captcha).delayedWrite(user);
    // Teleport the player to the position above the platform
    user.delayedWrite(CAPTCHA_POSITION);
    // Make sure the player cannot move
    user.delayedWrite(user.isGeyser() ? CAPTCHA_ABILITIES_BEDROCK : CAPTCHA_ABILITIES);
    // Make sure the player knows what to do
    user.delayedWrite(enterCodeMessage);
    // Send all packets in one flush
    user.getChannel().flush();
  }

  private void finish() {
    // Make sure we drop all further packets
    user.setState(SUCCESS);

    // Increment amount of total successful verifications
    GlobalSonarStatistics.totalSuccessfulVerifications++;

    // Add verified player to the database
    final VerifiedPlayer verifiedPlayer = new VerifiedPlayer(user.getInetAddress().toString(), playerUuid,
      login.getStart());
    Sonar.get().getVerifiedPlayerController().add(verifiedPlayer);

    // Call the VerifySuccessEvent for external API usage
    Sonar.get().getEventManager().publish(new UserVerifySuccessEvent(username, playerUuid, user, login.delay()));

    // If enabled, transfer the player back to the origin server
    // This feature was introduced by Mojang in Minecraft version 1.20.5
    // Thanks a lot to @Kadeluxe for helping me out with this!
    if (transfer) {
      user.write(transferToOrigin);
    }

    // Disconnect player with the verification success message
    user.disconnect(Sonar.get().getConfig().getVerification().getVerificationSuccess());

    FALLBACK.getLogger().info(Sonar.get().getConfig().getVerification().getSuccessLog()
      .replace("%name%", username)
      .replace("%time%", login.toString()));
  }

  /**
   * Fails the verification if a certain state is unexpected.
   *
   * @param expectedState Expected state
   */
  private void assertState(final @NotNull FallbackUserState expectedState) {
    final FallbackUserState state = user.getState();
    checkFrame(state == expectedState, "expected " + expectedState + ", got " + state);
  }

  /**
   * Checks if a certain condition is met, fails the verification if not.
   *
   * @param condition Condition to fail if it's false
   * @param message   Messages displayed in the stacktrace
   */
  private void checkFrame(final boolean condition, final @NotNull String message) {
    if (!condition) {
      // Only fail the player if we are either not in the POSITION state,
      // or if the user configured Sonar to display a CAPTCHA instead of failing.
      if (user.getState() != POSITION
        || !Sonar.get().getConfig().getVerification().getGravity().isCaptchaOnFail()) {
        user.fail(message);
      }
      throw new CorruptedFrameException(message);
    }
  }
}
