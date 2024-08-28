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
import java.awt.image.BufferedImage;
import java.util.Random;

@Getter
@RequiredArgsConstructor
public final class GridOverlayFilter {
  private final int amount, lineWidth, randomOffset;

  private static final Random RANDOM = new Random();

  public void transform(final @NotNull BufferedImage image,
                        final @NotNull Graphics2D graphics) {
    final int spacingX = image.getWidth() / amount + lineWidth;
    final int spacingY = image.getHeight() / amount + lineWidth;
    int currentX = spacingX / 2;
    int currentY = spacingY / 2;

    for (int i = 0; i < amount; i++) {
      graphics.setStroke(new BasicStroke(lineWidth * 0.5f + lineWidth * RANDOM.nextFloat()));
      graphics.drawLine(currentX, 0, currentX + RANDOM.nextInt(randomOffset), image.getHeight());
      graphics.drawLine(0, currentY, image.getWidth(), currentY + RANDOM.nextInt(randomOffset));

      currentX += spacingX + lineWidth;
      currentY += spacingY + lineWidth;
    }
  }
}
