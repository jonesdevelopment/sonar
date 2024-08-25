/*
 * Copyright (C) 2024 Sonar Contributors
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

package xyz.jonesdev.sonar.captcha.filters;

import com.jhlabs.image.ImageMath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.util.Random;

@Getter
@RequiredArgsConstructor
public final class SmearOverlayFilter {
  private final float density, mix = 0.5f;
  private final int distance;

  private static final Random RANDOM = new Random();

  public void transform(final @NotNull BufferedImage image) {
    int radius = distance + 1;
    int radius2 = radius * radius;
    final int numShapes = (int) (2 * density * image.getWidth() * image.getHeight() / radius);
    for (int i = 0; i < numShapes; i++) {
      int sx = (RANDOM.nextInt() & 0x7fffffff) % image.getWidth();
      int sy = (RANDOM.nextInt() & 0x7fffffff) % image.getHeight();
      int rgb = image.getRGB(sx, sy);
      for (int x = sx - radius; x < sx + radius + 1; x++) {
        for (int y = sy - radius; y < sy + radius + 1; y++) {
          int f = 0;
          if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight() && f <= radius2) {
            int rgb2 = image.getRGB(x, y);
            image.setRGB(x, y, ImageMath.mixColors(mix, rgb2, rgb));
          }
        }
      }
    }
  }
}
