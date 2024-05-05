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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.HashMap;
import java.util.Map;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.ID_TO_PROTOCOL_CONSTANT;

// Mostly taken from
// https://github.com/Nan1t/NanoLimbo/blob/main/src/main/java/ua/nanit/limbo/protocol/PacketSnapshot.java
@Getter
public final class FallbackPacketSnapshot implements FallbackPacket {
  private final Map<Integer, Integer> mappings = new HashMap<>(ID_TO_PROTOCOL_CONSTANT.size());
  private final Map<Integer, byte[]> cachedBytes = new HashMap<>(ID_TO_PROTOCOL_CONSTANT.size());
  private final FallbackPacket originalPacket;

  public FallbackPacketSnapshot(final @NotNull FallbackPacket originalPacket) {
    final Map<Integer, Integer> hashes = new HashMap<>(ID_TO_PROTOCOL_CONSTANT.size());

    for (final ProtocolVersion protocolVersion : ID_TO_PROTOCOL_CONSTANT.values()) {
      // Allocate a buffer for each protocol version
      final ByteBuf byteBuf = Unpooled.buffer();
      try {
        originalPacket.encode(byteBuf, protocolVersion);
      } catch (Exception exception) {
        Sonar.get().getLogger().error("Could not encode packet {} for version {}: {}",
          originalPacket.toString(), protocolVersion, exception);
        break;
      }

      // Make sure we don't unnecessarily fill the RAM
      final int protocol = protocolVersion.getProtocol();
      final int hash = byteBuf.hashCode();
      final int hashed = hashes.getOrDefault(hash, -1);
      if (hashed != -1) {
        mappings.put(protocol, hashed);
      } else {
        hashes.put(hash, protocol);
        mappings.put(protocol, protocol);
        // Cache the raw bytes of the encoded packet
        final byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        cachedBytes.put(protocol, bytes);
      }
      // Make sure to release the buffer to avoid memory leaks
      byteBuf.release();
    }
    this.originalPacket = originalPacket;
  }

  @Override
  public void encode(final @NotNull ByteBuf byteBuf,
                     final @NotNull ProtocolVersion protocolVersion) throws Exception {
    final int hash = mappings.get(protocolVersion.getProtocol());
    final byte[] bytes = cachedBytes.get(hash);

    if (bytes != null) {
      byteBuf.writeBytes(bytes);
    } else {
      Sonar.get().getLogger().error("Could not find cached packet {} for version {}",
        toString(), protocolVersion);
      throw new IllegalStateException("Unable to find cached packet. Contact the developer!");
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }
}
