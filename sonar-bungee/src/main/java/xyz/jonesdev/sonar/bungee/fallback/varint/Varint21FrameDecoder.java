/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.bungee.fallback.varint;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// Mostly taken from
// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/netty/MinecraftVarintFrameDecoder.java
public final class Varint21FrameDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(final @NotNull ChannelHandlerContext ctx,
                        final ByteBuf byteBuf,
                        final List<Object> out) throws Exception {
    if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
      // MessageToMessageDecoder always do release after decoding
      // https://github.com/jonesdevelopment/sonar-legacy/pull/5
      return;
    }

    final VarintByteDecoder reader = new VarintByteDecoder();
    final int end = byteBuf.forEachByte(reader);

    if (end == -1) {
      // This is probably a good sign that the buffer was too short or empty
      // since the ByteBuf cannot hold a proper VarInt.
      if (reader.getResult() == VarintByteDecoder.DecoderResult.RUN_OF_ZEROES) {
        byteBuf.clear();
      }
      return;
    }

    switch (reader.getResult()) {
      case RUN_OF_ZEROES: {
        // this will return to the point where the next varInt starts
        byteBuf.readerIndex(end);
        break;
      }

      case SUCCESS: {
        final int readVarInt = reader.getReadVarInt();

        if (readVarInt < 0) {
          byteBuf.clear();
          throw new CorruptedFrameException("Result: " + reader);
        } else if (readVarInt == 0) {
          // Actually, we don't want to throw an Exception if the packet is empty.
          // The check would also false flag a lot of legit players since packets
          // in 1.7 could sometimes be empty.
          byteBuf.readerIndex(end + 1);
          return;
        }

        final int bytesRead = reader.getBytesRead();
        final int minimumRead = bytesRead + readVarInt;

        if (byteBuf.isReadable(minimumRead)) {
          out.add(byteBuf.retainedSlice(end + 1, readVarInt));

          byteBuf.skipBytes(minimumRead);
        }
        break;
      }

      default: {
        byteBuf.clear();
        throw new CorruptedFrameException("Result: " + reader);
      }
    }
  }
}
