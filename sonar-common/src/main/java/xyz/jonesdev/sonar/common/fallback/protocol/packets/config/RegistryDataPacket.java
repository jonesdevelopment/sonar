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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.config;

import io.netty.buffer.ByteBuf;
import lombok.*;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.dimension.DimensionRegistry;

import java.util.ArrayList;
import java.util.List;

import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.writeNamelessCompoundTag;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.writeString;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarInt;

@Getter
@ToString
@RequiredArgsConstructor
public final class RegistryDataPacket implements FallbackPacket {
  private final CompoundBinaryTag tag;
  private final List<RegistryDataPacket.Bundle> bundles;
  private final String type;

  public RegistryDataPacket() {
    this(null, null);
  }

  public RegistryDataPacket(final String type, final List<Bundle> bundles) {
    this.tag = DimensionRegistry.CODEC_1_20;
    this.type = type;
    this.bundles = bundles;
  }

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_5) < 0) {
      writeNamelessCompoundTag(byteBuf, tag);
    } else {
      writeString(byteBuf, type);
      writeVarInt(byteBuf, bundles.size());

      for (final RegistryDataPacket.Bundle bundle : bundles) {
        writeString(byteBuf, bundle.getName());
        // Write the bundle tag
        final CompoundBinaryTag tag = bundle.getTag();
        if (tag != null) {
          byteBuf.writeBoolean(true);
          writeNamelessCompoundTag(byteBuf, tag);
        } else {
          byteBuf.writeBoolean(false);
        }
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public static @NotNull List<FallbackPacket> of(final @NotNull CompoundBinaryTag rootTag) {
    final List<FallbackPacket> packets = new ArrayList<>();
    for (final String type : rootTag.keySet()) {
      final ArrayList<RegistryDataPacket.Bundle> bundles = new ArrayList<>();
      for (final BinaryTag binaryTag : rootTag.getCompound(type).getList("value")) {
        final CompoundBinaryTag tag = (CompoundBinaryTag) binaryTag;
        bundles.add(new Bundle(tag.getString("name"), tag.getCompound("element")));
      }
      packets.add(new RegistryDataPacket(type, bundles));
    }
    return packets;
  }

  @Data
  @Value
  public static class Bundle {
    @NotNull String name;
    // In the protocol, it is able to be null
    @Nullable CompoundBinaryTag tag;
  }
}
