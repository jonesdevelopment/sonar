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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.event.impl.CaptchaGenerationStartEvent;
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

import java.util.Random;
import java.util.UUID;

@UtilityClass
public class FallbackPreparer {
  private final Random RANDOM = new Random();

  public static final int PLAYER_ENTITY_ID = RANDOM.nextInt();
  public static final int VEHICLE_ENTITY_ID = RANDOM.nextInt();

  public static final CompoundBinaryTag MAP_ITEM_NBT = CompoundBinaryTag.builder()
    .put("map", IntBinaryTag.intBinaryTag(0)) // map type
    .build();

  public static final BlockType[] POSSIBLE_BLOCK_TYPES = BlockType.values();
  public final FallbackPacket[] BLOCKS_PACKETS = new FallbackPacket[POSSIBLE_BLOCK_TYPES.length];

  public final int BLOCKS_PER_ROW = 8; // 8 * 8 = 64 (vanilla protocol maximum)
  public final int SPAWN_X_POSITION = 16 / 2; // middle of the chunk
  public final int SPAWN_Z_POSITION = 16 / 2; // middle of the chunk
  public final int DEFAULT_Y_COLLIDE_POSITION = 1 + RANDOM.nextInt(255); // 255 is the maximum Y position
  public final int IN_AIR_Y_POSITION = 1000 + RANDOM.nextInt(338); // High altitude (randomized)
  public final int IN_VOID_Y_POSITION = -(120 + RANDOM.nextInt(120)); // Low altitude (randomized)

  public final int FIRST_TELEPORT_ID = RANDOM.nextInt();
  public final int SECOND_TELEPORT_ID = RANDOM.nextInt();

  public final FallbackPacket DEFAULT_ABILITIES = new PlayerAbilitiesPacket(0x00, 0, 0);
  public final FallbackPacket NO_MOVE_ABILITIES = new PlayerAbilitiesPacket(0x02, 0, 0);
  public final FallbackPacket NO_MOVE_ABILITIES_BEDROCK = new PlayerAbilitiesPacket(0x06, 0, 0);
  public final FallbackPacket CAPTCHA_POSITION = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
    SPAWN_X_POSITION, IN_AIR_Y_POSITION, SPAWN_Z_POSITION, 0, 90, 0, 0, false));
  public final FallbackPacket EMPTY_CHUNK_DATA = new FallbackPacketSnapshot(new ChunkDataPacket(0, 0));
  public final FallbackPacket FINISH_CONFIGURATION = new FinishConfigurationPacket();
  public final FallbackPacket REGISTRY_SYNC_LEGACY = new FallbackPacketSnapshot(new RegistryDataPacket(DimensionRegistry.CODEC_1_20, null, null));
  public final FallbackPacket[] REGISTRY_SYNC_1_20_5 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_20);
  public final FallbackPacket[] REGISTRY_SYNC_1_21 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_21);
  public final FallbackPacket START_WRITING_CHUNKS = new GameEventPacket(13, 0);
  public final static FallbackPacket INVALID_HELD_ITEM_SLOT = new SetHeldItemPacket(-1);
  public final FallbackPacket CAPTCHA_KEEP_ALIVE = new KeepAlivePacket(RANDOM.nextInt());

  public static FallbackPacket loginSuccess;
  public FallbackPacket welcomeMessage;
  public FallbackPacket enterCodeMessage;
  public FallbackPacket incorrectCaptcha;
  public static FallbackPacket joinGame;
  public FallbackPacket defaultSpawnPosition;
  public FallbackPacket spawnPosition;
  public FallbackPacket fallStartPosition;
  public FallbackPacket fallStartPositionLegacy;
  public static FallbackPacket transferToOrigin;
  public FallbackPacket blacklisted;
  public FallbackPacket alreadyQueued;
  public FallbackPacket alreadyVerifying;
  public FallbackPacket reconnectedTooFast;
  public FallbackPacket protocolBlacklisted;
  public FallbackPacket invalidUsername;
  public FallbackPacket tooManyOnlinePerIP;
  public FallbackPacket removeBoat;
  public FallbackPacket teleportMinecart;
  public FallbackPacket spawnBoatEntity;
  public FallbackPacket spawnMinecartEntity;
  public FallbackPacket setPassengers;
  public FallbackPacket[] xpCountdown;

  public static int maxMovementTick, dynamicSpawnYPosition, maxTotalPacketsSent;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void prepare() {
    // Preload the packet registry to avoid CPU/RAM issues on 1st connection
    Sonar.get().getLogger().info("Preloading all registered packets...");
    FallbackPacketRegistry.values();

    // Prepare LoginSuccess packet with capped username to 16 characters
    final UUID uuid = UUID.randomUUID();
    String username = Sonar.get().getConfig().getGeneralConfig().getString("verification.cached-username");
    if (username.length() > 16) {
      username = username.substring(0, 16);
    }
    loginSuccess = new FallbackPacketSnapshot(new LoginSuccessPacket(uuid, username, true));

    // Prepare JoinGame packet
    joinGame = new FallbackPacketSnapshot(new JoinGamePacket(PLAYER_ENTITY_ID,
      Sonar.get().getConfig().getVerification().getGamemode().getId(), RANDOM.nextLong(),
      0, 0, false, true, false,
      false, false, false, true,
      new String[]{"minecraft:overworld"}, "minecraft:overworld", "flat"));

    // Prepare the gravity check
    maxMovementTick = Sonar.get().getConfig().getVerification().getGravity().getMaxMovementTicks();

    double motionY = 0, fallDistance = 0;

    for (int i = 0; i < maxMovementTick; i++) {
      motionY = (motionY - 0.1) * 0.98f;
      fallDistance += Math.abs(motionY);
    }

    // Set the dynamic block and collide Y position based on the maximum fall distance
    dynamicSpawnYPosition = DEFAULT_Y_COLLIDE_POSITION + (int) Math.ceil(fallDistance);
    defaultSpawnPosition = new FallbackPacketSnapshot(new SetDefaultSpawnPositionPacket(
      SPAWN_X_POSITION, IN_AIR_Y_POSITION, SPAWN_Z_POSITION));
    spawnPosition = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
      SPAWN_X_POSITION, IN_AIR_Y_POSITION, SPAWN_Z_POSITION,
      0, 0, FIRST_TELEPORT_ID, 0, false));
    fallStartPosition = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
      SPAWN_X_POSITION, dynamicSpawnYPosition - IN_AIR_Y_POSITION, SPAWN_Z_POSITION,
      0, -90, SECOND_TELEPORT_ID, 1 << 1, false));
    fallStartPositionLegacy = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
      SPAWN_X_POSITION, dynamicSpawnYPosition, SPAWN_Z_POSITION,
      0, -90, 0, 0, false));

    // Prepare collision platform positions
    final int length = BLOCKS_PER_ROW / 2;
    for (int i = 0; i < BLOCKS_PACKETS.length; i++) {
      final BlockUpdate[] blocks = new BlockUpdate[BLOCKS_PER_ROW * BLOCKS_PER_ROW];

      int index = 0;
      for (int x = 0; x < BLOCKS_PER_ROW; x++) {
        for (int z = 0; z < BLOCKS_PER_ROW; z++) {
          final BlockUpdate.BlockPosition position = new BlockUpdate.BlockPosition(
            x + length, DEFAULT_Y_COLLIDE_POSITION, z + length, 0, 0);
          blocks[index++] = new BlockUpdate(position, POSSIBLE_BLOCK_TYPES[i]);
        }
      }
      BLOCKS_PACKETS[i] = new UpdateSectionBlocksPacket(0, 0, blocks);
    }

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
    removeBoat = new RemoveEntitiesPacket(VEHICLE_ENTITY_ID);
    teleportMinecart = new TeleportEntityPacket(
      VEHICLE_ENTITY_ID, SPAWN_X_POSITION, IN_VOID_Y_POSITION, SPAWN_Z_POSITION, false);
    spawnBoatEntity = new FallbackPacketSnapshot(new SpawnEntityPacket(VEHICLE_ENTITY_ID,
      EntityType.BOAT, RANDOM.nextInt(16), IN_AIR_Y_POSITION, RANDOM.nextInt(16),
      0, 0, 0, 0));
    spawnMinecartEntity = new FallbackPacketSnapshot(new SpawnEntityPacket(VEHICLE_ENTITY_ID,
      EntityType.MINECART, RANDOM.nextInt(16), IN_AIR_Y_POSITION, RANDOM.nextInt(16),
      0, 0, 0, 0));
    setPassengers = new FallbackPacketSnapshot(new SetPassengersPacket(VEHICLE_ENTITY_ID, PLAYER_ENTITY_ID));

    // If the welcome message is empty, we don't need to send a message to the player
    final String welcome = Sonar.get().getConfig().getMessagesConfig().getString("verification.welcome");
    if (welcome.isEmpty()) {
      welcomeMessage = null;
    } else {
      welcomeMessage = new FallbackPacketSnapshot(new SystemChatPacket(new ComponentHolder(
        MiniMessage.miniMessage().deserialize(welcome,
          Placeholder.component("prefix", Sonar.get().getConfig().getPrefix())))));
    }

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
      final CaptchaGenerationStartEvent generationStartEvent = new CaptchaGenerationStartEvent(
        Sonar.get().getFallback().getCaptchaGenerator());
      Sonar.get().getEventManager().publish(generationStartEvent);

      if (generationStartEvent.getCaptchaGenerator() == null
        || generationStartEvent.getCaptchaGenerator() instanceof StandardCaptchaGenerator) {
        Sonar.get().getFallback().setCaptchaGenerator(new StandardCaptchaGenerator(
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

    maxTotalPacketsSent = maxMovementTick + 2
      + (xpCountdown == null ? 0 : xpCountdown.length) * 20 + 5
      + Sonar.get().getConfig().getVerification().getVehicle().getMinimumPackets() * 4
      + Sonar.get().getConfig().getVerification().getMap().getMaxTries()
      + 150 /* some arbitrary leeway */;
  }
}
