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

package xyz.jonesdev.sonar.common.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Jon Chambers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// Taken from
// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/api/src/main/java/com/velocitypowered/api/util/FastUuidSansHyphens.java
/**
 * This is a modified FastUUID implementation. The primary difference is that it does not dash its
 * UUIDs. As the native Java 9+ UUID.toString() implementation dashes its UUIDs, we use the FastUUID
 * methods, which ought to be faster than a String.replace().
 */
@UtilityClass
public class FastUuidSansHyphens {
  private static final int MOJANG_BROKEN_UUID_LENGTH = 32;

  private static final char[] HEX_DIGITS =
    new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

  /**
   * Returns a string representation of the given UUID. The returned string is formatted as a
   * Mojang-style UUID.
   *
   * @param uuid the UUID to represent as a string
   *
   * @return a string representation of the given UUID
   */
  public static @NotNull String toString(final @NotNull UUID uuid) {
    final long mostSignificantBits = uuid.getMostSignificantBits();
    final long leastSignificantBits = uuid.getLeastSignificantBits();

    final char[] uuidChars = new char[MOJANG_BROKEN_UUID_LENGTH];

    uuidChars[0]  = HEX_DIGITS[(int) ((mostSignificantBits & 0xf000000000000000L) >>> 60)];
    uuidChars[1]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x0f00000000000000L) >>> 56)];
    uuidChars[2]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x00f0000000000000L) >>> 52)];
    uuidChars[3]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x000f000000000000L) >>> 48)];
    uuidChars[4]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000f00000000000L) >>> 44)];
    uuidChars[5]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000f0000000000L) >>> 40)];
    uuidChars[6]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000f000000000L) >>> 36)];
    uuidChars[7]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000f00000000L) >>> 32)];
    uuidChars[8]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000f0000000L) >>> 28)];
    uuidChars[9]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000000f000000L) >>> 24)];
    uuidChars[10] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000000f00000L) >>> 20)];
    uuidChars[11] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000000f0000L) >>> 16)];
    uuidChars[12] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000000000f000L) >>> 12)];
    uuidChars[13] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000000000f00L) >>> 8)];
    uuidChars[14] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000000000f0L) >>> 4)];
    uuidChars[15] = HEX_DIGITS[(int)  (mostSignificantBits & 0x000000000000000fL)];
    uuidChars[16] = HEX_DIGITS[(int) ((leastSignificantBits & 0xf000000000000000L) >>> 60)];
    uuidChars[17] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0f00000000000000L) >>> 56)];
    uuidChars[18] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00f0000000000000L) >>> 52)];
    uuidChars[19] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000f000000000000L) >>> 48)];
    uuidChars[20] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000f00000000000L) >>> 44)];
    uuidChars[21] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000f0000000000L) >>> 40)];
    uuidChars[22] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000f000000000L) >>> 36)];
    uuidChars[23] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000f00000000L) >>> 32)];
    uuidChars[24] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000f0000000L) >>> 28)];
    uuidChars[25] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000000f000000L) >>> 24)];
    uuidChars[26] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000000f00000L) >>> 20)];
    uuidChars[27] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000000f0000L) >>> 16)];
    uuidChars[28] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000000000f000L) >>> 12)];
    uuidChars[29] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000000000f00L) >>> 8)];
    uuidChars[30] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000000000f0L) >>> 4)];
    uuidChars[31] = HEX_DIGITS[(int)  (leastSignificantBits & 0x000000000000000fL)];

    return new String(uuidChars);
  }
}
