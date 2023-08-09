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

package xyz.jonesdev.sonar.bungee.fallback.compress;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.compress.PacketCompressor;
import org.jetbrains.annotations.NotNull;

import java.util.zip.DataFormatException;

import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;
import static xyz.jonesdev.sonar.common.protocol.VarIntUtil.*;

@Getter
@AllArgsConstructor
public final class FallbackPacketCompressor extends PacketCompressor {
  private int compressionThreshold;
  private final VelocityCompressor compressor;

  private static final int MAX_COMPRESSED_LENGTH = 1 << 21;

  @Override
  public void setThreshold(final int compressionThreshold) {
    this.compressionThreshold = compressionThreshold;
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
    // Do nothing
  }

  @Override
  public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
    compressor.close();
  }

  @Override
  protected void encode(final ChannelHandlerContext ctx,
                        final @NotNull ByteBuf msg,
                        final ByteBuf out) throws Exception {
    final int uncompressed = msg.readableBytes();
    if (uncompressed < compressionThreshold) {
      // Under the threshold, do nothing.
      writeVarInt(out, uncompressed + 1);
      writeVarInt(out, 0);
      out.writeBytes(msg);
    } else {
      handleCompressed(ctx, msg, out);
    }
  }

  private void handleCompressed(final @NotNull ChannelHandlerContext ctx,
                                final @NotNull ByteBuf msg,
                                final ByteBuf out) throws DataFormatException {
    final int uncompressed = msg.readableBytes();

    write21BitVarInt(out, 0); // Fake packet length
    writeVarInt(out, uncompressed);

    final ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), compressor, msg);

    final int startCompressed = out.writerIndex();
    try {
      compressor.deflate(compatibleIn, out);
    } finally {
      compatibleIn.release();
    }

    final int compressedLength = out.writerIndex() - startCompressed;
    if (compressedLength >= MAX_COMPRESSED_LENGTH) {
      throw new DataFormatException("The server sent a very large (over 2MiB compressed) packet.");
    }

    int writerIndex = out.writerIndex();
    int packetLength = out.readableBytes() - 3;
    out.writerIndex(0);
    write21BitVarInt(out, packetLength); // Rewrite packet length
    out.writerIndex(writerIndex);
  }

  @Override
  protected ByteBuf allocateBuffer(final ChannelHandlerContext ctx,
                                   final @NotNull ByteBuf msg,
                                   final boolean preferDirect) throws Exception {
    int uncompressed = msg.readableBytes();
    if (uncompressed < compressionThreshold) {
      int finalBufferSize = uncompressed + 1;
      finalBufferSize += varIntBytes(finalBufferSize);
      return ctx.alloc().directBuffer(finalBufferSize);
    }

    final int initialBufferSize = (uncompressed - 1) + 3 + varIntBytes(uncompressed);
    return preferredBuffer(ctx.alloc(), compressor, initialBufferSize);
  }
}
