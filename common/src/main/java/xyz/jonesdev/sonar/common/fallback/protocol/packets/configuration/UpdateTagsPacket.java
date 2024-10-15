package xyz.jonesdev.sonar.common.fallback.protocol.packets.configuration;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import java.util.HashMap;
import java.util.Map;

import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeString;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeVarInt;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class UpdateTagsPacket implements FallbackPacket {
  private Map<String, Map<String, int[]>> tags = new HashMap<>();

  public UpdateTagsPacket(final @NotNull CompoundBinaryTag compoundTag) {
    for (final String type : compoundTag.keySet()) {
      final Map<String, int[]> boundTags = new HashMap<>();
      final CompoundBinaryTag tag = compoundTag.getCompound(type);
      for (final String key : tag.keySet()) {
        boundTags.put(key, tag.getIntArray(key));
      }
      tags.put(type, boundTags);
    }
  }

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    writeVarInt(byteBuf, tags.size());
    for (final Map.Entry<String, Map<String, int[]>> entry : tags.entrySet()) {
      writeString(byteBuf, entry.getKey());
      writeVarInt(byteBuf, entry.getValue().size());
      for (final Map.Entry<String, int[]> boundTags : entry.getValue().entrySet()) {
        writeString(byteBuf, boundTags.getKey());
        writeVarInt(byteBuf, boundTags.getValue().length);
        for (int i : boundTags.getValue()) {
          writeVarInt(byteBuf, i);
        }
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }
}
