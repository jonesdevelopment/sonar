/*
 * Copyright (C) 2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.protocol;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.captcha.StandardCaptchaGenerator;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockType;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockUpdate;
import xyz.jonesdev.sonar.common.fallback.protocol.captcha.CaptchaPreparer;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.entity.EntityType;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration.FinishConfigurationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration.RegistryDataPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginSuccessPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;
import xyz.jonesdev.sonar.common.util.ComponentHolder;

import java.security.SecureRandom;
import java.util.UUID;

@UtilityClass
public class FallbackPreparer {
  private final SecureRandom RANDOM = new SecureRandom();
  // LoginSuccess
  public FallbackPacket loginSuccess;
  // Abilities
  public final FallbackPacket DEFAULT_ABILITIES = new FallbackPacketSnapshot(new PlayerAbilitiesPacket(0x00, 0f, 0f));
  public final FallbackPacket CAPTCHA_ABILITIES = new FallbackPacketSnapshot(new PlayerAbilitiesPacket(0x02, 0f, 0f));
  public final FallbackPacket CAPTCHA_ABILITIES_BEDROCK = new FallbackPacketSnapshot(new PlayerAbilitiesPacket(0x06, 0f, 0f));
  // Chunks
  public final FallbackPacket EMPTY_CHUNK_DATA = new FallbackPacketSnapshot(new ChunkDataPacket(0, 0));
  // Finish Configuration
  public final FallbackPacket FINISH_CONFIGURATION = new FinishConfigurationPacket(); // No snapshot needed because it's empty either way
  // Synchronize Registry
  public final FallbackPacket REGISTRY_SYNC_LEGACY = new FallbackPacketSnapshot(new RegistryDataPacket(DimensionRegistry.CODEC_1_20, null, null));
  public final FallbackPacket[] REGISTRY_SYNC_1_20_5 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_20);
  public final FallbackPacket[] REGISTRY_SYNC_1_21 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_21);
  // Keep Alive
  public final FallbackPacket CAPTCHA_KEEP_ALIVE = new FallbackPacketSnapshot(new KeepAlivePacket(-1337L));
  // Game Event (1.20.3+)
  public final FallbackPacket START_WRITING_CHUNKS = new FallbackPacketSnapshot(new GameEventPacket(13, 0));
  // Chat
  public FallbackPacket enterCodeMessage;
  public FallbackPacket incorrectCaptcha;
  // JoinGame
  public final int PLAYER_ENTITY_ID = RANDOM.nextInt(10);
  public FallbackPacket joinGame;
  // Update Section Blocks
  public FallbackPacket updateSectionBlocks;
  // Default Spawn Position
  public FallbackPacket defaultSpawnPosition;
  // Spawn Position
  public final int TELEPORT_ID = RANDOM.nextInt();
  public FallbackPacket spawnPosition;
  // Transfer packet
  public static FallbackPacket transferToOrigin;

  // Disconnect messages
  public FallbackPacket blacklisted;
  public FallbackPacket alreadyQueued;
  public FallbackPacket alreadyVerifying;
  public FallbackPacket reconnectedTooFast;
  public FallbackPacket protocolBlacklisted;
  public FallbackPacket invalidUsername;
  public FallbackPacket tooManyOnlinePerIP;

  // Vehicle
  public FallbackPacket removeEntities;
  public FallbackPacket spawnEntity;
  public static FallbackPacket setPassengers;
  public static final int VEHICLE_ENTITY_ID = PLAYER_ENTITY_ID + 1 + RANDOM.nextInt(10);

  // Collisions
  public final int BLOCKS_PER_ROW = 8; // 8 * 8 = 64 (protocol maximum)
  public final int SPAWN_X_POSITION = 16 / 2; // middle of the chunk
  public final int SPAWN_Z_POSITION = 16 / 2; // middle of the chunk
  public final int DEFAULT_Y_COLLIDE_POSITION = 155 + RANDOM.nextInt(101); // 255 is the maximum Y position
  public final int IN_AIR_Y_POSITION = 1337;

  // CAPTCHA position
  public final FallbackPacket CAPTCHA_POSITION = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
    SPAWN_X_POSITION, IN_AIR_Y_POSITION, SPAWN_Z_POSITION, 0, 90, 0, false));

  // Platform
  public BlockType blockType;
  public int maxMovementTick, dynamicSpawnYPosition;
  public double maxFallDistance;

  // XP packets
  public FallbackPacket[] xpCountdown;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void prepare() {
    // Preload the packet registry to avoid CPU/RAM issues on 1st connection
    Sonar.get().getLogger().info("Preloading all registered packets...");
    FallbackPacketRegistry.values();

    // Prepare LoginSuccess packet
    loginSuccess = new FallbackPacketSnapshot(new LoginSuccessPacket(UUID.randomUUID(),
      Sonar.get().getConfig().getGeneralConfig().getString("verification.cached-username")));

    // Prepare JoinGame packet
    joinGame = new FallbackPacketSnapshot(new JoinGamePacket(PLAYER_ENTITY_ID,
      Sonar.get().getConfig().getVerification().getGamemode().getId(),
      RANDOM.nextLong(), false, 0,
      true, false, false,
      new String[]{"minecraft:overworld"}, "minecraft:overworld"));

    // Prepare the gravity check
    maxMovementTick = Sonar.get().getConfig().getVerification().getGravity().getMaxMovementTicks();
    maxFallDistance = 0;

    double motionY = -0.08 * 0.98f;
    for (int i = 0; i < maxMovementTick; i++) {
      motionY = (motionY - 0.08) * 0.98f;
      maxFallDistance += Math.abs(motionY);
    }

    // Set the dynamic block and collide Y position based on the maximum fall distance
    dynamicSpawnYPosition = DEFAULT_Y_COLLIDE_POSITION + (int) Math.ceil(maxFallDistance);
    defaultSpawnPosition = new FallbackPacketSnapshot(new SetDefaultSpawnPositionPacket(
      SPAWN_X_POSITION, dynamicSpawnYPosition, SPAWN_Z_POSITION));
    spawnPosition = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
      SPAWN_X_POSITION, dynamicSpawnYPosition, SPAWN_Z_POSITION,
      0, -90, TELEPORT_ID, false));

    // Prepare collision platform positions
    blockType = BlockType.valueOf(Sonar.get().getConfig().getGeneralConfig().getString(
      "verification.checks.collision.collision-block-type").toUpperCase());
    final BlockUpdate[] changedBlocks = new BlockUpdate[BLOCKS_PER_ROW * BLOCKS_PER_ROW];

    int index = 0;
    for (int x = 0; x < BLOCKS_PER_ROW; x++) {
      for (int z = 0; z < BLOCKS_PER_ROW; z++) {
        final BlockUpdate.BlockPosition position = new BlockUpdate.BlockPosition(
          x + (BLOCKS_PER_ROW / 2),
          DEFAULT_Y_COLLIDE_POSITION,
          z + (BLOCKS_PER_ROW / 2),
          0, 0);
        changedBlocks[index++] = new BlockUpdate(position, blockType);
      }
    }

    updateSectionBlocks = new FallbackPacketSnapshot(new UpdateSectionBlocksPacket(0, 0, changedBlocks));

    // Prepare disconnect packets during login
    blacklisted = new FallbackPacketSnapshot(DisconnectPacket.create(Sonar.get().getConfig().getVerification().getBlacklisted(), true));
    alreadyVerifying = new FallbackPacketSnapshot(DisconnectPacket.create(Sonar.get().getConfig().getVerification().getAlreadyVerifying(), true));
    alreadyQueued = new FallbackPacketSnapshot(DisconnectPacket.create(Sonar.get().getConfig().getVerification().getAlreadyQueued(), true));
    protocolBlacklisted = new FallbackPacketSnapshot(DisconnectPacket.create(Sonar.get().getConfig().getVerification().getProtocolBlacklisted(), true));
    reconnectedTooFast = new FallbackPacketSnapshot(DisconnectPacket.create(Sonar.get().getConfig().getVerification().getTooFastReconnect(), true));
    invalidUsername = new FallbackPacketSnapshot(DisconnectPacket.create(Sonar.get().getConfig().getVerification().getInvalidUsername(), true));
    tooManyOnlinePerIP = new FallbackPacketSnapshot(DisconnectPacket.create(Sonar.get().getConfig().getTooManyOnlinePerIp(), true));

    // Prepare transfer packet
    if (Sonar.get().getConfig().getGeneralConfig().getBoolean("verification.transfer.enabled")) {
      transferToOrigin = new FallbackPacketSnapshot(new TransferPacket(
        Sonar.get().getConfig().getGeneralConfig().getString("verification.transfer.destination-host"),
        Sonar.get().getConfig().getGeneralConfig().getInt("verification.transfer.destination-port")));
    } else {
      transferToOrigin = null;
    }

    // Prepare packets for the vehicle check
    removeEntities = new FallbackPacketSnapshot(new RemoveEntitiesPacket(VEHICLE_ENTITY_ID));
    spawnEntity = new FallbackPacketSnapshot(new SpawnEntityPacket(
      VEHICLE_ENTITY_ID, EntityType.BOAT, SPAWN_X_POSITION, IN_AIR_Y_POSITION, SPAWN_Z_POSITION));
    setPassengers = new FallbackPacketSnapshot(new SetPassengersPacket(VEHICLE_ENTITY_ID, PLAYER_ENTITY_ID));

    if (Sonar.get().getConfig().getVerification().getMap().getTiming() != SonarConfiguration.Verification.Timing.NEVER
      || Sonar.get().getConfig().getVerification().getGravity().isCaptchaOnFail()) {
      // Prepare CAPTCHA messages
      enterCodeMessage = new FallbackPacketSnapshot(new SystemChatPacket(new ComponentHolder(
        MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("verification.captcha.enter"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())))));
      incorrectCaptcha = new FallbackPacketSnapshot(new SystemChatPacket(new ComponentHolder(
        MiniMessage.miniMessage().deserialize(
          Sonar.get().getConfig().getMessagesConfig().getString("verification.captcha.incorrect"),
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())))));

      // Prepare countdown
      xpCountdown = new FallbackPacket[Sonar.get().getConfig().getVerification().getMap().getMaxDuration() / 1000];

      for (int i = 0; i < xpCountdown.length; i++) {
        final float bar = (float) i / xpCountdown.length;
        xpCountdown[i] = new FallbackPacketSnapshot(new SetExperiencePacket(bar, i, 0));
      }

      // Update the CAPTCHA generator if necessary
      if (Sonar.get().getFallback().getCaptchaGenerator() == null
        || Sonar.get().getFallback().getCaptchaGenerator() instanceof StandardCaptchaGenerator) {
        Sonar.get().getFallback().setCaptchaGenerator(new StandardCaptchaGenerator(128, 128,
          Sonar.get().getConfig().getVerification().getMap().getBackgroundImage()));
      } else {
        Sonar.get().getLogger().info("Custom CAPTCHA generator detected, skipping reinitialization.");
      }

      // Prepare CAPTCHA answers
      CaptchaPreparer.prepare();
    } else {
      // Throw away if not needed
      enterCodeMessage = null;
      incorrectCaptcha = null;
      xpCountdown = null;
      Sonar.get().getFallback().setCaptchaGenerator(null);
    }
  }
}
