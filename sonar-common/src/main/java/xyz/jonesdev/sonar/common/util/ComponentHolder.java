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

import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion.MINECRAFT_1_20_3;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeNamelessCompoundTag;
import static xyz.jonesdev.sonar.common.util.ProtocolUtil.writeString;

public final class ComponentHolder {
  private final String serializedComponent;
  private BinaryTag cachedBinaryTag;

  public ComponentHolder(final Component component) {
    this.serializedComponent = GsonComponentSerializer.gson().serialize(component);
  }

  // Mostly taken from
  // https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/packet/chat/ComponentHolder.java
  private BinaryTag serialize(final JsonElement json) {
    if (json instanceof JsonPrimitive) {
      final JsonPrimitive jsonPrimitive = (JsonPrimitive) json;

      if (jsonPrimitive.isNumber()) {
        final Number number = json.getAsNumber();

        if (number instanceof Byte) {
          return ByteBinaryTag.byteBinaryTag((Byte) number);
        } else if (number instanceof Short) {
          return ShortBinaryTag.shortBinaryTag((Short) number);
        } else if (number instanceof Integer) {
          return IntBinaryTag.intBinaryTag((Integer) number);
        } else if (number instanceof Long) {
          return LongBinaryTag.longBinaryTag((Long) number);
        } else if (number instanceof Float) {
          return FloatBinaryTag.floatBinaryTag((Float) number);
        } else if (number instanceof Double) {
          return DoubleBinaryTag.doubleBinaryTag((Double) number);
        } else if (number instanceof LazilyParsedNumber) {
          return IntBinaryTag.intBinaryTag(number.intValue());
        }
      } else if (jsonPrimitive.isString()) {
        return StringBinaryTag.stringBinaryTag(jsonPrimitive.getAsString());
      } else if (jsonPrimitive.isBoolean()) {
        return ByteBinaryTag.byteBinaryTag((byte) (jsonPrimitive.getAsBoolean() ? 1 : 0));
      } else {
        throw new IllegalArgumentException("Unknown JSON primitive: " + jsonPrimitive);
      }
    } else if (json instanceof JsonObject) {
      final CompoundBinaryTag.Builder compound = CompoundBinaryTag.builder();

      for (final Map.Entry<String, JsonElement> property : ((JsonObject) json).entrySet()) {
        compound.put(property.getKey(), serialize(property.getValue()));
      }

      return compound.build();
    } else if (json instanceof JsonArray) {
      final JsonArray jsonArray = json.getAsJsonArray();

      if (jsonArray.size() == 0) {
        return ListBinaryTag.empty();
      }

      final List<BinaryTag> tagItems = new ArrayList<>(jsonArray.size());
      BinaryTagType<? extends BinaryTag> listType = null;

      for (final JsonElement jsonEl : jsonArray) {
        final BinaryTag tag = serialize(jsonEl);
        tagItems.add(tag);

        if (listType == null) {
          listType = tag.type();
        } else if (listType != tag.type()) {
          listType = BinaryTagTypes.COMPOUND;
        }
      }

      switch (Objects.requireNonNull(listType).id()) {
        case 1://BinaryTagTypes.BYTE:
          final byte[] bytes = new byte[jsonArray.size()];
          for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (Byte) jsonArray.get(i).getAsNumber();
          }

          return ByteArrayBinaryTag.byteArrayBinaryTag(bytes);
        case 3://BinaryTagTypes.INT:
          final int[] ints = new int[jsonArray.size()];
          for (int i = 0; i < ints.length; i++) {
            ints[i] = (Integer) jsonArray.get(i).getAsNumber();
          }

          return IntArrayBinaryTag.intArrayBinaryTag(ints);
        case 4://BinaryTagTypes.LONG:
          final long[] longs = new long[jsonArray.size()];
          for (int i = 0; i < longs.length; i++) {
            longs[i] = (Long) jsonArray.get(i).getAsNumber();
          }

          return LongArrayBinaryTag.longArrayBinaryTag(longs);
        case 10://BinaryTagTypes.COMPOUND:
          tagItems.replaceAll(tag -> {
            if (tag.type() == BinaryTagTypes.COMPOUND) {
              return tag;
            } else {
              return CompoundBinaryTag.builder().put("", tag).build();
            }
          });
          break;
      }

      return ListBinaryTag.listBinaryTag(listType, tagItems);
    }

    return EndBinaryTag.endBinaryTag();
  }

  public void write(final @NotNull ByteBuf byteBuf,
                    final @NotNull ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(MINECRAFT_1_20_3) >= 0) {
      if (cachedBinaryTag == null) {
        cachedBinaryTag = serialize(new JsonParser().parse(serializedComponent));
      }
      writeNamelessCompoundTag(byteBuf, cachedBinaryTag);
    } else {
      writeString(byteBuf, serializedComponent);
    }
  }
}
