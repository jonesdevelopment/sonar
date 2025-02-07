/*
 * Copyright (C) 2025 Sonar Contributors
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
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;

import java.io.InputStream;
import java.util.Objects;

@UtilityClass
public final class DimensionRegistry {
  public final CompoundBinaryTag CODEC_1_16;
  public final CompoundBinaryTag CODEC_1_16_2;
  public final CompoundBinaryTag CODEC_1_18_2;
  public final CompoundBinaryTag CODEC_1_19;
  public final CompoundBinaryTag CODEC_1_19_1;
  public final CompoundBinaryTag CODEC_1_19_4;
  public final CompoundBinaryTag CODEC_1_20;
  public final CompoundBinaryTag CODEC_1_20_5;
  public final CompoundBinaryTag CODEC_1_21;
  public final CompoundBinaryTag CODEC_1_21_2;
  public final CompoundBinaryTag CODEC_1_21_4;

  static {
    CODEC_1_16 = getCodec("codec_1_16.nbt");
    CODEC_1_16_2 = getCodec("codec_1_16_2.nbt");
    CODEC_1_18_2 = getCodec("codec_1_18_2.nbt");
    CODEC_1_19 = getCodec("codec_1_19.nbt");
    CODEC_1_19_1 = getCodec("codec_1_19_1.nbt");
    CODEC_1_19_4 = getCodec("codec_1_19_4.nbt");
    CODEC_1_20 = getCodec("codec_1_20.nbt");
    CODEC_1_20_5 = getCodec("codec_1_20_5.nbt");
    CODEC_1_21 = getCodec("codec_1_21.nbt");
    CODEC_1_21_2 = getCodec("codec_1_21_2.nbt");
    CODEC_1_21_4 = getCodec("codec_1_21_4.nbt");
  }

  private @NotNull CompoundBinaryTag getCodec(final @NotNull String fileName) {
    try (final InputStream inputStream = Sonar.class.getResourceAsStream("/assets/codecs/" + fileName)) {
      return BinaryTagIO.unlimitedReader().read(Objects.requireNonNull(inputStream), BinaryTagIO.Compression.GZIP);
    } catch (Throwable throwable) {
      Sonar.get0().getLogger().error("Could not load mappings for {}: {}", fileName, throwable);
      throw new IllegalStateException(throwable);
    }
  }
}
