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

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.netty.DeferredByteBufHolder;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_13;
import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;
import static xyz.jonesdev.sonar.common.utility.protocol.ProtocolUtil.*;

// Mostly taken from
// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/packet/PluginMessage.java
@Getter
@ToString
public final class PluginMessagePacket extends DeferredByteBufHolder implements FallbackPacket {
  private String channel;

  public PluginMessagePacket() {
    super(null);
  }

  @Override
  public int expectedMinLength(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    return 4;
  }

  @Override
  public int expectedMaxLength(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    return 0xFFF; // strict size limit
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    channel = readString(byteBuf, 512);

    if (protocolVersion.compareTo(MINECRAFT_1_13) >= 0) {
      channel = transformLegacyToModernChannel(channel);
    }

    if (protocolVersion.compareTo(MINECRAFT_1_8) >= 0) {
      replace(byteBuf.readRetainedSlice(byteBuf.readableBytes()));
    } else {
      replace(readRetainedByteBufSlice17(byteBuf));
    }
  }

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }
}
