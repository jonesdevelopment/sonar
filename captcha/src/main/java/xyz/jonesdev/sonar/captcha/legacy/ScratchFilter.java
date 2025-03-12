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

package xyz.jonesdev.sonar.captcha.legacy;

import com.jhlabs.image.AbstractBufferedImageOp;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

@RequiredArgsConstructor
public final class ScratchFilter extends AbstractBufferedImageOp {
  private static final Random RANDOM = new Random();

  private final int amount;

  @Override
  public @NotNull BufferedImage filter(final @NotNull BufferedImage src, final BufferedImage dst) {
    final Graphics2D graphics = src.createGraphics();

    // Apply some gradient effect on them
    final Color color0 = Color.getHSBColor(RANDOM.nextFloat(), RANDOM.nextFloat(), 1);
    final Color color1 = new Color(~color0.getRGB());
    final GradientPaint gradient = new GradientPaint(0, 0, color0, src.getWidth(), src.getHeight(), color1);
    graphics.setPaint(gradient);

    final float halfWidth = src.getWidth() * 0.5f;

    for (int i = 0; i < amount; ++i) {
      final float randomX = src.getWidth() * RANDOM.nextFloat();
      final float randomY = src.getHeight() * RANDOM.nextFloat();
      final float amplitude = 6.2831855f * (RANDOM.nextFloat() - 0.5f);
      final float sin = (float) Math.sin(amplitude) * halfWidth;
      final float cos = (float) Math.cos(amplitude) * halfWidth;
      final float x1 = randomX - cos;
      final float y1 = randomY - sin;
      final float x2 = randomX + cos;
      final float y2 = randomY + sin;
      graphics.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
    }

    graphics.dispose();
    return src;
  }
}
