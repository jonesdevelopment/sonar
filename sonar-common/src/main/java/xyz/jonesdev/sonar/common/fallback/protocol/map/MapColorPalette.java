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

package xyz.jonesdev.sonar.common.fallback.protocol.map;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

@UtilityClass
public class MapColorPalette {
  // https://minecraft.wiki/w/Map_item_format#Full_color_tables
  public static final int[] COLOR_MAP = {
    0,
    0,
    0,
    0,
    -10912473,
    -9594576,
    -8408520,
    -12362211,
    -5331853,
    -2766452,
    -530013,
    -8225962,
    -7697782,
    -5658199,
    -3815995,
    -9934744,
    -4980736,
    -2359296,
    -65536,
    -7929856,
    -9408332,
    -7697700,
    -6250241,
    -11250553,
    -9079435,
    -7303024,
    -5789785,
    -10987432,
    -16754944,
    -16750080,
    -16745472,
    -16760576,
    -4934476,
    -2302756,
    -1,
    -7895161,
    -9210239,
    -7499618,
    -5986120,
    -11118495,
    -8304095,
    -6464728,
    -4756945,
    -10471400,
    -11579569,
    -10461088,
    -9408400,
    -12895429,
    -10136525,
    -8689858,
    -7375032,
    -11780314,
    -13816396,
    -13158436,
    -12566273,
    -14605945,
    -4935252,
    -2303533,
    -779,
    -7895679,
    -6792924,
    -4559572,
    -2588877,
    -9288933,
    -8571496,
    -6733382,
    -5092136,
    -10606478,
    -12030824,
    -10976070,
    -10053160,
    -13217422,
    -6184668,
    -3816148,
    -1710797,
    -8816357,
    -10907631,
    -9588715,
    -8401895,
    -12358643,
    -5613196,
    -3117682,
    -884827,
    -8371369,
    -13290187,
    -12500671,
    -11776948,
    -14145496,
    -9671572,
    -8092540,
    -6710887,
    -11447983,
    -13280916,
    -12489340,
    -11763815,
    -14138543,
    -10933123,
    -9619815,
    -8437838,
    -12377762,
    -14404227,
    -13876839,
    -13415246,
    -14997410,
    -12045020,
    -10993364,
    -10073037,
    -13228005,
    -12035804,
    -10982100,
    -10059981,
    -13221093,
    -9690076,
    -8115156,
    -6737101,
    -11461861,
    -15658735,
    -15395563,
    -15132391,
    -15921907,
    -5199818,
    -2634430,
    -332211,
    -8094168,
    -12543338,
    -11551561,
    -10691627,
  };

  public byte[] getBufferFromImage(final @NotNull BufferedImage bufferedImage) {
    final int width = bufferedImage.getWidth();
    final int height = bufferedImage.getHeight();

    final int[] pixels = new int[128 * 128];
    final byte[] buffer = new byte[128 * 128];

    bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

    for (int i = 0; i < pixels.length; i++) {
      buffer[i] = mapColorToMinecraft(pixels[i]);
    }
    return buffer;
  }

  private byte mapColorToMinecraft(final int argb) {
    int minDistance = Integer.MAX_VALUE;
    byte closestIndex = 0;
    for (byte i = 0; i < COLOR_MAP.length; i++) {
      final int distance = calculateColorDistance(argb, COLOR_MAP[i]);
      if (distance < minDistance) {
        minDistance = distance;
        closestIndex = i;
      }
    }
    return closestIndex;
  }

  private int calculateColorDistance(final int argb0, final int argb1) {
    final int red0 = (argb0 >> 16) & 0xFF;
    final int green0 = (argb0 >> 8) & 0xFF;
    final int blue0 = argb0 & 0xFF;
    final int red1 = (argb1 >> 16) & 0xFF;
    final int green1 = (argb1 >> 8) & 0xFF;
    final int blue1 = argb1 & 0xFF;
    // Color distance metric (e.g. squared Euclidean distance)
    return (red0 - red1) * (red0 - red1)
      + (green0 - green1) * (green0 - green1)
      + (blue0 - blue1) * (blue0 - blue1);
  }
}
