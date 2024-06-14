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

package xyz.jonesdev.sonar.common.fallback.protocol.dimension;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;

import java.io.InputStream;
import java.util.Objects;

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
  public final @NotNull CompoundBinaryTag CODEC_1_21;
  public final @NotNull CompoundBinaryTag OLD_CODEC;

  static {
    CODEC_1_16 = getCodec("codec_1_16.nbt");
    CODEC_1_18_2 = getCodec("codec_1_18_2.nbt");
    CODEC_1_19 = getCodec("codec_1_19.nbt");
    CODEC_1_19_1 = getCodec("codec_1_19_1.nbt");
    CODEC_1_19_4 = getCodec("codec_1_19_4.nbt");
    CODEC_1_20 = getCodec("codec_1_20.nbt");
    CODEC_1_21 = getCodec("codec_1_21.nbt");
    OLD_CODEC = getCodec("codec_old.nbt");

    DEFAULT_DIMENSION_1_16 = getDimension(CODEC_1_16);
    DEFAULT_DIMENSION_1_18_2 = getDimension(CODEC_1_18_2);
  }

  private static @NotNull DimensionInfo getDimension(final @NotNull CompoundBinaryTag tag) {
    final ListBinaryTag dimensions = tag.getCompound("minecraft:dimension_type").getList("value");
    final BinaryTag elementTag = ((CompoundBinaryTag) dimensions.get(0)).get("element");
    return new DimensionInfo("minecraft:overworld", 0, (CompoundBinaryTag) elementTag);
  }

  private @NotNull CompoundBinaryTag getCodec(final @NotNull String fileName) {
    try (final InputStream inputStream = Sonar.class.getResourceAsStream("/assets/codecs/" + fileName)) {
      return BinaryTagIO.reader().read(Objects.requireNonNull(inputStream), BinaryTagIO.Compression.GZIP);
    } catch (Throwable throwable) {
      Sonar.get().getLogger().error("Could not load mappings for {}: {}", fileName, throwable);
      throw new IllegalStateException(throwable);
    }
  }
}
