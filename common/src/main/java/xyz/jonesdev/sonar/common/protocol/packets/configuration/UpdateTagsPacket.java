/*
 * Copyright (C) 2026 Jones Development
 *
 * All rights reserved.
 * This software is proprietary and cannot be copied, modified, or distributed without explicit permission.
 */

package xyz.jonesdev.sonar.common.protocol.packets.configuration;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.SonarPacket;
import xyz.jonesdev.sonar.common.protocol.SonarPacketSnapshot;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// If you want a decent solution, use Sonar 3.0 - I don't have the time to implement tags.
//
// Taken from
// https://github.com/BoomEaro/NanoLimbo/blob/feature/1.21.2/src/main/java/ua/nanit/limbo/protocol/packets/configuration/PacketUpdateTags.java
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public final class UpdateTagsPacket implements SonarPacket {
  private Map<String, Map<String, List<Integer>>> tags;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    ProtocolUtil.writeVarInt(byteBuf, this.tags.size());
    for (Map.Entry<String, Map<String, List<Integer>>> entry : this.tags.entrySet()) {
      ProtocolUtil.writeString(byteBuf, entry.getKey());

      Map<String, List<Integer>> subTags = entry.getValue();
      ProtocolUtil.writeVarInt(byteBuf, subTags.size());

      for (Map.Entry<String, List<Integer>> subEntry : subTags.entrySet()) {
        ProtocolUtil.writeString(byteBuf, subEntry.getKey());

        List<Integer> ids = subEntry.getValue();
        ProtocolUtil.writeVarInt(byteBuf, ids.size());
        for (int id : ids) {
          ProtocolUtil.writeVarInt(byteBuf, id);
        }
      }
    }
  }

  public static @NotNull SonarPacket of(@NotNull CompoundBinaryTag tags) {
    Map<String, Map<String, List<Integer>>> tagsMap = new HashMap<>();

    for (Map.Entry<String, ? extends BinaryTag> namedTag : tags) {
      Map<String, List<Integer>> subTagsMap = new HashMap<>();
      CompoundBinaryTag subTag = (CompoundBinaryTag) namedTag.getValue();

      for (Map.Entry<String, ? extends BinaryTag> subNamedTag : subTag) {
        List<Integer> idsList = new ArrayList<>();
        ListBinaryTag ids = (ListBinaryTag) subNamedTag.getValue();
        for (BinaryTag id : ids) {
          idsList.add(((IntBinaryTag) id).value());
        }
        subTagsMap.put(subNamedTag.getKey(), idsList);
      }
      tagsMap.put(namedTag.getKey(), subTagsMap);
    }
    return new SonarPacketSnapshot(new UpdateTagsPacket(tagsMap));
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
