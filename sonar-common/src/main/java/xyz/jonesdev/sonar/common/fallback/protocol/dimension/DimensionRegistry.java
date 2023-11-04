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

package xyz.jonesdev.sonar.common.fallback.protocol.dimension;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@UtilityClass
public final class DimensionRegistry {
  public final DimensionInfo DEFAULT_DIMENSION_1_16;
  public final DimensionInfo DEFAULT_DIMENSION_1_18_2;

  public final @NotNull CompoundBinaryTag CODEC_1_16;
  public final @NotNull CompoundBinaryTag CODEC_1_18_2;
  public final @NotNull CompoundBinaryTag CODEC_1_19;
  public final @NotNull CompoundBinaryTag CODEC_1_19_1;
  public final @NotNull CompoundBinaryTag CODEC_1_19_4;
  public final @NotNull CompoundBinaryTag CODEC_1_20;
  public final @NotNull CompoundBinaryTag OLD_CODEC;

  static  {
    CODEC_1_16 = getMapping("codec_1_16.snbt");
    CODEC_1_18_2 = getMapping("codec_1_18_2.snbt");
    CODEC_1_19 = getMapping("codec_1_19.snbt");
    CODEC_1_19_1 = getMapping("codec_1_19_1.snbt");
    CODEC_1_19_4 = getMapping("codec_1_19_4.snbt");
    CODEC_1_20 = getMapping("codec_1_20.snbt");
    OLD_CODEC = getMapping("codec_old.snbt");

    DEFAULT_DIMENSION_1_16 = getDimension(CODEC_1_16);
    DEFAULT_DIMENSION_1_18_2 = getDimension(CODEC_1_18_2);
  }

  private static @NotNull DimensionInfo getDimension(final @NotNull CompoundBinaryTag tag) {
    final ListBinaryTag dimensions = tag.getCompound("minecraft:dimension_type").getList("value");
    final BinaryTag elementTag = ((CompoundBinaryTag) dimensions.get(0)).get("element");
    return new DimensionInfo("minecraft:overworld", 0, (CompoundBinaryTag) elementTag);
  }

  private static CompoundBinaryTag getMapping(final @NotNull String resPath) {
    try (final InputStream inputStream = Sonar.class.getResourceAsStream("/mappings/" + resPath)) {
      return TagStringIO.get().asCompound(streamToString(inputStream));
    } catch (IOException exception) {
      throw new IllegalStateException("Could not find mappings for " + resPath);
    }
  }

  private static @NotNull String streamToString(final InputStream inputStream) throws IOException {
    try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      try (final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
        return bufferedReader.lines().collect(Collectors.joining(Sonar.LINE_SEPARATOR));
      }
    }
  }
}
