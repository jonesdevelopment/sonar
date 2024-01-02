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

package xyz.jonesdev.sonar.common.fallback.protocol.packets.play;

import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import static xyz.jonesdev.sonar.common.utility.component.ComponentSerializer.serialize;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.writeNamelessCompoundTag;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.writeString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class Disconnect implements FallbackPacket {
  private @Nullable String reason;
  private BinaryTag binaryTag;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_3) >= 0) {
      writeNamelessCompoundTag(byteBuf, binaryTag);
    } else {
      writeString(byteBuf, reason);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public static @NotNull Disconnect create(final Component component) {
    final String serialized = JSONComponentSerializer.json().serialize(component);
    final BinaryTag binaryTag = serialize(new JsonParser().parse(serialized));
    return new Disconnect(serialized, binaryTag);
  }
}
