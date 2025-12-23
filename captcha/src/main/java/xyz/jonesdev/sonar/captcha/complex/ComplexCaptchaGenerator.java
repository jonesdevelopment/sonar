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

package xyz.jonesdev.sonar.captcha.complex;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.captcha.CaptchaGenerator;
import xyz.jonesdev.sonar.captcha.TTFFontProvider;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

@RequiredArgsConstructor
public final class ComplexCaptchaGenerator implements CaptchaGenerator {
  private final File background;
  private final int width = 128, height = 128;
  private BufferedImage backgroundImage;

  private static final Font[] FONTS = {
    TTFFontProvider.loadFont("/assets/fonts/dotness.ttf"), // by http://bythebutterfly.com/
    TTFFontProvider.loadFont("/assets/fonts/DotMatrix.ttf"),
  };
  private static final Random RANDOM = new SecureRandom();
  private static final double MAX_ROTATION_RADIANS = Math.toRadians(15);
  private static final int LINE_COUNT = 8;
  private static final int MIN_LINE_COLOR = 100;
  private static final int MAX_TEXT_COLOR = 150;
  private static final int DOT_COUNT = 100;

  private @NotNull BufferedImage createBackgroundImage() {
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    final Graphics2D graphics = image.createGraphics();
    if (background == null) {
      graphics.setBackground(Color.WHITE);
      graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
      return image;
    }
    if (backgroundImage == null) {
      try {
        backgroundImage = ImageIO.read(background);
      } catch (IOException exception) {
        throw new IllegalStateException("Could not read background image", exception);
      }
    }
    graphics.drawImage(backgroundImage, 0, 0, Color.WHITE, null);
    return image;
  }

  @Override
  public @NotNull BufferedImage createImage(char @NotNull [] answer) {
    final BufferedImage image = createBackgroundImage();
    final Graphics2D graphics = image.createGraphics();

    final String text = new String(answer);
    final int padding = 12;
    final double maxWidth = width - 2D * padding;
    final double maxHeight = height - 2D * padding;

    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    final FontRenderContext frc = graphics.getFontRenderContext();
    float optimalSize = 24;
    final float trialSize = 100;

    final Rectangle2D trialBounds = FONTS[0].deriveFont(trialSize).getStringBounds(text, frc);

    if (trialBounds.getWidth() > 0 && trialBounds.getHeight() > 0) {
      final double scaleFactorWidth = maxWidth / trialBounds.getWidth();
      final double scaleFactorHeight = maxHeight / trialBounds.getHeight();
      optimalSize = (float) (trialSize * Math.min(scaleFactorWidth, scaleFactorHeight));
    }

    if (optimalSize < 1f) {
      optimalSize = 1f;
    }

    final Font finalFont = FONTS[0].deriveFont(optimalSize);
    graphics.setFont(finalFont);

    final Rectangle2D fullBounds = finalFont.getStringBounds(text, frc);

    double currentX = (width - fullBounds.getWidth()) / 2D;

    final double baselineY = (height / 2D) - (fullBounds.getHeight() / 2D) - fullBounds.getY();
    double currentY = baselineY;

    for (final char c : answer) {
      final Font font = FONTS[RANDOM.nextInt(FONTS.length)].deriveFont(optimalSize);
      graphics.setFont(font);

      final String character = String.valueOf(c);
      final Rectangle2D charBounds = font.getStringBounds(character, frc);

      final double charWidth = charBounds.getWidth();
      final double charHeight = charBounds.getHeight();
      final double charCenterX = currentX + charWidth / 2D;
      final double charCenterY = baselineY + charBounds.getY() + charHeight / 2D;

      final Color[] colors = new Color[2 + RANDOM.nextInt(2)];
      final float[] colorFractions = new float[colors.length];
      for (int i = 0; i < colors.length; i++) {
        colors[i] = getRandomTextColor();
        colorFractions[i] = i / (float) colors.length;
      }

      graphics.setPaint(new RadialGradientPaint((float) currentX, (float) currentY, (float) charWidth,
        colorFractions, colors, MultipleGradientPaint.CycleMethod.REFLECT));

      final double rotation = (RANDOM.nextDouble() * 2 * MAX_ROTATION_RADIANS) - MAX_ROTATION_RADIANS;

      final AffineTransform originalTransform = graphics.getTransform();

      graphics.rotate(rotation, charCenterX, charCenterY);

      final TextLayout layout = new TextLayout(character, font, frc);

      final AffineTransform translate = AffineTransform.getTranslateInstance(currentX, currentY);
      final Shape outline = layout.getOutline(translate);
      graphics.fill(outline);
      graphics.setTransform(originalTransform);

      currentX += charWidth;
      currentY += RANDOM.nextDouble(charHeight / 4) - charHeight / 8;
    }

    drawNoiseLines(graphics);
    drawNoiseDots(graphics);
    graphics.dispose();
    return image;
  }

  private @NotNull Color getRandomTextColor() {
    final int r = Math.min(RANDOM.nextInt(256), MAX_TEXT_COLOR);
    final int g = Math.min(RANDOM.nextInt(256), MAX_TEXT_COLOR);
    final int b = Math.min(RANDOM.nextInt(256), MAX_TEXT_COLOR);
    return new Color(r, g, b, 220);
  }

  private @NotNull Color getRandomColor() {
    final int r = Math.max(RANDOM.nextInt(256), MIN_LINE_COLOR);
    final int g = Math.max(RANDOM.nextInt(256), MIN_LINE_COLOR);
    final int b = Math.max(RANDOM.nextInt(256), MIN_LINE_COLOR);
    final int a = Math.max(Math.min(RANDOM.nextInt(256), 200), MIN_LINE_COLOR);
    return new Color(r, g, b, a);
  }

  private void drawNoiseLines(final @NotNull Graphics2D graphics) {
    for (int i = 0; i < LINE_COUNT; i++) {
      graphics.setColor(getRandomColor());
      graphics.setStroke(new BasicStroke(1f + RANDOM.nextFloat() * 2f,
        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      if (RANDOM.nextBoolean()) {
        float x1 = RANDOM.nextInt(width);
        float y1 = RANDOM.nextInt(height);
        float ctrlX = RANDOM.nextInt(width);
        float ctrlY = RANDOM.nextInt(height);
        float x2 = RANDOM.nextInt(width);
        float y2 = RANDOM.nextInt(height);

        graphics.draw(new QuadCurve2D.Float(x1, y1, ctrlX, ctrlY, x2, y2));
      } else {
        int x1 = RANDOM.nextInt(width);
        int y1 = RANDOM.nextInt(height);
        int x2 = RANDOM.nextInt(width);
        int y2 = RANDOM.nextInt(height);

        graphics.drawLine(x1, y1, x2, y2);
      }
    }
  }

  private void drawNoiseDots(final @NotNull Graphics2D graphics) {
    for (int i = 0; i < DOT_COUNT; i++) {
      graphics.setColor(getRandomColor());

      final int x = RANDOM.nextInt(width);
      final int y = RANDOM.nextInt(height);
      final int radius = 1 + RANDOM.nextInt(5);

      graphics.fillOval(x, y, radius, radius);
    }
  }
}
