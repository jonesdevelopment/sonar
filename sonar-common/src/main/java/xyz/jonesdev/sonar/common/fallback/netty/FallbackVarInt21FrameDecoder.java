/*
 * Copyright (C) 2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/netty/MinecraftVarintFrameDecoder.java
public final class FallbackVarInt21FrameDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(final @NotNull ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.clear();
      return;
    }

    final FallbackVarInt21ByteDecoder reader = new FallbackVarInt21ByteDecoder();
    final int end = in.forEachByte(reader);

    if (end == -1) {
      // We tried to go beyond the end of the buffer.
      // This is probably a good sign that the buffer was too short to hold a proper varint.
      if (reader.getResult() == FallbackVarInt21ByteDecoder.DecodeResult.RUN_OF_ZEROES) {
        // Special case where the entire packet is just a run of zeroes. We ignore them all.
        in.clear();
      }
      return;
    }

    if (reader.getResult() == FallbackVarInt21ByteDecoder.DecodeResult.RUN_OF_ZEROES) {
      // This will return to the point where the next varint starts
      in.readerIndex(end);
    } else if (reader.getResult() == FallbackVarInt21ByteDecoder.DecodeResult.SUCCESS) {
      final int readVarInt = reader.getReadVarInt();
      final int bytesRead = reader.getBytesRead();

      if (readVarInt < 0) {
        in.clear();
        throw new CorruptedFrameException("Invalid VarInt length " + readVarInt);
      } else if (readVarInt == 0) {
        // Skip over the empty packet(s) and ignore it
        in.readerIndex(end + 1);
      } else {
        int minimumRead = bytesRead + readVarInt;
        if (in.isReadable(minimumRead)) {
          out.add(in.retainedSlice(end + 1, readVarInt));
          in.skipBytes(minimumRead);
        }
      }
    } else if (reader.getResult() == FallbackVarInt21ByteDecoder.DecodeResult.TOO_BIG) {
      in.clear();
      throw new CorruptedFrameException("Too big VarInt " + reader.getReadVarInt());
    }
  }
}
