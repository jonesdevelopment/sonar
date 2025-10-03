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

package xyz.jonesdev.sonar.common.fallback.protocol;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.event.impl.CaptchaGenerationStartEvent;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.captcha.StandardCaptchaGenerator;
import xyz.jonesdev.sonar.captcha.legacy.LegacyCaptchaGenerator;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockType;
import xyz.jonesdev.sonar.common.fallback.protocol.block.BlockUpdate;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionRegistry;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionType;
import xyz.jonesdev.sonar.common.fallback.protocol.entity.EntityType;
import xyz.jonesdev.sonar.common.fallback.protocol.item.ItemType;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration.RegistryDataPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginSuccessPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;
import xyz.jonesdev.sonar.common.util.ComponentHolder;

import java.io.File;
import java.util.Random;
import java.util.UUID;

@UtilityClass
public class FallbackPreparer {
  private final Random RANDOM = new Random();

  public static final int PLAYER_ENTITY_ID = RANDOM.nextInt();
  public static final int VEHICLE_ENTITY_ID = RANDOM.nextInt();

  public static final BlockType[] POSSIBLE_BLOCK_TYPES = BlockType.values();
  public final FallbackPacket[] BLOCKS_PACKETS = new FallbackPacket[POSSIBLE_BLOCK_TYPES.length];

  public final int BLOCKS_PER_ROW = 8; // 8 * 8 = 64 (vanilla protocol maximum)
  public final int SPAWN_X_POSITION = 16 / 2; // middle of the chunk
  public final int SPAWN_Z_POSITION = 16 / 2; // middle of the chunk
  public final int PLATFORM_Y_POSITION = 1 + RANDOM.nextInt(255); // 255 is the maximum Y position
  public final int IN_AIR_Y_POSITION = 3000 + RANDOM.nextInt(500); // Random high altitude

  public final int FIRST_TELEPORT_ID = RANDOM.nextInt();
  public final int SECOND_TELEPORT_ID = RANDOM.nextInt();
  public final int PRE_JOIN_KEEP_ALIVE_ID = RANDOM.nextInt();

  private final int MAP_SLOT = RANDOM.nextInt(9);

  public static final FallbackPacket CAPTCHA_SET_CONTAINER_SLOT = new SetContainerSlotPacket(
    0, 36 + MAP_SLOT, 1, ItemType.FILLED_MAP, CompoundBinaryTag.builder()
    .put("map", IntBinaryTag.intBinaryTag(0)) // map id
    .build());
  public final static FallbackPacket CAPTCHA_HELD_ITEM_SLOT = new SetHeldItemPacket(MAP_SLOT);
  public final FallbackPacket DEFAULT_ABILITIES = new PlayerAbilitiesPacket(0x00, 0, 0);
  public final FallbackPacket NO_MOVE_ABILITIES = new PlayerAbilitiesPacket(0x02, 0, 0);
  public final FallbackPacket NO_MOVE_ABILITIES_BEDROCK = new PlayerAbilitiesPacket(0x06, 0, 0);
  public final FallbackPacket CAPTCHA_POSITION = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
    SPAWN_X_POSITION, 10000, SPAWN_Z_POSITION, 0, 90, 0, 0, false, false, true));
  // I think the people working at Microsoft are actually the *real* ultimate trolls of the internet!
  public final FallbackPacket[] EMPTY_CHUNK_DATA = new FallbackPacket[]{
    new FallbackPacketSnapshot(new ChunkDataPacket(0, 0)),
    new FallbackPacketSnapshot(new ChunkDataPacket(0, 1)),
    new FallbackPacketSnapshot(new ChunkDataPacket(0, -1)),
    new FallbackPacketSnapshot(new ChunkDataPacket(1, 0)),
    new FallbackPacketSnapshot(new ChunkDataPacket(1, 1)),
    new FallbackPacketSnapshot(new ChunkDataPacket(1, -1)),
    new FallbackPacketSnapshot(new ChunkDataPacket(-1, 0)),
    new FallbackPacketSnapshot(new ChunkDataPacket(-1, 1)),
    new FallbackPacketSnapshot(new ChunkDataPacket(-1, -1))
  };
  public final FallbackPacket PRE_JOIN_KEEP_ALIVE = new FallbackPacketSnapshot(new KeepAlivePacket(PRE_JOIN_KEEP_ALIVE_ID));
  public final FallbackPacket[] REGISTRY_SYNC_1_20 = new FallbackPacket[] {
    new FallbackPacketSnapshot(new RegistryDataPacket(DimensionRegistry.CODEC_1_20, null, null))};
  public final FallbackPacket[] REGISTRY_SYNC_1_20_5 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_20_5);
  public final FallbackPacket[] REGISTRY_SYNC_1_21 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_21);
  public final FallbackPacket[] REGISTRY_SYNC_1_21_2 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_21_2);
  public final FallbackPacket[] REGISTRY_SYNC_1_21_4 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_21_4);
  public final FallbackPacket[] REGISTRY_SYNC_1_21_5 = RegistryDataPacket.of(DimensionRegistry.CODEC_1_21_5);
  public final FallbackPacket START_WRITING_CHUNKS = new GameEventPacket(13, 0);
  public final static FallbackPacket INVALID_HELD_ITEM_SLOT = new SetHeldItemPacket(-1);
  public final FallbackPacket RANDOM_KEEP_ALIVE = new FallbackPacketSnapshot(new KeepAlivePacket(RANDOM.nextInt()));
  public static final FallbackPacket SPAWN_BOAT_ENTITY = new FallbackPacketSnapshot(new SpawnEntityPacket(
    VEHICLE_ENTITY_ID, EntityType.BOAT, SPAWN_X_POSITION, IN_AIR_Y_POSITION, SPAWN_Z_POSITION,
    0, 0, 0, 0));
  public static final FallbackPacket SPAWN_MINECART_ENTITY = new FallbackPacketSnapshot(new SpawnEntityPacket(
    VEHICLE_ENTITY_ID, EntityType.MINECART, SPAWN_X_POSITION, IN_AIR_Y_POSITION - 16, SPAWN_Z_POSITION,
    0, 0, 0, 0));
  public static final FallbackPacket REMOVE_VEHICLE = new RemoveEntitiesPacket(VEHICLE_ENTITY_ID);
  public static final FallbackPacket SET_VEHICLE_PASSENGERS = new FallbackPacketSnapshot(
    new SetPassengersPacket(VEHICLE_ENTITY_ID, PLAYER_ENTITY_ID));
  public FallbackPacket[] incorrectCaptcha;

  public FallbackPacket loginSuccess;
  public FallbackPacket welcomeMessage;
  public FallbackPacket enterCodeMessage;
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
  public FallbackPacket unsupportedVersion;
  public FallbackPacket tooManyOnlinePerIP;
  public FallbackPacket[] xpCountdown;
  public FallbackPacket updateTime;

  public static int maxMovementTick, dynamicSpawnYPosition, maxTotalPacketsSent;

  public void prepare() {
    // Preload the packet registry to avoid CPU/RAM issues on 1st connection
    Sonar.get0().getLogger().info("Preloading all registered packets...");
    //noinspection ResultOfMethodCallIgnored
    FallbackPacketRegistry.values();

    // Prepare LoginSuccess packet with capped username to 16 characters
    final UUID uuid = UUID.randomUUID();
    String username = Sonar.get0().getConfig().getGeneralConfig().getString("verification.cached-username");
    if (username.length() > 16) {
      username = username.substring(0, 16);
    }
    loginSuccess = new FallbackPacketSnapshot(new LoginSuccessPacket(uuid, username, true));

    // Prepare JoinGame packet
    joinGame = new FallbackPacketSnapshot(new JoinGamePacket(PLAYER_ENTITY_ID,
      Sonar.get0().getConfig().getVerification().getGamemode().getId(),
      -1, 0, 0,
      RANDOM.nextInt(3), 1, 0, 0,
      new String[]{"minecraft:overworld"}, "minecraft:overworld", "flat",
      DimensionType.OVERWORLD, RANDOM.nextLong() & 1337,
      false, true, false,
      false, false, false, true));

    // Prepare the gravity check
    maxMovementTick = Sonar.get0().getConfig().getVerification().getGravity().getMaxMovementTicks();

    double motionY = 0, fallDistance = 0;

    for (int i = 0; i < maxMovementTick; i++) {
      motionY = (motionY - 0.1) * 0.98f;
      fallDistance += Math.abs(motionY);
    }

    // Set the dynamic block and collide Y position based on the maximum fall distance
    dynamicSpawnYPosition = PLATFORM_Y_POSITION + (int) Math.ceil(fallDistance);
    defaultSpawnPosition = new FallbackPacketSnapshot(new SetDefaultSpawnPositionPacket(
      "minecraft:overworld",
      SPAWN_X_POSITION, IN_AIR_Y_POSITION, SPAWN_Z_POSITION));
    spawnPosition = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
      SPAWN_X_POSITION, IN_AIR_Y_POSITION, SPAWN_Z_POSITION,
      0, 0, FIRST_TELEPORT_ID, 0, false, false, true));
    fallStartPosition = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
      SPAWN_X_POSITION, dynamicSpawnYPosition - IN_AIR_Y_POSITION, SPAWN_Z_POSITION,
      0, -90, SECOND_TELEPORT_ID, 1 << 1, false, false, true));
    fallStartPositionLegacy = new FallbackPacketSnapshot(new SetPlayerPositionRotationPacket(
      SPAWN_X_POSITION, dynamicSpawnYPosition, SPAWN_Z_POSITION,
      0, -90, 0, 0, false, false, true));

    // Prepare collision platform positions
    final int length = BLOCKS_PER_ROW / 2;
    for (int i = 0; i < BLOCKS_PACKETS.length; i++) {
      final BlockUpdate[] blocks = new BlockUpdate[BLOCKS_PER_ROW * BLOCKS_PER_ROW];

      int index = 0;
      for (int x = 0; x < BLOCKS_PER_ROW; x++) {
        for (int z = 0; z < BLOCKS_PER_ROW; z++) {
          final BlockUpdate.BlockPosition position = new BlockUpdate.BlockPosition(
            x + length, PLATFORM_Y_POSITION, z + length, 0, 0);
          blocks[index++] = new BlockUpdate(position, POSSIBLE_BLOCK_TYPES[i]);
        }
      }
      BLOCKS_PACKETS[i] = new FallbackPacketSnapshot(new UpdateSectionBlocksPacket(0, 0, blocks));
    }

    // Prepare disconnect packets during login
    blacklisted = new FallbackPacketSnapshot(new DisconnectPacket(Sonar.get0().getConfig().getVerification().getBlacklisted(), true));
    alreadyVerifying = new FallbackPacketSnapshot(new DisconnectPacket(Sonar.get0().getConfig().getVerification().getAlreadyVerifying(), true));
    alreadyQueued = new FallbackPacketSnapshot(new DisconnectPacket(Sonar.get0().getConfig().getVerification().getAlreadyQueued(), true));
    protocolBlacklisted = new FallbackPacketSnapshot(new DisconnectPacket(Sonar.get0().getConfig().getVerification().getProtocolBlacklisted(), true));
    reconnectedTooFast = new FallbackPacketSnapshot(new DisconnectPacket(Sonar.get0().getConfig().getVerification().getTooFastReconnect(), true));
    unsupportedVersion = new FallbackPacketSnapshot(new DisconnectPacket(Sonar.get0().getConfig().getVerification().getUnsupportedVersion(), true));
    tooManyOnlinePerIP = new FallbackPacketSnapshot(new DisconnectPacket(Sonar.get0().getConfig().getTooManyOnlinePerIp(), true));

    // Prepare transfer packet
    if (Sonar.get0().getConfig().getGeneralConfig().getBoolean("verification.transfer.enabled")) {
      transferToOrigin = new FallbackPacketSnapshot(new TransferPacket(
        Sonar.get0().getConfig().getGeneralConfig().getString("verification.transfer.destination-host"),
        Sonar.get0().getConfig().getGeneralConfig().getInt("verification.transfer.destination-port")));
    } else {
      transferToOrigin = null;
    }

    // Prepare update time packet
    final int timeOfDay = Sonar.get0().getConfig().getVerification().getTimeOfDay();
    if (timeOfDay != 1000) {
      updateTime = new UpdateTimePacket(0L, timeOfDay, false);
    } else {
      updateTime = null;
    }

    // If the welcome message is empty, we don't need to send a message to the player
    final String welcome = Sonar.get0().getConfig().getMessagesConfig().getString("verification.welcome");
    if (welcome.isEmpty()) {
      welcomeMessage = null;
    } else {
      welcomeMessage = new FallbackPacketSnapshot(new SystemChatPacket(new ComponentHolder(
        MiniMessage.miniMessage().deserialize(welcome,
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())))));
    }

    if (Sonar.get0().getConfig().getVerification().getMap().getTiming() != SonarConfiguration.Verification.Timing.NEVER
      || Sonar.get0().getConfig().getVerification().getGravity().isCaptchaOnFail()) {
      // Prepare CAPTCHA messages
      enterCodeMessage = new FallbackPacketSnapshot(new SystemChatPacket(new ComponentHolder(
        MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("verification.captcha.enter"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())))));
      incorrectCaptcha = new FallbackPacket[Sonar.get0().getConfig().getVerification().getMap().getMaxTries()];
      for (int i = 0; i < incorrectCaptcha.length; i++) {
        incorrectCaptcha[i] = new FallbackPacketSnapshot(new SystemChatPacket(new ComponentHolder(
          MiniMessage.miniMessage().deserialize(
            Sonar.get0().getConfig().getMessagesConfig().getString("verification.captcha.incorrect"),
            Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
            Placeholder.unparsed("attempts-left", String.valueOf(i + 1))))));
      }
      // Prepare countdown
      xpCountdown = new FallbackPacket[Sonar.get0().getConfig().getVerification().getMap().getMaxDuration() / 1000];

      for (int i = 0; i < xpCountdown.length; i++) {
        final float bar = (float) i / xpCountdown.length;
        xpCountdown[i] = new FallbackPacketSnapshot(new SetExperiencePacket(bar, i, 0));
      }

      // Update the CAPTCHA generator if necessary
      final CaptchaGenerationStartEvent generationStartEvent = new CaptchaGenerationStartEvent(
        Sonar.get0().getFallback().getCaptchaGenerator());
      Sonar.get0().getEventManager().publish(generationStartEvent);

      if (generationStartEvent.getCaptchaGenerator() == null
        || generationStartEvent.getCaptchaGenerator() instanceof StandardCaptchaGenerator
        || generationStartEvent.getCaptchaGenerator() instanceof LegacyCaptchaGenerator) {
        final File backgroundImage = Sonar.get0().getConfig().getVerification().getMap().getBackgroundImage();
        final boolean useLegacyCaptchaGenerator = Sonar.get0().getConfig().getGeneralConfig()
          .getString("verification.checks.map-captcha.style").equalsIgnoreCase("legacy");
        Sonar.get0().getFallback().setCaptchaGenerator(useLegacyCaptchaGenerator
          ? new LegacyCaptchaGenerator(backgroundImage) : new StandardCaptchaGenerator(backgroundImage));
      } else {
        Sonar.get0().getLogger().info("Custom CAPTCHA generator detected, skipping reinitialization.");
      }

      // Prepare CAPTCHA answers
      CaptchaPreparer.prepare();
    } else {
      // Throw away if not needed
      enterCodeMessage = null;
      incorrectCaptcha = null;
      xpCountdown = null;
      Sonar.get0().getFallback().setCaptchaGenerator(null);
    }

    maxTotalPacketsSent = maxMovementTick + 2
      + (xpCountdown == null ? 0 : xpCountdown.length) * 20 + 5
      + Sonar.get0().getConfig().getVerification().getVehicle().getMinimumPackets() * 4
      + Sonar.get0().getConfig().getVerification().getMap().getMaxTries()
      + Sonar.get0().getConfig().getVerification().getMaxPacketCount();
  }

  public static FallbackPacket[] getRegistryPackets(final @NotNull ProtocolVersion protocolVersion) {
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_5)) {
      return REGISTRY_SYNC_1_21_5;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_4)) {
      return REGISTRY_SYNC_1_21_4;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
      return REGISTRY_SYNC_1_21_2;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21)) {
      return REGISTRY_SYNC_1_21;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20_5)) {
      return REGISTRY_SYNC_1_20_5;
    }
    return REGISTRY_SYNC_1_20;
  }
}
