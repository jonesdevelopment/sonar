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

import io.netty.util.ByteProcessor;
import lombok.Getter;
import lombok.ToString;

// Taken from
// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/netty/VarintByteDecoder.java
@Getter
@ToString
public final class Varint21ByteDecoder implements ByteProcessor {
  private DecoderResult result = DecoderResult.TOO_SMALL;
  private int readVarInt, bytesRead;

  @Override
  public boolean process(byte b) throws Exception {
    if (b == 0 && bytesRead == 0) {
      result = DecoderResult.RUN_OF_ZEROES;
      return true;
    }

    if (result == DecoderResult.RUN_OF_ZEROES) {
      return false;
    }

    readVarInt |= (b & 0x7F) << bytesRead++ * 7;

    if (bytesRead > 3) {
      result = DecoderResult.TOO_BIG;
      return false;
    }

    if ((b & 0x80) != 128) {
      result = DecoderResult.SUCCESS;
      return false;
    }
    return true;
  }

  public enum DecoderResult {
    SUCCESS,
    TOO_SMALL,
    TOO_BIG,
    RUN_OF_ZEROES
  }
}
