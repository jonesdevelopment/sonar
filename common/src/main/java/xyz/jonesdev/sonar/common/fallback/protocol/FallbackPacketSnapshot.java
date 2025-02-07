/*
 * Copyright (C) 2025 Sonar Contributors
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
import io.netty.handler.codec.EncoderException;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.ID_TO_PROTOCOL_CONSTANT;

// https://github.com/Nan1t/NanoLimbo/blob/main/src/main/java/ua/nanit/limbo/protocol/PacketSnapshot.java
public final class FallbackPacketSnapshot implements FallbackPacket {
  private final IntObjectMap<Integer> mappings = new IntObjectHashMap<>(ID_TO_PROTOCOL_CONSTANT.size());
  private final IntObjectMap<byte[]> cachedBytes = new IntObjectHashMap<>(ID_TO_PROTOCOL_CONSTANT.size());
  @Getter
  private final Class<? extends FallbackPacket> originalPacketClass;

  public FallbackPacketSnapshot(final @NotNull FallbackPacket originalPacket) {
    this.originalPacketClass = originalPacket.getClass();

    final IntObjectMap<Integer> hashedData = new IntObjectHashMap<>(ID_TO_PROTOCOL_CONSTANT.size());

    for (final ProtocolVersion protocolVersion : ID_TO_PROTOCOL_CONSTANT.values()) {
      // Allocate a buffer for each protocol version
      final ByteBuf byteBuf = Unpooled.buffer();
      try {
        try {
          originalPacket.encode(byteBuf, protocolVersion);
        } catch (Throwable throwable) {
          Sonar.get0().getLogger().error("Could not encode packet {} for version {}: {}",
            originalPacket, protocolVersion, throwable);
          break;
        }

        final Integer protocol = protocolVersion.getProtocol();
        final Integer hash = byteBuf.hashCode();
        final Integer hashed = hashedData.getOrDefault(hash, -1);

        if (hashed != -1) {
          mappings.put(protocol, hashed);
        } else {
          hashedData.put(hash, protocol);
          mappings.put(protocol, protocol);
          // Cache the raw bytes of the encoded packet
          final byte[] bytes = new byte[byteBuf.readableBytes()];
          byteBuf.readBytes(bytes);
          cachedBytes.put(protocol, bytes);
        }
      } finally {
        // Make sure to release the buffer to avoid memory leaks
        byteBuf.release();
      }
    }
  }

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    final Integer hash = mappings.get(protocolVersion.getProtocol());
    if (hash == null) {
      throw ProtocolUtil.DEBUG ? new EncoderException("Unable to find hash") : QuietDecoderException.INSTANCE;
    }
    final byte[] bytes = cachedBytes.get(hash);
    if (bytes == null) {
      throw ProtocolUtil.DEBUG ? new EncoderException("Unable to find cached packet") : QuietDecoderException.INSTANCE;
    }
    byteBuf.writeBytes(bytes);
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }
}
