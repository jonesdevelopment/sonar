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

package xyz.jonesdev.sonar.common.utility.nbt;

import com.google.gson.*;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Taken from
// https://github.com/Nan1t/NanoLimbo/pull/79/files#diff-0e355ffd4eb0e15e2c2e628227debc9b6ccefeb167d7b9619311cdc7db2645ec
@UtilityClass
public class NBTMessageUtil {
  public static BinaryTag fromJson(final JsonElement json) {
    if (json instanceof JsonPrimitive) {
      JsonPrimitive jsonPrimitive = (JsonPrimitive) json;
      if (jsonPrimitive.isNumber()) {
        Number number = json.getAsNumber();

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
        }
      } else if (jsonPrimitive.isString()) {
        return StringBinaryTag.stringBinaryTag(jsonPrimitive.getAsString());
      } else if (jsonPrimitive.isBoolean()) {
        return ByteBinaryTag.byteBinaryTag(jsonPrimitive.getAsBoolean() ? (byte) 1 : (byte) 0);
      } else {
        throw new IllegalArgumentException("Unknown JSON primitive: " + jsonPrimitive);
      }
    } else if (json instanceof JsonObject) {
      final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
      for (Map.Entry<String, JsonElement> property : ((JsonObject) json).entrySet()) {
        builder.put(property.getKey(), fromJson(property.getValue()));
      }
      return builder.build();
    } else if (json instanceof JsonArray) {
      final JsonArray jsonArray = ((JsonArray) json);

      if (jsonArray.size() == 0) {
        return ListBinaryTag.listBinaryTag(EndBinaryTag.endBinaryTag().type(), Collections.emptyList());
      }

      final BinaryTagType<?> tagByteType = ByteBinaryTag.ZERO.type();
      final BinaryTagType<?> tagIntType = IntBinaryTag.intBinaryTag(0).type();
      final BinaryTagType<?> tagLongType = LongBinaryTag.longBinaryTag(0).type();

      final BinaryTag listTag;
      final BinaryTagType<?> listType = fromJson(jsonArray.get(0)).type();

      if (listType.equals(tagByteType)) {
        byte[] bytes = new byte[jsonArray.size()];
        for (int i = 0; i < bytes.length; i++) {
          bytes[i] = (Byte) jsonArray.get(i).getAsNumber();
        }
        listTag = ByteArrayBinaryTag.byteArrayBinaryTag(bytes);
      } else if (listType.equals(tagIntType)) {
        int[] ints = new int[jsonArray.size()];
        for (int i = 0; i < ints.length; i++) {
          ints[i] = (Integer) jsonArray.get(i).getAsNumber();
        }
        listTag = IntArrayBinaryTag.intArrayBinaryTag(ints);
      } else if (listType.equals(tagLongType)) {
        long[] longs = new long[jsonArray.size()];
        for (int i = 0; i < longs.length; i++) {
          longs[i] = (Long) jsonArray.get(i).getAsNumber();
        }
        listTag = LongArrayBinaryTag.longArrayBinaryTag(longs);
      } else {
        final List<BinaryTag> tagItems = new ArrayList<>(jsonArray.size());

        for (final JsonElement jsonEl : jsonArray) {
          final BinaryTag subTag = fromJson(jsonEl);
          if (subTag.type() != listType) {
            throw new IllegalArgumentException("Cannot convert mixed JsonArray to Tag");
          }
          tagItems.add(subTag);
        }
        listTag = ListBinaryTag.listBinaryTag(listType, tagItems);
      }
      return listTag;
    } else if (json instanceof JsonNull) {
      return EndBinaryTag.endBinaryTag();
    }
    throw new IllegalArgumentException("Unknown JSON element: " + json);
  }
}
