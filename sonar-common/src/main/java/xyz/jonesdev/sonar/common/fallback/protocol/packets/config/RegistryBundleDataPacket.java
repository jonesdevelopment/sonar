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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.writeNamelessCompoundTag;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.writeString;
import static xyz.jonesdev.sonar.common.utility.protocol.VarIntUtil.writeVarInt;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RegistryBundleDataPacket implements FallbackPacket {

  public String type;
  public List<Bundle> bundleList;

  @Override
  public void encode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    writeString(byteBuf, Objects.requireNonNull(type, "Type cannot be null!"));
    final List<Bundle> bundleList = Objects.requireNonNull(this.bundleList, "bundleList cannot be null!");
    writeVarInt(byteBuf, bundleList.size());
    for (final Bundle bundle : bundleList) {
      writeString(byteBuf, bundle.name);
      final CompoundBinaryTag tag = bundle.tag;
      if (tag != null) {
        byteBuf.writeBoolean(true);
        writeNamelessCompoundTag(byteBuf, tag);
      } else {
        byteBuf.writeBoolean(false);
      }
    }
  }

  @Override
  public void decode(ByteBuf byteBuf, ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }

  public static @NotNull List<FallbackPacket> of(@NotNull final CompoundBinaryTag rootTag) {
    final List<FallbackPacket> packets = new ArrayList<>();
    for (final String type : rootTag.keySet()) {
      final ArrayList<Bundle> bundles = new ArrayList<>();
      for (final BinaryTag binaryTag : rootTag.getCompound(type).getList("value")) {
        final CompoundBinaryTag tag = (CompoundBinaryTag) binaryTag;
        bundles.add(new Bundle(tag.getString("name"), tag.getCompound("element")));
      }
      packets.add(new RegistryBundleDataPacket(type, bundles));
    }
    return packets;
  }

  @Data
  @Value
  public static class Bundle {
    @NotNull String name;
    @Nullable CompoundBinaryTag tag; // In the protocol it designed can be null. But not sure if this can be mark to @Nullable.
  }
}
