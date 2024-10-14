package xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class UpdateTagsPacket implements FallbackPacket {
  private Map<String, Map<String, int[]>> tags = new HashMap<>();

  public UpdateTagsPacket(final @NotNull CompoundBinaryTag compoundTag) {
    for (final @NotNull String type : compoundTag.keySet()) {
      final @NotNull Map<String, int[]> boundTags = new HashMap<>();
      final @NotNull CompoundBinaryTag tag = compoundTag.getCompound(type);
      for (final String key : tag.keySet()) {
        boundTags.put(key, tag.getIntArray(key));
      }
      tags.put(type, boundTags);
    }
  }

  @Override
  public void encode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    ProtocolUtil.writeVarInt(byteBuf, tags.size());
    for (final Map.Entry<String, Map<String, int[]>> entry : tags.entrySet()) {
      ProtocolUtil.writeString(byteBuf, entry.getKey());
      ProtocolUtil.writeVarInt(byteBuf, entry.getValue().size());
      for (final Map.Entry<String, int[]> boundTags : entry.getValue().entrySet()) {
        ProtocolUtil.writeString(byteBuf, boundTags.getKey());
        ProtocolUtil.writeVarInt(byteBuf, boundTags.getValue().length);
        for (int i : boundTags.getValue()) {
          ProtocolUtil.writeVarInt(byteBuf, i);
        }
      }
    }
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }
}
