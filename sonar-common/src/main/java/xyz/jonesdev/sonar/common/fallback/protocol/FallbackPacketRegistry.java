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

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration.FinishConfigurationPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration.RegistryDataPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginAcknowledgedPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.login.LoginSuccessPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;

import java.util.*;
import java.util.function.Supplier;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.*;

// Mostly taken from
// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/StateRegistry.java
public enum FallbackPacketRegistry {
  LOGIN {
    {
      clientbound.register(DisconnectPacket.class, DisconnectPacket::new,
        map(0x00, MINECRAFT_1_7_2, true));
      clientbound.register(LoginSuccessPacket.class, LoginSuccessPacket::new,
        map(0x02, MINECRAFT_1_7_2, true));

      serverbound.register(LoginAcknowledgedPacket.class, LoginAcknowledgedPacket::new,
        map(0x03, MINECRAFT_1_20_2, false));
    }
  },
  CONFIG {
    {
      clientbound.register(DisconnectPacket.class, DisconnectPacket::new,
        map(0x01, MINECRAFT_1_20_2, true),
        map(0x02, MINECRAFT_1_20_5, true));
      clientbound.register(FinishConfigurationPacket.class, FinishConfigurationPacket::new,
        map(0x02, MINECRAFT_1_20_2, false),
        map(0x03, MINECRAFT_1_20_5, false));
      clientbound.register(KeepAlivePacket.class, KeepAlivePacket::new,
        map(0x03, MINECRAFT_1_20_2, false),
        map(0x04, MINECRAFT_1_20_5, false));
      clientbound.register(RegistryDataPacket.class, RegistryDataPacket::new,
        map(0x05, MINECRAFT_1_20_2, true),
        map(0x07, MINECRAFT_1_20_5, true));

      serverbound.register(ClientInformationPacket.class, ClientInformationPacket::new,
        map(0x00, MINECRAFT_1_20_2, false));
      serverbound.register(PluginMessagePacket.class, PluginMessagePacket::new,
        map(0x01, MINECRAFT_1_20_2, false),
        map(0x02, MINECRAFT_1_20_5, false));
      serverbound.register(FinishConfigurationPacket.class, FinishConfigurationPacket::new,
        map(0x02, MINECRAFT_1_20_2, false),
        map(0x03, MINECRAFT_1_20_5, false));
      serverbound.register(KeepAlivePacket.class, KeepAlivePacket::new,
        map(0x03, MINECRAFT_1_20_2, false),
        map(0x04, MINECRAFT_1_20_5, false));
    }
  },
  GAME {
    {
      clientbound.register(JoinGamePacket.class, JoinGamePacket::new,
        map(0x01, MINECRAFT_1_7_2, true),
        map(0x23, MINECRAFT_1_9, true),
        map(0x25, MINECRAFT_1_13, true),
        map(0x25, MINECRAFT_1_14, true),
        map(0x26, MINECRAFT_1_15, true),
        map(0x25, MINECRAFT_1_16, true),
        map(0x24, MINECRAFT_1_16_2, true),
        map(0x26, MINECRAFT_1_17, true),
        map(0x23, MINECRAFT_1_19, true),
        map(0x25, MINECRAFT_1_19_1, true),
        map(0x24, MINECRAFT_1_19_3, true),
        map(0x28, MINECRAFT_1_19_4, true),
        map(0x29, MINECRAFT_1_20_2, true),
        map(0x2B, MINECRAFT_1_20_5, true));
      clientbound.register(KeepAlivePacket.class, KeepAlivePacket::new,
        map(0x00, MINECRAFT_1_7_2, false),
        map(0x1F, MINECRAFT_1_9, false),
        map(0x21, MINECRAFT_1_13, false),
        map(0x20, MINECRAFT_1_14, false),
        map(0x21, MINECRAFT_1_15, false),
        map(0x20, MINECRAFT_1_16, false),
        map(0x1F, MINECRAFT_1_16_2, false),
        map(0x21, MINECRAFT_1_17, false),
        map(0x1E, MINECRAFT_1_19, false),
        map(0x20, MINECRAFT_1_19_1, false),
        map(0x1F, MINECRAFT_1_19_3, false),
        map(0x23, MINECRAFT_1_19_4, false),
        map(0x24, MINECRAFT_1_20_2, false),
        map(0x26, MINECRAFT_1_20_5, false));
      clientbound.register(DisconnectPacket.class, DisconnectPacket::new,
        map(0x40, MINECRAFT_1_7_2, true),
        map(0x1A, MINECRAFT_1_9, true),
        map(0x1B, MINECRAFT_1_13, true),
        map(0x1A, MINECRAFT_1_14, true),
        map(0x1B, MINECRAFT_1_15, true),
        map(0x1A, MINECRAFT_1_16, true),
        map(0x19, MINECRAFT_1_16_2, true),
        map(0x1A, MINECRAFT_1_17, true),
        map(0x17, MINECRAFT_1_19, true),
        map(0x19, MINECRAFT_1_19_1, true),
        map(0x17, MINECRAFT_1_19_3, true),
        map(0x1A, MINECRAFT_1_19_4, true),
        map(0x1B, MINECRAFT_1_20_2, true),
        map(0x1D, MINECRAFT_1_20_5, true));
      clientbound.register(SetPlayerPositionRotation.class, SetPlayerPositionRotation::new,
        map(0x08, MINECRAFT_1_7_2, true),
        map(0x2E, MINECRAFT_1_9, true),
        map(0x2F, MINECRAFT_1_12_1, true),
        map(0x32, MINECRAFT_1_13, true),
        map(0x35, MINECRAFT_1_14, true),
        map(0x36, MINECRAFT_1_15, true),
        map(0x35, MINECRAFT_1_16, true),
        map(0x34, MINECRAFT_1_16_2, true),
        map(0x38, MINECRAFT_1_17, true),
        map(0x36, MINECRAFT_1_19, true),
        map(0x39, MINECRAFT_1_19_1, true),
        map(0x38, MINECRAFT_1_19_3, true),
        map(0x3C, MINECRAFT_1_19_4, true),
        map(0x3E, MINECRAFT_1_20_2, true),
        map(0x40, MINECRAFT_1_20_5, true));
      clientbound.register(PlayerAbilitiesPacket.class, PlayerAbilitiesPacket::new,
        map(0x39, MINECRAFT_1_7_2, true),
        map(0x2B, MINECRAFT_1_9, true),
        map(0x2C, MINECRAFT_1_12_1, true),
        map(0x2E, MINECRAFT_1_13, true),
        map(0x31, MINECRAFT_1_14, true),
        map(0x32, MINECRAFT_1_15, true),
        map(0x31, MINECRAFT_1_16, true),
        map(0x30, MINECRAFT_1_16_2, true),
        map(0x32, MINECRAFT_1_17, true),
        map(0x2f, MINECRAFT_1_19, true),
        map(0x31, MINECRAFT_1_19_1, true),
        map(0x30, MINECRAFT_1_19_3, true),
        map(0x34, MINECRAFT_1_19_4, true),
        map(0x36, MINECRAFT_1_20_2, true),
        map(0x38, MINECRAFT_1_20_5, true));
      clientbound.register(ChunkDataPacket.class, ChunkDataPacket::new,
        map(0x21, MINECRAFT_1_7_2, true),
        map(0x20, MINECRAFT_1_9, true),
        map(0x22, MINECRAFT_1_13, true),
        map(0x21, MINECRAFT_1_14, true),
        map(0x22, MINECRAFT_1_15, true),
        map(0x21, MINECRAFT_1_16, true),
        map(0x20, MINECRAFT_1_16_2, true),
        map(0x22, MINECRAFT_1_17, true),
        map(0x1F, MINECRAFT_1_19, true),
        map(0x21, MINECRAFT_1_19_1, true),
        map(0x20, MINECRAFT_1_19_3, true),
        map(0x24, MINECRAFT_1_19_4, true),
        map(0x25, MINECRAFT_1_20_2, true),
        map(0x27, MINECRAFT_1_20_5, true));
      clientbound.register(RemoveEntitiesPacket.class, RemoveEntitiesPacket::new,
        map(0x13, MINECRAFT_1_7_2, true),
        map(0x30, MINECRAFT_1_9, true),
        map(0x31, MINECRAFT_1_12, true),
        map(0x32, MINECRAFT_1_12_2, true),
        map(0x35, MINECRAFT_1_13, true),
        map(0x37, MINECRAFT_1_14, true),
        map(0x38, MINECRAFT_1_15, true),
        map(0x37, MINECRAFT_1_16, true),
        map(0x36, MINECRAFT_1_16_2, true),
        map(0x3A, MINECRAFT_1_17, true),
        map(0x38, MINECRAFT_1_19, true),
        map(0x3B, MINECRAFT_1_19_1, true),
        map(0x3A, MINECRAFT_1_19_3, true),
        map(0x3E, MINECRAFT_1_19_4, true),
        map(0x3E, MINECRAFT_1_19_4, true),
        map(0x40, MINECRAFT_1_20_2, true),
        map(0x42, MINECRAFT_1_20_5, true));
      clientbound.register(SetPassengersPacket.class, SetPassengersPacket::new,
        map(0x1B, MINECRAFT_1_7_2, true),
        map(0x40, MINECRAFT_1_9, true),
        map(0x42, MINECRAFT_1_12, true),
        map(0x43, MINECRAFT_1_12_1, true),
        map(0x46, MINECRAFT_1_13, true),
        map(0x4A, MINECRAFT_1_14, true),
        map(0x4B, MINECRAFT_1_15, true),
        map(0x54, MINECRAFT_1_17, true),
        map(0x57, MINECRAFT_1_19_1, true),
        map(0x55, MINECRAFT_1_19_3, true),
        map(0x59, MINECRAFT_1_19_4, true),
        map(0x5B, MINECRAFT_1_20_2, true),
        map(0x5D, MINECRAFT_1_20_3, true),
        map(0x5F, MINECRAFT_1_20_5, true));
      clientbound.register(SpawnEntityPacket.class, SpawnEntityPacket::new,
        map(0x0E, MINECRAFT_1_7_2, true),
        map(0x00, MINECRAFT_1_9, true),
        map(0x01, MINECRAFT_1_19_4, true));
      clientbound.register(PlayerInfoPacket.class, PlayerInfoPacket::new,
        map(0x38, MINECRAFT_1_7_2, true),
        map(0x2D, MINECRAFT_1_9, true),
        map(0x2E, MINECRAFT_1_12_1, true),
        map(0x30, MINECRAFT_1_13, true),
        map(0x33, MINECRAFT_1_14, true),
        map(0x34, MINECRAFT_1_15, true),
        map(0x33, MINECRAFT_1_16, true),
        map(0x32, MINECRAFT_1_16_2, true),
        map(0x36, MINECRAFT_1_17, true),
        map(0x34, MINECRAFT_1_19, true),
        map(0x37, MINECRAFT_1_19_1, true),
        map(0x36, MINECRAFT_1_19_3, true),
        map(0x3A, MINECRAFT_1_19_4, true),
        map(0x3C, MINECRAFT_1_20_2, true),
        map(0x3E, MINECRAFT_1_20_5, true));
      clientbound.register(UpdateSectionBlocksPacket.class, UpdateSectionBlocksPacket::new,
        map(0x22, MINECRAFT_1_7_2, true),
        map(0x10, MINECRAFT_1_9, true),
        map(0x0F, MINECRAFT_1_13, true),
        map(0x10, MINECRAFT_1_15, true),
        map(0x0F, MINECRAFT_1_16, true),
        map(0x3B, MINECRAFT_1_16_2, true),
        map(0x3F, MINECRAFT_1_17, true),
        map(0x3D, MINECRAFT_1_19, true),
        map(0x40, MINECRAFT_1_19_1, true),
        map(0x3F, MINECRAFT_1_19_3, true),
        map(0x43, MINECRAFT_1_19_4, true),
        map(0x45, MINECRAFT_1_20_2, true),
        map(0x47, MINECRAFT_1_20_3, true),
        map(0x49, MINECRAFT_1_20_5, true));
      clientbound.register(TransactionPacket.class, TransactionPacket::new,
        map(0x32, MINECRAFT_1_7_2, true),
        map(0x11, MINECRAFT_1_9, true),
        map(0x12, MINECRAFT_1_13, true),
        map(0x13, MINECRAFT_1_15, true),
        map(0x12, MINECRAFT_1_16, true),
        map(0x11, MINECRAFT_1_16_2, true),
        map(0x30, MINECRAFT_1_17, true),
        map(0x2D, MINECRAFT_1_19, true),
        map(0x2F, MINECRAFT_1_19_1, true),
        map(0x2E, MINECRAFT_1_19_3, true),
        map(0x32, MINECRAFT_1_19_4, true),
        map(0x33, MINECRAFT_1_20_2, true),
        map(0x35, MINECRAFT_1_20_5, true));
      clientbound.register(SetDefaultSpawnPositionPacket.class, SetDefaultSpawnPositionPacket::new,
        map(0x05, MINECRAFT_1_7_2, true),
        map(0x43, MINECRAFT_1_9, true),
        map(0x45, MINECRAFT_1_12, true),
        map(0x46, MINECRAFT_1_12_1, true),
        map(0x49, MINECRAFT_1_13, true),
        map(0x4D, MINECRAFT_1_14, true),
        map(0x4E, MINECRAFT_1_15, true),
        map(0x42, MINECRAFT_1_16, true),
        map(0x4B, MINECRAFT_1_17, true),
        map(0x4A, MINECRAFT_1_19, true),
        map(0x4D, MINECRAFT_1_19_1, true),
        map(0x4C, MINECRAFT_1_19_3, true),
        map(0x50, MINECRAFT_1_19_4, true),
        map(0x52, MINECRAFT_1_20_2, true),
        map(0x54, MINECRAFT_1_20_3, true),
        map(0x56, MINECRAFT_1_20_5, true));
      clientbound.register(MapDataPacket.class, MapDataPacket::new,
        map(0x34, MINECRAFT_1_7_2, true),
        map(0x24, MINECRAFT_1_9, true),
        map(0x26, MINECRAFT_1_13, true),
        map(0x27, MINECRAFT_1_15, true),
        map(0x26, MINECRAFT_1_16, true),
        map(0x25, MINECRAFT_1_16_2, true),
        map(0x27, MINECRAFT_1_17, true),
        map(0x24, MINECRAFT_1_19, true),
        map(0x26, MINECRAFT_1_19_1, true),
        map(0x25, MINECRAFT_1_19_3, true),
        map(0x29, MINECRAFT_1_19_4, true),
        map(0x2A, MINECRAFT_1_20_2, true),
        map(0x2C, MINECRAFT_1_20_5, true));
      clientbound.register(SetContainerSlotPacket.class, SetContainerSlotPacket::new,
        map(0x2F, MINECRAFT_1_7_2, true),
        map(0x16, MINECRAFT_1_9, true),
        map(0x17, MINECRAFT_1_13, true),
        map(0x16, MINECRAFT_1_14, true),
        map(0x17, MINECRAFT_1_15, true),
        map(0x16, MINECRAFT_1_16, true),
        map(0x15, MINECRAFT_1_16_2, true),
        map(0x16, MINECRAFT_1_17, true),
        map(0x13, MINECRAFT_1_19, true),
        map(0x12, MINECRAFT_1_19_3, true),
        map(0x14, MINECRAFT_1_19_4, true),
        map(0x15, MINECRAFT_1_20_2, true));
      clientbound.register(SetExperiencePacket.class, SetExperiencePacket::new,
        map(0x1F, MINECRAFT_1_7_2, true),
        map(0x3D, MINECRAFT_1_9, true),
        map(0x3F, MINECRAFT_1_12, true),
        map(0x40, MINECRAFT_1_12_1, true),
        map(0x43, MINECRAFT_1_13, true),
        map(0x47, MINECRAFT_1_14, true),
        map(0x48, MINECRAFT_1_15, true),
        map(0x51, MINECRAFT_1_17, true),
        map(0x54, MINECRAFT_1_19_1, true),
        map(0x52, MINECRAFT_1_19_3, true),
        map(0x56, MINECRAFT_1_19_4, true),
        map(0x58, MINECRAFT_1_20_2, true),
        map(0x5A, MINECRAFT_1_20_3, true),
        map(0x5C, MINECRAFT_1_20_5, true));
      clientbound.register(SystemChatPacket.class, SystemChatPacket::new,
        map(0x02, MINECRAFT_1_7_2, false),
        map(0x0F, MINECRAFT_1_9, false),
        map(0x0E, MINECRAFT_1_13, false),
        map(0x0F, MINECRAFT_1_15, false),
        map(0x0E, MINECRAFT_1_16, false),
        map(0x0F, MINECRAFT_1_17, false),
        map(0x5F, MINECRAFT_1_19, false),
        map(0x62, MINECRAFT_1_19_1, false),
        map(0x60, MINECRAFT_1_19_3, false),
        map(0x64, MINECRAFT_1_19_4, false),
        map(0x67, MINECRAFT_1_20_2, false),
        map(0x69, MINECRAFT_1_20_3, false),
        map(0x6C, MINECRAFT_1_20_5, false));
      clientbound.register(GameEventPacket.class, GameEventPacket::new,
        map(0x20, MINECRAFT_1_20_3, true),
        map(0x22, MINECRAFT_1_20_5, true));
      clientbound.register(TransferPacket.class, TransferPacket::new,
        map(0x73, MINECRAFT_1_20_5, true));
      clientbound.register(SetHeldItemPacket.class, SetHeldItemPacket::new,
        map(0x09, MINECRAFT_1_7_2, true),
        map(0x37, MINECRAFT_1_9, true),
        map(0x39, MINECRAFT_1_12, true),
        map(0x3A, MINECRAFT_1_12_1, true),
        map(0x3D, MINECRAFT_1_13, true),
        map(0x3F, MINECRAFT_1_14, true),
        map(0x40, MINECRAFT_1_15, true),
        map(0x3F, MINECRAFT_1_16, true),
        map(0x48, MINECRAFT_1_17, true),
        map(0x47, MINECRAFT_1_19, true),
        map(0x4A, MINECRAFT_1_19_1, true),
        map(0x49, MINECRAFT_1_19_3, true),
        map(0x4D, MINECRAFT_1_19_4, true),
        map(0x4F, MINECRAFT_1_20_2, true),
        map(0x51, MINECRAFT_1_20_3, true),
        map(0x53, MINECRAFT_1_20_5, true));

      serverbound.register(SetHeldItemPacket.class, SetHeldItemPacket::new,
        map(0x09, MINECRAFT_1_7_2, false),
        map(0x17, MINECRAFT_1_9, false),
        map(0x1A, MINECRAFT_1_12, false),
        map(0x21, MINECRAFT_1_13, false),
        map(0x23, MINECRAFT_1_14, false),
        map(0x24, MINECRAFT_1_16, false),
        map(0x25, MINECRAFT_1_16_2, false),
        map(0x27, MINECRAFT_1_19, false),
        map(0x28, MINECRAFT_1_19_1, false),
        map(0x2B, MINECRAFT_1_20_2, false),
        map(0x2C, MINECRAFT_1_20_3, false),
        map(0x2F, MINECRAFT_1_20_5, false));
      serverbound.register(PaddleBoatPacket.class, PaddleBoatPacket::new,
        map(0x11, MINECRAFT_1_9, false),
        map(0x12, MINECRAFT_1_12, false),
        map(0x11, MINECRAFT_1_12_1, false),
        map(0x14, MINECRAFT_1_13, false),
        map(0x16, MINECRAFT_1_14, false),
        map(0x17, MINECRAFT_1_16, false),
        map(0x16, MINECRAFT_1_17, false),
        map(0x18, MINECRAFT_1_19, false),
        map(0x19, MINECRAFT_1_19_1, false),
        map(0x18, MINECRAFT_1_19_3, false),
        map(0x19, MINECRAFT_1_19_4, false),
        map(0x1B, MINECRAFT_1_20_2, false),
        map(0x1C, MINECRAFT_1_20_3, false),
        map(0x1F, MINECRAFT_1_20_5, false));
      serverbound.register(PlayerInputPacket.class, PlayerInputPacket::new,
        map(0x0C, MINECRAFT_1_7_2, false),
        map(0x15, MINECRAFT_1_9, false),
        map(0x16, MINECRAFT_1_12, false),
        map(0x1A, MINECRAFT_1_13, false),
        map(0x1C, MINECRAFT_1_14, false),
        map(0x1D, MINECRAFT_1_16, false),
        map(0x1C, MINECRAFT_1_17, false),
        map(0x1E, MINECRAFT_1_19, false),
        map(0x1F, MINECRAFT_1_19_1, false),
        map(0x1E, MINECRAFT_1_19_3, false),
        map(0x1F, MINECRAFT_1_19_4, false),
        map(0x22, MINECRAFT_1_20_2, false),
        map(0x23, MINECRAFT_1_20_3, false),
        map(0x26, MINECRAFT_1_20_5, false));
      serverbound.register(SystemChatPacket.class, SystemChatPacket::new,
        map(0x01, MINECRAFT_1_7_2, false),
        map(0x02, MINECRAFT_1_9, false),
        map(0x03, MINECRAFT_1_12, false),
        map(0x02, MINECRAFT_1_12_1, false),
        map(0x03, MINECRAFT_1_14, false),
        map(0x04, MINECRAFT_1_19, false),
        map(0x05, MINECRAFT_1_19_1, false),
        map(0x05, MINECRAFT_1_19_3, false),
        map(0x06, MINECRAFT_1_20_5, false));
      serverbound.register(KeepAlivePacket.class, KeepAlivePacket::new,
        map(0x00, MINECRAFT_1_7_2, false),
        map(0x0B, MINECRAFT_1_9, false),
        map(0x0C, MINECRAFT_1_12, false),
        map(0x0B, MINECRAFT_1_12_1, false),
        map(0x0E, MINECRAFT_1_13, false),
        map(0x0F, MINECRAFT_1_14, false),
        map(0x10, MINECRAFT_1_16, false),
        map(0x0F, MINECRAFT_1_17, false),
        map(0x11, MINECRAFT_1_19, false),
        map(0x12, MINECRAFT_1_19_1, false),
        map(0x11, MINECRAFT_1_19_3, false),
        map(0x12, MINECRAFT_1_19_4, false),
        map(0x14, MINECRAFT_1_20_2, false),
        map(0x15, MINECRAFT_1_20_3, false),
        map(0x18, MINECRAFT_1_20_5, false));
      serverbound.register(ClientInformationPacket.class, ClientInformationPacket::new,
        map(0x15, MINECRAFT_1_7_2, false),
        map(0x04, MINECRAFT_1_9, false),
        map(0x05, MINECRAFT_1_12, false),
        map(0x04, MINECRAFT_1_12_1, false),
        map(0x05, MINECRAFT_1_14, false),
        map(0x07, MINECRAFT_1_19, false),
        map(0x08, MINECRAFT_1_19_1, false),
        map(0x07, MINECRAFT_1_19_3, false),
        map(0x08, MINECRAFT_1_19_4, false),
        map(0x09, MINECRAFT_1_20_2, false),
        map(0x0A, MINECRAFT_1_20_5, false));
      serverbound.register(PluginMessagePacket.class, PluginMessagePacket::new,
        map(0x17, MINECRAFT_1_7_2, false),
        map(0x09, MINECRAFT_1_9, false),
        map(0x0A, MINECRAFT_1_12, false),
        map(0x09, MINECRAFT_1_12_1, false),
        map(0x0A, MINECRAFT_1_13, false),
        map(0x0B, MINECRAFT_1_14, false),
        map(0x0A, MINECRAFT_1_17, false),
        map(0x0C, MINECRAFT_1_19, false),
        map(0x0D, MINECRAFT_1_19_1, false),
        map(0x0C, MINECRAFT_1_19_3, false),
        map(0x0D, MINECRAFT_1_19_4, false),
        map(0x0F, MINECRAFT_1_20_2, false),
        map(0x12, MINECRAFT_1_21, false));
      serverbound.register(SetPlayerPositionRotation.class, SetPlayerPositionRotation::new,
        map(0x06, MINECRAFT_1_7_2, false),
        map(0x0D, MINECRAFT_1_9, false),
        map(0x0F, MINECRAFT_1_12, false),
        map(0x0E, MINECRAFT_1_12_1, false),
        map(0x11, MINECRAFT_1_13, false),
        map(0x12, MINECRAFT_1_14, false),
        map(0x13, MINECRAFT_1_16, false),
        map(0x12, MINECRAFT_1_17, false),
        map(0x14, MINECRAFT_1_19, false),
        map(0x15, MINECRAFT_1_19_1, false),
        map(0x14, MINECRAFT_1_19_3, false),
        map(0x15, MINECRAFT_1_19_4, false),
        map(0x17, MINECRAFT_1_20_2, false),
        map(0x18, MINECRAFT_1_20_3, false),
        map(0x1B, MINECRAFT_1_20_5, false));
      serverbound.register(ConfirmTeleportationPacket.class, ConfirmTeleportationPacket::new,
        map(0x00, MINECRAFT_1_9, false));
      serverbound.register(SetPlayerPositionPacket.class, SetPlayerPositionPacket::new,
        map(0x04, MINECRAFT_1_7_2, false),
        map(0x0C, MINECRAFT_1_9, false),
        map(0x0E, MINECRAFT_1_12, false),
        map(0x0D, MINECRAFT_1_12_1, false),
        map(0x10, MINECRAFT_1_13, false),
        map(0x11, MINECRAFT_1_14, false),
        map(0x12, MINECRAFT_1_16, false),
        map(0x11, MINECRAFT_1_17, false),
        map(0x13, MINECRAFT_1_19, false),
        map(0x14, MINECRAFT_1_19_1, false),
        map(0x13, MINECRAFT_1_19_3, false),
        map(0x14, MINECRAFT_1_19_4, false),
        map(0x16, MINECRAFT_1_20_2, false),
        map(0x17, MINECRAFT_1_20_3, false),
        map(0x1A, MINECRAFT_1_20_5, false));
      serverbound.register(SetPlayerOnGround.class, SetPlayerOnGround::new,
        map(0x03, MINECRAFT_1_7_2, false),
        map(0x0F, MINECRAFT_1_9, false),
        map(0x0D, MINECRAFT_1_12, false),
        map(0x0C, MINECRAFT_1_12_1, false),
        map(0x0F, MINECRAFT_1_13, false),
        map(0x14, MINECRAFT_1_14, false),
        map(0x15, MINECRAFT_1_16, false),
        map(0x14, MINECRAFT_1_17, false),
        map(0x16, MINECRAFT_1_19, false),
        map(0x17, MINECRAFT_1_19_1, false),
        map(0x16, MINECRAFT_1_19_3, false),
        map(0x17, MINECRAFT_1_19_4, false),
        map(0x19, MINECRAFT_1_20_2, false),
        map(0x1A, MINECRAFT_1_20_3, false),
        map(0x1D, MINECRAFT_1_20_5, false));
      serverbound.register(TransactionPacket.class, TransactionPacket::new,
        map(0x0F, MINECRAFT_1_7_2, false),
        map(0x05, MINECRAFT_1_9, false),
        map(0x06, MINECRAFT_1_12, false),
        map(0x05, MINECRAFT_1_12_1, false),
        map(0x06, MINECRAFT_1_13, false),
        map(0x07, MINECRAFT_1_14, false),
        map(0x1D, MINECRAFT_1_17, false),
        map(0x1F, MINECRAFT_1_19, false),
        map(0x20, MINECRAFT_1_19_1, false),
        map(0x1F, MINECRAFT_1_19_3, false),
        map(0x20, MINECRAFT_1_19_4, false),
        map(0x23, MINECRAFT_1_20_2, false),
        map(0x24, MINECRAFT_1_20_3, false),
        map(0x27, MINECRAFT_1_20_5, false));
    }
  };

  protected final PacketRegistry clientbound = new PacketRegistry();
  protected final PacketRegistry serverbound = new PacketRegistry();

  public enum Direction {
    SERVERBOUND,
    CLIENTBOUND
  }

  public FallbackPacketRegistry.ProtocolRegistry getProtocolRegistry(final Direction direction,
                                                                     final ProtocolVersion version) {
    return (direction == Direction.SERVERBOUND ? serverbound : clientbound).getProtocolRegistry(version);
  }

  public static class PacketRegistry {
    private final Map<ProtocolVersion, ProtocolRegistry> versions;

    PacketRegistry() {
      final Map<ProtocolVersion, ProtocolRegistry> mutableVersions = new EnumMap<>(ProtocolVersion.class);
      for (final ProtocolVersion version : ProtocolVersion.values()) {
        if (!version.isUnknown()) {
          mutableVersions.put(version, new ProtocolRegistry(version));
        }
      }
      this.versions = Collections.unmodifiableMap(mutableVersions);
    }

    ProtocolRegistry getProtocolRegistry(final ProtocolVersion version) {
      final ProtocolRegistry registry = versions.get(version);
      if (registry == null) {
        throw new IllegalArgumentException("Could not find data for protocol version " + version);
      }
      return registry;
    }

    <P extends FallbackPacket> void register(final Class<P> clazz,
                                             final Supplier<P> packetSupplier,
                                             final PacketMapping @NotNull ... mappings) {
      if (mappings.length == 0) {
        throw new IllegalArgumentException("At least one mapping must be provided.");
      }

      for (int i = 0; i < mappings.length; i++) {
        final PacketMapping current = mappings[i];
        final PacketMapping next = (i + 1 < mappings.length) ? mappings[i + 1] : current;

        final ProtocolVersion from = current.protocolVersion;
        final ProtocolVersion to = getProtocolVersion(current, next);

        for (final ProtocolVersion protocol : EnumSet.range(from, to)) {
          if (protocol == to && next != current) {
            break;
          }

          final ProtocolRegistry registry = versions.get(protocol);
          if (registry == null) {
            throw new IllegalArgumentException("Unknown protocol version " + from);
          }

          if (registry.packetIdToSupplier.containsKey(current.id)) {
            throw new IllegalArgumentException("Can not register class " + clazz.getSimpleName()
              + " with id " + current.id + " for " + registry.protocolVersion
              + " because another packet is already registered");
          }

          if (registry.packetClassToId.containsKey(clazz)) {
            throw new IllegalArgumentException(clazz.getSimpleName()
              + " is already registered for version " + registry.protocolVersion);
          }

          if (!current.encodeOnly) {
            registry.packetIdToSupplier.put(current.id, packetSupplier);
          }
          registry.packetClassToId.put(clazz, current.id);
        }
      }
    }

    @NotNull
    private static ProtocolVersion getProtocolVersion(final @NotNull PacketMapping current,
                                                      final @NotNull PacketMapping next) {
      return current == next ? LATEST_VERSION : next.protocolVersion;
    }
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ProtocolRegistry {
    private final ProtocolVersion protocolVersion;
    private final IntObjectMap<Supplier<? extends FallbackPacket>> packetIdToSupplier =
      new IntObjectHashMap<>(16, 0.5f);
    private final Map<Class<? extends FallbackPacket>, Integer> packetClassToId =
      new HashMap<>(16, 0.5f);

    public FallbackPacket createPacket(final int id) {
      final Supplier<? extends FallbackPacket> supplier = packetIdToSupplier.get(id);

      if (supplier == null) {
        return null;
      }
      return supplier.get();
    }

    public int getPacketId(final @NotNull FallbackPacket packet) {
      final int id = packetClassToId.getOrDefault(packet.getClass(), Integer.MIN_VALUE);

      if (id == Integer.MIN_VALUE) {
        throw new IllegalArgumentException("Could not find packet");
      }
      return id;
    }
  }

  @Data
  public static final class PacketMapping {
    private final int id;
    private final ProtocolVersion protocolVersion;
    private final boolean encodeOnly;

    PacketMapping(final int id,
                  final ProtocolVersion protocolVersion,
                  final boolean encodeOnly) {
      this.id = id;
      this.protocolVersion = protocolVersion;
      this.encodeOnly = encodeOnly;
    }
  }

  private static @NotNull PacketMapping map(final int id,
                                            final ProtocolVersion protocolVersion,
                                            final boolean encodeOnly) {
    return new PacketMapping(id, protocolVersion, encodeOnly);
  }
}
