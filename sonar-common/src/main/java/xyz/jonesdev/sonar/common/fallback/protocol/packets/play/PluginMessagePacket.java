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
import io.netty.handler.codec.CorruptedFrameException;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_8;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readExtendedForgeShort;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.readString;

@Getter
@ToString
public final class PluginMessagePacket implements FallbackPacket {
  private String channel;
  private byte[] data;

  private static final int FORGE_MAX_ARRAY_LENGTH = Integer.MAX_VALUE & 0x1FFF9A;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    channel = readString(byteBuf, 48);

    final int length;
    if (protocolVersion.compareTo(MINECRAFT_1_8) >= 0) {
      length = byteBuf.readableBytes();
      if (length > Short.MAX_VALUE) {
        throw new CorruptedFrameException("Got too much data: " + length);
      }
    } else {
      length = readExtendedForgeShort(byteBuf);
      if (length > FORGE_MAX_ARRAY_LENGTH) {
        throw new CorruptedFrameException("Got too much data: " + length);
      }
    }

    data = new byte[length];
    byteBuf.readBytes(data);
  }

  @Override
  public int expectedMinLength(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    return 4;
  }

  @Override
  public int expectedMaxLength(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    return 0xFFF; // strict size limit
  }
}
