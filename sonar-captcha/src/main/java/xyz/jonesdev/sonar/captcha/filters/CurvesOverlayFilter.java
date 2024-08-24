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

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.image.BufferedImage;
import java.util.Random;

@Getter
@RequiredArgsConstructor
public final class CurvesOverlayFilter {
  private final int amount;

  private static final Random RANDOM = new Random();

  public void transform(final @NotNull BufferedImage image,
                        final @NotNull Graphics2D graphics) {
    // Randomize the stroke width
    graphics.setStroke(new BasicStroke(1 + RANDOM.nextFloat()));

    final int halfWidth = image.getWidth() / 2;

    for (int i = 0; i < amount; ++i) {
      final float randomX = image.getWidth() * RANDOM.nextFloat();
      final float randomY = image.getHeight() * RANDOM.nextFloat();
      final float amplitude = 6.2831855f * (RANDOM.nextFloat() - 0.5f);
      final float sin = (float) Math.sin(amplitude) * halfWidth;
      final float cos = (float) Math.cos(amplitude) * halfWidth;
      final float x1 = randomX - cos;
      final float y1 = randomY - sin;
      final float x2 = randomX + cos;
      final float y2 = randomY + sin;
      // Control points for the cubic curve
      final float ctrlX1 = randomX + sin / 2;
      final float ctrlY1 = randomY - cos / 2;
      final float ctrlX2 = randomX - sin / 2;
      final float ctrlY2 = randomY + cos / 2;
      // Draw a cubic curve instead of a straight line
      graphics.draw(new CubicCurve2D.Float(x1, y1, ctrlX1, ctrlY1, ctrlX2, ctrlY2, x2, y2));
    }
  }
}
