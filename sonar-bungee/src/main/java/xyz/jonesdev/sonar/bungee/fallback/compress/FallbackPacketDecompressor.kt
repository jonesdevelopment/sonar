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
import net.md_5.bungee.compress.PacketDecompressor
import xyz.jonesdev.sonar.common.protocol.VarIntUtil.readVarInt

// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/netty/MinecraftCompressDecoder.java
class FallbackPacketDecompressor(
  private var compressionThreshold: Int,
  private val velocityCompressor: VelocityCompressor
) : PacketDecompressor(compressionThreshold) {

  @Throws(Exception::class)
  override fun handlerAdded(ctx: ChannelHandlerContext) {
    // Don't let the PacketDecompressor handle this
  }

  companion object {
    private const val MAXIMUM_UNCOMPRESSED_SIZE = 8 * 1024 * 1024 // 8MiB
  }

  @Suppress("unused") // TODO
  fun setCompressionThreshold(compressionThreshold: Int) {
    this.compressionThreshold = compressionThreshold
  }

  @Throws(Exception::class)
  override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
    val claimedUncompressedSize: Int = readVarInt(msg)
    if (claimedUncompressedSize == 0) {
      // This message is not compressed.
      out.add(msg.retain())

      // Check for invalid packet size after the packet is retained
      val size = msg.readableBytes()
      if (size > compressionThreshold + 2) {
        throw CorruptedFrameException("Invalid uncompressed packet size $size")
      }
      return
    }

    if (claimedUncompressedSize < compressionThreshold) {
      throw CorruptedFrameException("Uncompressed size $claimedUncompressedSize is less than $compressionThreshold")
    }
    if (claimedUncompressedSize > MAXIMUM_UNCOMPRESSED_SIZE) {
      throw CorruptedFrameException("Uncompressed size $claimedUncompressedSize exceeds maximum size")
    }

    val compatibleIn = ensureCompatible(ctx.alloc(), velocityCompressor, msg)
    val uncompressed = preferredBuffer(ctx.alloc(), velocityCompressor, claimedUncompressedSize)
    try {
      velocityCompressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize)
      out.add(uncompressed)
    } catch (exception: Exception) {
      uncompressed.release()
      throw exception
    } finally {
      compatibleIn.release()
    }
  }

  @Throws(Exception::class)
  override fun handlerRemoved(ctx: ChannelHandlerContext) {
    velocityCompressor.close()
  }
}
