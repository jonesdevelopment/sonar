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

package xyz.jonesdev.sonar.captcha.filters;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

@Getter
@RequiredArgsConstructor
public final class CircleInverseFilter {
  private final int amount, minRadius, maxRadiusExpansion;

  private static final Random RANDOM = new Random();

  public void transform(final @NotNull BufferedImage image) {
    final int minX = image.getWidth() / 4;
    final int minY = image.getHeight() / 4;

    for (int i = 0; i < amount; i++) {
      // Generate random position and radius for the circle
      final float centerX = minX + RANDOM.nextInt(image.getWidth() - minX * 2);
      final float centerY = minY + RANDOM.nextInt(image.getHeight() - minY * 2);
      final int radius = minRadius + RANDOM.nextInt(maxRadiusExpansion);

      final Circle circle = new Circle(centerX, centerY, radius);
      // Draw inverted circle
      _addInvertedCircle(image, circle);
    }
  }

  private static void _addInvertedCircle(final @NotNull BufferedImage bufferedImage,
                                         final @NotNull Circle circle) {
    for (int x = 0; x < bufferedImage.getWidth(); x++) {
      for (int y = 0; y < bufferedImage.getHeight(); y++) {
        if (isWithinCircle(x, y, circle, circle.radius)) {
          // Invert colors within the circle
          final int rgb = bufferedImage.getRGB(x, y);
          final int invertedRGB = invertColorAndFilter(rgb);
          bufferedImage.setRGB(x, y, invertedRGB);
        }
      }
    }
  }

  private static boolean isWithinCircle(final float x, final float y, final @NotNull Circle circle, int radius) {
    final float distanceX = x - circle.centerX;
    final float distanceY = y - circle.centerY;
    return distanceX * distanceX + distanceY * distanceY <= radius * radius;
  }

  @Getter
  @RequiredArgsConstructor
  static final class Circle {
    private final float centerX;
    private final float centerY;
    private final int radius;
  }

  private static int invertColorAndFilter(final int rgb) {
    final Color color = new Color(rgb);
    final int red = Math.max(Color.LIGHT_GRAY.getRed() - color.getRed(), 0);
    final int green = Math.max(Color.LIGHT_GRAY.getGreen() - color.getGreen(), 0);
    final int blue = Math.max(Color.LIGHT_GRAY.getBlue() - color.getBlue(), 0);
    return new Color(red, green, blue).getRGB();
  }
}
