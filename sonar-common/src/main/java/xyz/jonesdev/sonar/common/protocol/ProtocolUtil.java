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

package xyz.jonesdev.sonar.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.EncoderException;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static xyz.jonesdev.sonar.common.protocol.VarIntUtil.readVarInt;
import static xyz.jonesdev.sonar.common.protocol.VarIntUtil.writeVarInt;

// Mostly taken from Velocity
@UtilityClass
public class ProtocolUtil {
  private static final int FORGE_MAX_ARRAY_LENGTH = Integer.MAX_VALUE & 0x1FFF9A;

  private static final String BRAND_CHANNEL_LEGACY = "MC|Brand";
  private static final String BRAND_CHANNEL = "minecraft:brand";
  private static final String REGISTER_CHANNEL_LEGACY = "REGISTER";
  private static final String REGISTER_CHANNEL = "minecraft:register";
  private static final String UNREGISTER_CHANNEL_LEGACY = "UNREGISTER";
  private static final String UNREGISTER_CHANNEL = "minecraft:unregister";
  private static final Pattern INVALID_IDENTIFIER_REGEX = Pattern.compile("[^a-z0-9\\-_]*");

  public static @NotNull String readString(final ByteBuf buf) throws CorruptedFrameException {
    return readString(buf, Short.MAX_VALUE);
  }

  public static @NotNull String readString(final ByteBuf buf,
                                    final int cap) throws CorruptedFrameException {
    return readString(buf, cap, false);
  }

  public static @NotNull String readString(final ByteBuf buf,
                                    final int cap,
                                    final boolean legacy) throws CorruptedFrameException {
    final int length = readVarInt(buf);

    checkFrame(length >= 0, "Got a negative-length string");
    checkFrame(length <= cap * 3, "Bad string size");
    String str;
    if (legacy) {
      // TODO: length checking?
      str = buf.toString(StandardCharsets.UTF_8);
      buf.skipBytes(buf.readableBytes());
    } else {
      checkFrame(buf.isReadable(length), "Got an invalid string length");
      str = buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8);
      buf.skipBytes(length);
    }
    checkFrame(str.length() <= cap, "Got a too-long string");
    return str;
  }

  public static void writeString(final ByteBuf buf, final CharSequence str) {
    final int size = ByteBufUtil.utf8Bytes(str);
    writeVarInt(buf, size);
    buf.writeCharSequence(str, StandardCharsets.UTF_8);
  }

  public static void writeArray(final ByteBuf byteBuf, final byte[] bytes) {
    checkFrame(bytes.length < Short.MAX_VALUE, "Too long array");
    writeVarInt(byteBuf, bytes.length);
    byteBuf.writeBytes(bytes);
  }

  public static void writeStringArray(final ByteBuf byteBuf, final String[] stringArray) {
    writeVarInt(byteBuf, stringArray.length);
    for (final String s : stringArray) {
      writeString(byteBuf, s);
    }
  }

  public void writeCompoundTag(final ByteBuf byteBuf, final CompoundBinaryTag compoundTag) {
    try {
      BinaryTagIO.writer().write(compoundTag, (DataOutput) new ByteBufOutputStream(byteBuf));
    } catch (IOException e) {
      throw new EncoderException("Unable to encode NBT CompoundTag");
    }
  }

  private static int readExtendedForgeShort(ByteBuf buf) {
    int low = buf.readUnsignedShort();
    int high = 0;
    if ((low & 0x8000) != 0) {
      low = low & 0x7FFF;
      high = buf.readUnsignedByte();
    }
    return ((high & 0xFF) << 15) | low;
  }

  public static ByteBuf readRetainedByteBufSlice17(final ByteBuf buf) {
    final int length = readExtendedForgeShort(buf);
    checkFrame(length <= FORGE_MAX_ARRAY_LENGTH, "Too long");
    return buf.readRetainedSlice(length);
  }

  public static @NotNull String transformLegacyToModernChannel(@NotNull final String name) {
    if (name.indexOf(':') != -1) {
      return name;
    }

    switch (name) {
      case REGISTER_CHANNEL_LEGACY:
        return REGISTER_CHANNEL;
      case UNREGISTER_CHANNEL_LEGACY:
        return UNREGISTER_CHANNEL;
      case BRAND_CHANNEL_LEGACY:
        return BRAND_CHANNEL;
      case "BungeeCord":
        return "bungeecord:main";
      default:
        final String lower = name.toLowerCase();
        return "legacy:" + INVALID_IDENTIFIER_REGEX.matcher(lower).replaceAll("");
    }
  }

  private void checkFrame(final boolean expression, final String message) {
    if (!expression) {
      throw new CorruptedFrameException(message);
    }
  }
}
