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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.util.Random;

@Getter
@RequiredArgsConstructor
public final class NoiseOverlayFilter {
  private final float density, amount;

  private static final Random RANDOM = new Random();

  private int randomGaussian(int x) {
    x += (int) (RANDOM.nextGaussian() * amount);
    return Math.max(Math.min(x, 0xff), 0);
  }

  public void transform(final @NotNull BufferedImage image) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        if (RANDOM.nextFloat() <= density) {
          final int rgb = image.getRGB(x, y);
          int r = (rgb >> 16) & 0xff;
          int g = (rgb >> 8) & 0xff;
          int b = rgb & 0xff;
          r = randomGaussian(r);
          g = randomGaussian(g);
          b = randomGaussian(b);
          image.setRGB(x, y, (r << 16) | (g << 8) | b);
        }
      }
    }
  }
}
