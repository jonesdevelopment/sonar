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

package xyz.jonesdev.sonar.common.fallback.protocol;

import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockPosition;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockType;
import xyz.jonesdev.sonar.common.fallback.protocol.block.ChangedBlock;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.map.CaptchaPreparer;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.config.FinishConfigurationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.config.RegistryDataPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginSuccessPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class FallbackPreparer {

  // LoginSuccess
  public final FallbackPacket LOGIN_SUCCESS = new LoginSuccessPacket();
  // Abilities
  public final FallbackPacket DEFAULT_ABILITIES = new ClientAbilitiesPacket(0x00, 0f, 0f);
  public final FallbackPacket CAPTCHA_ABILITIES = new ClientAbilitiesPacket(0x02, 0f, 0f);
  public final FallbackPacket CAPTCHA_ABILITIES_BEDROCK = new ClientAbilitiesPacket(0x06, 0f, 0f);
  // Chunks
  public final FallbackPacket EMPTY_CHUNK_DATA = new ChunkDataPacket(0, 0);
  // Finish Configuration
  public final FallbackPacket FINISH_CONFIGURATION = new FinishConfigurationPacket();
  // Synchronize Registry
  public final FallbackPacket REGISTRY_SYNC_LEGACY = new RegistryDataPacket(DimensionRegistry.CODEC_1_20, null, null);
  public final List<FallbackPacket> REGISTRY_SYNC_1_20_5 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_20);
  // Keep Alive
  public final FallbackPacket CAPTCHA_KEEP_ALIVE = new KeepAlivePacket(0L);
  // Game Event (1.20.3+)
  public final FallbackPacket START_WRITING_CHUNKS = new GameEventPacket(13, 0);
  // Chat
  public FallbackPacket enterCodeMessage;
  public FallbackPacket incorrectCaptcha;
  // JoinGame
  public final int PLAYER_ENTITY_ID = ThreadLocalRandom.current().nextInt(10);
  public FallbackPacket joinGame;
  // Update Section Blocks
  public FallbackPacket updateSectionBlocks;
  // Default Spawn Position
  public FallbackPacket dynamicSpawnPosition;
  // Transfer packet
  public FallbackPacket transferToOrigin;

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
  public FallbackPacket setPassengers;
  public static final int VEHICLE_ENTITY_ID = PLAYER_ENTITY_ID + 1;

  // Collisions
  public final int BLOCKS_PER_ROW = 8; // 8 * 8 = 64 (protocol maximum)
  public final int SPAWN_X_POSITION = 16 / 2; // middle of the chunk
  public final int SPAWN_Z_POSITION = 16 / 2; // middle of the chunk
  public final int DEFAULT_Y_COLLIDE_POSITION = 255; // 255 is the maximum Y position allowed

  // Captcha position
  public final FallbackPacket CAPTCHA_POSITION = new PlayerPositionLookPacket(
    SPAWN_X_POSITION, 1337, SPAWN_Z_POSITION, 0f, 90f, 0, false);
  public final FallbackPacket CAPTCHA_SPAWN_POSITION = new DefaultSpawnPositionPacket(
    SPAWN_X_POSITION, 1337, SPAWN_Z_POSITION);

  // Blocks
  private final ChangedBlock[] CHANGED_BLOCKS = new ChangedBlock[BLOCKS_PER_ROW * BLOCKS_PER_ROW];
  public BlockType blockType = BlockType.BARRIER;

  public int maxMovementTick, dynamicSpawnYPosition;
  public double[] preparedCachedYMotions;
  public double maxFallDistance;

  // CAPTCHA
  public final CaptchaPreparer MAP_INFO_PREPARER = new CaptchaPreparer();

  // XP packets
  public FallbackPacket[] xpCountdown;

  public void prepare() {
    // Prepare JoinGame packet
    joinGame = new JoinGamePacket(PLAYER_ENTITY_ID,
      Sonar.get().getConfig().getVerification().getGravity().getGamemode().getId(),
      0, false, 0,
      true, false, false,
      new String[]{"minecraft:overworld"}, "minecraft:overworld");

    // Prepare cached motions for the gravity check
    maxFallDistance = 0;
    maxMovementTick = Sonar.get().getConfig().getVerification().getGravity().getMaxMovementTicks();
    preparedCachedYMotions = new double[maxMovementTick + 8];

    for (int i = 0; i < preparedCachedYMotions.length; i++) {
      final double gravity = -((Math.pow(0.98, i) - 1) * 3.92);
      preparedCachedYMotions[i] = gravity;
      maxFallDistance += gravity;
    }

    // Set the dynamic block and collide Y position based on the maximum fall distance
    dynamicSpawnYPosition = DEFAULT_Y_COLLIDE_POSITION + (int) Math.ceil(maxFallDistance);
    dynamicSpawnPosition = new DefaultSpawnPositionPacket(SPAWN_X_POSITION, dynamicSpawnYPosition, SPAWN_Z_POSITION);

    // Prepare collision platform positions
    int index = 0;
    for (int x = 0; x < BLOCKS_PER_ROW; x++) {
      for (int z = 0; z < BLOCKS_PER_ROW; z++) {
        final BlockPosition position = new BlockPosition(
          x + (BLOCKS_PER_ROW / 2),
          DEFAULT_Y_COLLIDE_POSITION,
          z + (BLOCKS_PER_ROW / 2),
          0, 0);
        CHANGED_BLOCKS[index++] = new ChangedBlock(position, blockType);
      }
    }

    // Prepare UpdateSectionBlocks packet
    updateSectionBlocks = new UpdateSectionBlocksPacket(0, 0, CHANGED_BLOCKS);

    // Prepare disconnect packets during login
    blacklisted = DisconnectPacket.create(Sonar.get().getConfig().getVerification().getBlacklisted(), true);
    alreadyVerifying = DisconnectPacket.create(Sonar.get().getConfig().getVerification().getAlreadyVerifying(), true);
    alreadyQueued = DisconnectPacket.create(Sonar.get().getConfig().getVerification().getAlreadyQueued(), true);
    protocolBlacklisted = DisconnectPacket.create(Sonar.get().getConfig().getVerification().getProtocolBlacklisted(), true);
    reconnectedTooFast = DisconnectPacket.create(Sonar.get().getConfig().getVerification().getTooFastReconnect(), true);
    invalidUsername = DisconnectPacket.create(Sonar.get().getConfig().getVerification().getInvalidUsername(), true);
    tooManyOnlinePerIP = DisconnectPacket.create(Sonar.get().getConfig().getTooManyOnlinePerIp(), true);

    // Prepare transfer packet
    transferToOrigin = new TransferPacket(
      Sonar.get().getConfig().getVerification().getTransfer().getHost(),
      Sonar.get().getConfig().getVerification().getTransfer().getPort());

    // Prepare packets for the vehicle check
    removeEntities = new RemoveEntitiesPacket(VEHICLE_ENTITY_ID);
    setPassengers = new SetPassengersPacket(VEHICLE_ENTITY_ID, PLAYER_ENTITY_ID);

    if (Sonar.get().getConfig().getVerification().getMap().getTiming() != SonarConfiguration.Verification.Timing.NEVER
      || Sonar.get().getConfig().getVerification().getGravity().isCaptchaOnFail()) {
      // Prepare CAPTCHA messages
      enterCodeMessage = new UniversalChatPacket(Sonar.get().getConfig().getVerification().getMap().getEnterCode(), UniversalChatPacket.SYSTEM_TYPE);
      incorrectCaptcha = new UniversalChatPacket(
        Sonar.get().getConfig().getVerification().getMap().getFailedCaptcha(), UniversalChatPacket.SYSTEM_TYPE);

      // Prepare countdown
      xpCountdown = new SetExperiencePacket[Sonar.get().getConfig().getVerification().getMap().getMaxDuration() / 1000];

      for (int i = 0; i < xpCountdown.length; i++) {
        xpCountdown[i] = new SetExperiencePacket((float) i / xpCountdown.length, i, i);
      }

      // Prepare CAPTCHA answers
      MAP_INFO_PREPARER.prepare();
    }
  }
}
