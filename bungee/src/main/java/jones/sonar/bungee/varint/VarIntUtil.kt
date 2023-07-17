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

package jones.sonar.bungee.varint

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.CorruptedFrameException
import kotlin.math.ceil

class VarIntUtil {
  companion object {
    fun readVarInt(buf: ByteBuf): Int {
      return readVarInt0(buf, 5)
    }

    private fun readVarInt0(buf: ByteBuf, maxBytes: Int): Int {
      val read = readVarIntSafely(buf, maxBytes)
      if (read == Int.MIN_VALUE) {
        throw CorruptedFrameException("Bad VarInt decoded")
      }
      return read
    }

    private fun readVarIntSafely(buf: ByteBuf, maxBytes: Int): Int {
      var i = 0
      val maxRead = maxBytes.coerceAtMost(buf.readableBytes())
      for (j in 0 until maxRead) {
        val k = buf.readByte().toInt()
        i = i or (k and 0x7F shl j * 7)
        if (k and 0x80 != 128) {
          return i
        }
      }
      return Int.MIN_VALUE
    }

    private val EXACT_BYTE_LENGTHS = IntArray(33)

    init {
      for (i in 0..31) {
        EXACT_BYTE_LENGTHS[i] = ceil((31.0 - (i - 1)) / 7).toInt()
      }
      EXACT_BYTE_LENGTHS[32] = 1; // Special case for the number 0.
    }

    fun varIntBytes(value: Int): Int {
      return EXACT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(value)]
    }

    fun write21BitVarInt(buf: ByteBuf, value: Int) {
      // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
      val w = value and 0x7F or 0x80 shl 16 or (value ushr 7 and 0x7F or 0x80 shl 8) or (value ushr 14)
      buf.writeMedium(w)
    }

    fun writeVarInt(buf: ByteBuf, value: Int) {
      // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
      // that the proxy will write, to improve inlining.
      if (value and (-0x1 shl 7) == 0) {
        buf.writeByte(value)
      } else if (value and (-0x1 shl 14) == 0) {
        val w = value and 0x7F or 0x80 shl 8 or (value ushr 7)
        buf.writeShort(w)
      } else {
        writeVarIntFull(buf, value)
      }
    }

    private fun writeVarIntFull(buf: ByteBuf, value: Int) {
      // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
      if (value and (-0x1 shl 7) == 0) {
        buf.writeByte(value)
      } else if (value and (-0x1 shl 14) == 0) {
        val w = value and 0x7F or 0x80 shl 8 or (value ushr 7)
        buf.writeShort(w)
      } else if (value and (-0x1 shl 21) == 0) {
        val w = value and 0x7F or 0x80 shl 16 or (value ushr 7 and 0x7F or 0x80 shl 8) or (value ushr 14)
        buf.writeMedium(w)
      } else if (value and (-0x1 shl 28) == 0) {
        val w = (value and 0x7F or 0x80 shl 24 or (value ushr 7 and 0x7F or 0x80 shl 16)
          or (value ushr 14 and 0x7F or 0x80 shl 8) or (value ushr 21))
        buf.writeInt(w)
      } else {
        val w = value and 0x7F or 0x80 shl 24 or (value ushr 7 and 0x7F or 0x80 shl 16
          ) or (value ushr 14 and 0x7F or 0x80 shl 8) or (value ushr 21 and 0x7F or 0x80)
        buf.writeInt(w)
        buf.writeByte(value ushr 28)
      }
    }
  }
}
