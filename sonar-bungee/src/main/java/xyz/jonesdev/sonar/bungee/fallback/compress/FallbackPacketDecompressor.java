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
import io.netty.handler.codec.CorruptedFrameException;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.compress.PacketDecompressor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;
import static xyz.jonesdev.sonar.common.protocol.VarIntUtil.readVarInt;

@Getter
public final class FallbackPacketDecompressor extends PacketDecompressor {
  @Setter
  private int compressionThreshold;
  private final VelocityCompressor compressor;

  private static final int VANILLA_MAXIMUM_UNCOMPRESSED_SIZE = 8 * 1024 * 1024; // 8MiB
  private static final int HARD_MAXIMUM_UNCOMPRESSED_SIZE = 16 * 1024 * 1024; // 16MiB
  private static final int UNCOMPRESSED_CAP = Boolean.getBoolean("sonar.increased-compression-cap")
      ? HARD_MAXIMUM_UNCOMPRESSED_SIZE : VANILLA_MAXIMUM_UNCOMPRESSED_SIZE;

  public FallbackPacketDecompressor(final int compressionThreshold, final VelocityCompressor compressor) {
    super(compressionThreshold);
    this.compressionThreshold = compressionThreshold;
    this.compressor = compressor;
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
  protected void decode(final ChannelHandlerContext ctx,
                        final @NotNull ByteBuf in,
                        final List<Object> out) throws Exception {
    final int claimedUncompressedSize = readVarInt(in);
    if (claimedUncompressedSize == 0) {
      // This message is not compressed.
      out.add(in.retain());

      // Check for invalid packet size after the packet is retained
      final int size = in.readableBytes();
      if (size > compressionThreshold + 2) {
        throw new CorruptedFrameException("Invalid uncompressed packet size " + size);
      }
      return;
    }

    if (claimedUncompressedSize < compressionThreshold) {
      throw new CorruptedFrameException(claimedUncompressedSize + " is less than " + compressionThreshold);
    }
    if (claimedUncompressedSize > UNCOMPRESSED_CAP) {
      throw new CorruptedFrameException(claimedUncompressedSize + " exceeds maximum size");
    }

    final ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), compressor, in);
    final ByteBuf uncompressed = preferredBuffer(ctx.alloc(), compressor, claimedUncompressedSize);

    try {
      compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
      out.add(uncompressed);
    } catch (Exception e) {
      uncompressed.release();
      throw e;
    } finally {
      compatibleIn.release();
    }
  }
}
