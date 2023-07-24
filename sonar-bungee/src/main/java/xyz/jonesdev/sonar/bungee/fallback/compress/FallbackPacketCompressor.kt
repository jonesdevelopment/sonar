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

package xyz.jonesdev.sonar.bungee.fallback.compress

import com.velocitypowered.natives.compression.VelocityCompressor
import com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible
import com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.CorruptedFrameException
import net.md_5.bungee.compress.PacketCompressor
import xyz.jonesdev.sonar.common.protocol.VarIntUtil.Companion.varIntBytes
import xyz.jonesdev.sonar.common.protocol.VarIntUtil.Companion.write21BitVarInt
import xyz.jonesdev.sonar.common.protocol.VarIntUtil.Companion.writeVarInt
import java.util.zip.DataFormatException

// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/netty/MinecraftCompressorAndLengthEncoder.java
class FallbackPacketCompressor(
  private var compressionThreshold: Int,
  private val velocityCompressor: VelocityCompressor
) : PacketCompressor() {

  companion object {
    private const val PROTOCOL_MAXIMUM = 1 shl 21
  }

  @Suppress("unused") // TODO
  fun setCompressionThreshold(compressionThreshold: Int) {
    this.compressionThreshold = compressionThreshold
  }

  override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
    val uncompressed = msg.readableBytes()
    if (uncompressed < compressionThreshold) {
      // Under the threshold, there is nothing to do.
      writeVarInt(out, uncompressed + 1)
      writeVarInt(out, 0)
      out.writeBytes(msg)
      return
    } else {
      handleCompressed(ctx, msg, out);
    }
  }

  @Throws(DataFormatException::class)
  private fun handleCompressed(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
    val uncompressed = msg.readableBytes()

    write21BitVarInt(out, 0)
    writeVarInt(out, uncompressed)

    val compatibleIn = ensureCompatible(ctx.alloc(), velocityCompressor, msg)

    val startCompressed = out.writerIndex()
    try {
      velocityCompressor.deflate(compatibleIn, out)
    } finally {
      compatibleIn.release()
    }

    val compressedLength = out.writerIndex() - startCompressed
    if (compressedLength >= PROTOCOL_MAXIMUM) {
      throw CorruptedFrameException("Very large (over 2MiB compressed) packet.")
    }

    val writerIndex = out.writerIndex()
    val packetLength = out.readableBytes() - 3
    out.writerIndex(0)
    write21BitVarInt(out, packetLength) // Rewrite packet length
    out.writerIndex(writerIndex)
  }

  @Throws(Exception::class)
  override fun handlerRemoved(ctx: ChannelHandlerContext) {
    velocityCompressor.close()
  }

  @Throws(Exception::class)
  override fun allocateBuffer(ctx: ChannelHandlerContext, msg: ByteBuf, preferDirect: Boolean): ByteBuf {
    val uncompressed = msg.readableBytes()
    if (uncompressed < compressionThreshold) {
      var finalBufferSize = uncompressed + 1
      finalBufferSize += varIntBytes(finalBufferSize)
      return ctx.alloc().directBuffer(finalBufferSize)
    }

    // (maximum data length after compression) + packet length varint + uncompressed data varint
    val initialBufferSize: Int = uncompressed - 1 + 3 + varIntBytes(uncompressed)
    return preferredBuffer(ctx.alloc(), velocityCompressor, initialBufferSize)
  }
}
