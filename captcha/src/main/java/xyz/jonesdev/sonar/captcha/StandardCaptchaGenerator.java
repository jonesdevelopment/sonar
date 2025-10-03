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

package xyz.jonesdev.sonar.captcha;

import com.jhlabs.image.FBMFilter;
import com.jhlabs.image.SmearFilter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.fallback.captcha.CaptchaGenerator;
import xyz.jonesdev.sonar.captcha.filters.CurvesOverlayFilter;
import xyz.jonesdev.sonar.captcha.filters.NoiseOverlayFilter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

@Getter
@RequiredArgsConstructor
public final class StandardCaptchaGenerator implements CaptchaGenerator {
  private static final CurvesOverlayFilter CURVES = new CurvesOverlayFilter(3);
  //private static final CircleInverseFilter CIRCLES = new CircleInverseFilter(1, 30, 10);
  private static final NoiseOverlayFilter NOISE = new NoiseOverlayFilter(1, 20);
  private static final SmearFilter SMEAR = new SmearFilter();
  private static final FBMFilter FBM = new FBMFilter();
  private static final Random RANDOM = new Random();
  private static final Color[] COLORS = new Color[4];
  private static final float[] COLOR_FRACTIONS = new float[COLORS.length];

  static {
    FBM.setAmount(0.6f);
    FBM.setScale(15);

    SMEAR.setShape(SmearFilter.SQUARES);
    SMEAR.setDensity(0.1f);
    SMEAR.setDistance(0);
    SMEAR.setMix(0.35f);

    // Create fractions based on the number of colors
    for (int i = 0; i < COLOR_FRACTIONS.length; i++) {
      COLOR_FRACTIONS[i] = (float) i / (COLOR_FRACTIONS.length - 1);
    }
  }

  private final int width = 128, height = 128;
  private final @Nullable File background;
  private @Nullable BufferedImage backgroundImage;

  @Override
  public @NotNull BufferedImage createImage(final char @NotNull [] answer) {
    final BufferedImage image = createBackgroundImage();
    final Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // Draw characters and other effects on the image
    applyRandomColorGradient(graphics);
    drawCharacters(graphics, answer);
    CURVES.transform(image, graphics);
    NOISE.transform(image);
    //SMEAR.filter(image, image);
    //CIRCLES.transform(image); // TODO: check if the text is still easy to read after this
    // Make sure to dispose the graphics instance
    graphics.dispose();
    return image;
  }

  private @NotNull BufferedImage createBackgroundImage() {
    // Create a new image buffer with a 3-byte color spectrum
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    if (background == null) {
      // Fill the entire image with a noise texture
      return FBM.filter(image, image);
    }
    // Try to load the image from a file if it doesn't exist yet
    if (backgroundImage == null) {
      try {
        backgroundImage = ImageIO.read(background);
      } catch (IOException exception) {
        throw new IllegalStateException("Could not read background image", exception);
      }
    }
    image.createGraphics().drawImage(backgroundImage, 0, 0, Color.WHITE, null);
    return image;
  }

  private void applyRandomColorGradient(final @NotNull Graphics2D graphics) {
    // Randomize the colors for the gradient effect
    for (int i = 0; i < COLORS.length; i++) {
      final float random = 0.9f + RANDOM.nextFloat() * 0.1f;
      COLORS[i] = Color.getHSBColor(RANDOM.nextFloat(), random * 0.8f, random);
    }

    // Apply the random gradient effect
    graphics.setPaint(new RadialGradientPaint(0, 0, width,
      COLOR_FRACTIONS, COLORS, MultipleGradientPaint.CycleMethod.REFLECT));
  }

  private void drawCharacters(final @NotNull Graphics2D graphics,
                              final char @NotNull [] answer) {
    final FontRenderContext ctx = graphics.getFontRenderContext();
    final GlyphVector[] glyphs = new GlyphVector[answer.length];

    for (int i = 0; i < answer.length; i++) {
      // Create a glyph vector for the character with a random font
      final Font font = StandardTTFFontProvider.FONTS[RANDOM.nextInt(StandardTTFFontProvider.FONTS.length)];
      glyphs[i] = font.createGlyphVector(ctx, String.valueOf(answer[i]));
    }

    final double scalingXY = 5 - Math.min(answer.length, 5) * 0.65;

    // Calculate first X and Y positions
    final double totalWidth = Arrays.stream(glyphs)
      .mapToDouble(glyph -> glyph.getLogicalBounds().getWidth() * scalingXY - 1)
      .sum();
    double beginX = Math.max(Math.min(width / 2D - totalWidth / 2D, totalWidth), 0);
    double beginY = (height + StandardTTFFontProvider.STANDARD_FONT_SIZE / 2D) / 2D + scalingXY;

    // Draw each glyph one by one
    for (final GlyphVector glyph : glyphs) {
      final AffineTransform transformation = AffineTransform.getTranslateInstance(beginX, beginY);
      // Shear the glyph by a random amount
      final double shearXY = Math.sin(beginX + beginY) / 6;
      transformation.shear(shearXY, shearXY);
      // Scale the glyph to perfectly fit the image
      transformation.scale(scalingXY, scalingXY);
      // Draw the glyph to the buffered image
      final Shape transformedShape = transformation.createTransformedShape(glyph.getOutline());
      graphics.fill(transformedShape);
      // Draw a random outline around the glyph
      if (RANDOM.nextFloat() < 0.25f) {
        createGlyphOutline(graphics, transformedShape);
      }
      // Make sure the next glyph isn't drawn at the same position
      beginX += glyph.getVisualBounds().getWidth() * scalingXY + 2;
      beginY += -10 + RANDOM.nextFloat() * 20;
    }
  }

  private void createGlyphOutline(final @NotNull Graphics2D graphics, final @NotNull Shape shape) {
    final float txy = 1.25f + RANDOM.nextFloat();
    final float width = 1 + RANDOM.nextFloat();

    // Create a randomly translated and stroked shape based on the original glyph shape
    final AffineTransform translation = AffineTransform.getTranslateInstance(txy, txy);
    final Shape strokedShape = new BasicStroke(width).createStrokedShape(shape);

    graphics.fill(translation.createTransformedShape(strokedShape));
  }
}
