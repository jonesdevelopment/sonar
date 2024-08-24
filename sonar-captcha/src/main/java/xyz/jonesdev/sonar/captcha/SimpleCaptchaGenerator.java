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

package xyz.jonesdev.sonar.captcha;

import com.jhlabs.image.FBMFilter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.captcha.CaptchaGenerator;
import xyz.jonesdev.sonar.captcha.filters.CurvesOverlayFilter;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;

@Getter
@RequiredArgsConstructor
public final class SimpleCaptchaGenerator implements CaptchaGenerator {
  private static final CurvesOverlayFilter CURVES = new CurvesOverlayFilter(3);
  private static final FBMFilter FBM = new FBMFilter();

  static {
    FBM.setAmount(0.6f);
    FBM.setScale(15);
  }

  private static final String[] FONT_NAMES = {"RUBBERSTAMP", "DSnetStamped"};
  private static final Font[] FONTS = new Font[FONT_NAMES.length];
  private static final int FONT_SIZE = 60;

  static {
    for (int i = 0; i < FONT_NAMES.length; i++) {
      FONTS[i] = loadFont(String.format("/assets/fonts/%s.ttf",
        FONT_NAMES[new Random().nextInt(FONT_NAMES.length)]));
    }
  }

  private final int width, height;
  private final Random random;

  @Override
  public @NotNull BufferedImage createImage(final char[] answer) {
    final BufferedImage image = new BufferedImage(width, height, TYPE_3BYTE_BGR);
    // Fill the entire image with a noise texture
    FBM.filter(image, image);
    // Get the background image and create a new foreground image
    final Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // Draw characters and other effects on the image
    drawCharacters(graphics, answer);
    CURVES.transform(image, graphics);
    // Make sure to dispose the graphics instance
    graphics.dispose();
    return image;
  }

  private void drawCharacters(final @NotNull Graphics2D graphics,
                              final char @NotNull [] answer) {
    // Apply the gradient
    final Color color0 = Color.getHSBColor(random.nextFloat(), 1, 1);
    final Color color1 = Color.getHSBColor(random.nextFloat(), 1, 1);
    final Paint gradient = new GradientPaint(0, 0, color0, width, height, color1);
    graphics.setPaint(gradient);

    final FontRenderContext ctx = graphics.getFontRenderContext();
    final List<GlyphVector> glyphs = new ArrayList<>(answer.length);

    for (final char character : answer) {
      // Create a glyph vector for the character
      final Font font = FONTS[random.nextInt(FONTS.length)];
      final GlyphVector glyph = font.createGlyphVector(ctx, String.valueOf(character));
      glyphs.add(glyph);
    }

    // Calculate first X position
    final double totalWidth = glyphs.stream()
      .mapToDouble(glyph -> glyph.getLogicalBounds().getWidth() - 1)
      .sum();
    double beginX = Math.max(Math.min(width / 2D - totalWidth / 2D, totalWidth), 0);
    double beginY = (height + FONT_SIZE / 2D) / 2D;

    // Draw each glyph one by one
    for (final GlyphVector glyph : glyphs) {
      final AffineTransform transformation = AffineTransform.getTranslateInstance(beginX, beginY);
      // Rotate the glyph by a random amount
      transformation.rotate(Math.toRadians(-5 + random.nextInt(10)));
      // Draw the glyph to the buffered image
      final Shape transformedShape = transformation.createTransformedShape(glyph.getOutline());
      graphics.fill(transformedShape);
      // Make sure the next character isn't drawn at the same position
      beginX += glyph.getVisualBounds().getWidth() + 2;
      beginY += Math.sin(beginX / 3) * 6;
    }
  }

  private static Font loadFont(final @NotNull String path) {
    try {
      // Load the font from the TTF file
      final Font customFont = Font.createFont(Font.TRUETYPE_FONT,
        Objects.requireNonNull(SimpleCaptchaGenerator.class.getResourceAsStream(path)));
      // Set the font size and style
      return customFont.deriveFont(Font.PLAIN, FONT_SIZE);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}
