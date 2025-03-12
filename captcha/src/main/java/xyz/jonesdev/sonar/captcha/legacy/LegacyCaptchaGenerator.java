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

import com.jhlabs.image.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.fallback.captcha.CaptchaGenerator;
import xyz.jonesdev.sonar.captcha.filters.NoiseOverlayFilter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;

@Getter
@RequiredArgsConstructor
public final class LegacyCaptchaGenerator implements CaptchaGenerator {
  private static final ScratchFilter SCRATCHES = new ScratchFilter(4);
  private static final NoiseOverlayFilter NOISE = new NoiseOverlayFilter(1, 7);
  private static final SmearFilter SMEAR = new SmearFilter();
  private static final SaturationFilter SATURATION = new SaturationFilter();
  private static final RippleFilter RIPPLE = new RippleFilter();
  private static final Random RANDOM = new Random();

  static {
    RIPPLE.setXAmplitude(0);
    float yAmplitude = 10 - RANDOM.nextInt(20);
    if (Math.abs(yAmplitude) < 5) {
      yAmplitude = yAmplitude >= 0 ? 5 : -5;
    }
    RIPPLE.setYAmplitude(yAmplitude);

    SATURATION.setAmount(0.45f + RANDOM.nextFloat() * 0.15f);

    SMEAR.setShape(SmearFilter.CIRCLES);
    SMEAR.setMix(0.15f);
    SMEAR.setDensity(0.1f);
    SMEAR.setDistance(5);
  }

  private final int width = 128, height = 128;
  private final @Nullable File background;
  private @Nullable BufferedImage backgroundImage;
  private GradientPaint gradient;

  public BufferedImage createCachedBackgroundImage() {
    // Create a new image buffer with a 3-byte color spectrum
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    if (background == null) {
      if (backgroundImage != null) {
        return backgroundImage;
      }
      // Don't use any special background image
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      // Fill the entire background image with a noise texture
      image = new CausticsFilter().filter(image, null);
      // Adjust the saturation of the randomly generated background noise
      final SaturationFilter saturationFilter = new SaturationFilter();
      saturationFilter.setAmount(0.4f + RANDOM.nextFloat() * 0.2f);
      image = saturationFilter.filter(image, null);
      // Un-sharpen the background a bit
      image = new UnsharpFilter().filter(image, null);
      return image;
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

  private @NotNull BufferedImage mergeImages(final @NotNull BufferedImage background,
                                             final @NotNull BufferedImage foreground) {
    // Get the background image and create a new foreground image
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    // Create a new image with transparency for the merged result
    final Graphics2D graphics = image.createGraphics();
    // Draw the foreground image on top of the background at specified coordinates
    graphics.drawImage(background, 0, 0, null);
    // Set AlphaComposite to handle transparency for the foreground image
    graphics.setComposite(AlphaComposite.SrcOver);
    // Draw the foreground image on top of the background at specified coordinates
    graphics.drawImage(foreground, 0, 0, null);
    graphics.dispose();
    return image;
  }

  @Override
  public @NotNull BufferedImage createImage(final char @NotNull [] answer) {
    final Color color0 = Color.getHSBColor(RANDOM.nextFloat(), 1, 1);
    final Color color1 = Color.getHSBColor(RANDOM.nextFloat(), 1, 0.5f);
    gradient = new GradientPaint(0, 0, color0, width, height, color1);

    // Get the background image and create a new foreground image
    BufferedImage foreground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = foreground.createGraphics();

    final FontRenderContext ctx = graphics.getFontRenderContext();
    // Change some rendering hints for anti aliasing
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // Draw characters
    drawCharacters(graphics, ctx, answer);
    // Make sure to dispose the graphics after using it
    graphics.dispose();

    // Apply any given filter to the foreground
    new BumpFilter().filter(foreground, foreground);
    SMEAR.filter(foreground, foreground);
    RIPPLE.filter(foreground, foreground);
    SCRATCHES.filter(foreground, foreground);

    final BufferedImage merged = mergeImages(createCachedBackgroundImage(), foreground);
    NOISE.transform(merged);
    return merged;
  }

  private void drawCharacters(final @NotNull Graphics2D graphics,
                              final @NotNull FontRenderContext ctx,
                              final char[] answer) {
    // Create font render context
    final int fontSize = 62 - answer.length * 2;
    final Font font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);

    // Calculate string width
    final double stringWidth = font.getStringBounds(answer, 0, answer.length, ctx).getWidth();
    // Calculate character positions
    int beginX = (int) ((width - stringWidth) / 2D + stringWidth / (answer.length * 2.5D));
    final int beginY = (int) ((height + fontSize / 2D) / 2D);
    double rotation = 0;

    if (gradient != null) {
      graphics.setPaint(gradient);
    }

    // Draw each character one by one
    for (final char character : answer) {
      // Create a glyph vector for the character
      final GlyphVector glyphVector = font.createGlyphVector(ctx, String.valueOf(character));
      final int visualBoundsWidth = (int) glyphVector.getVisualBounds().getWidth();
      if (gradient == null) {
        Objects.requireNonNull(backgroundImage);
        // Use the inverse color by checking the background image
        // Then, create a gradient for the text by using the colors
        final int x0 = Math.min(Math.max(beginX + 5 /* small threshold */, 0), width);
        final int x1 = Math.min(Math.max(beginX + (int) visualBoundsWidth, 0), width);
        final int y = beginY + fontSize / 2;
        final Color color0 = new Color(~backgroundImage.getRGB(x0, y));
        final Color color1 = new Color(~backgroundImage.getRGB(x1, y));
        final GradientPaint gradient = new GradientPaint(0, 0, color0, width, height, color1);
        graphics.setPaint(gradient);
      }

      // Apply a transformation to the glyph vector using AffineTransform
      final AffineTransform transformation = AffineTransform.getTranslateInstance(beginX, beginY);

      // Add a bit of randomization to the rotation
      rotation += Math.toRadians(6 - RANDOM.nextInt(12));
      transformation.rotate(rotation);
      transformation.scale(1, 1.25);

      // Draw the glyph to the buffered image
      final Shape transformedShape = transformation.createTransformedShape(glyphVector.getOutline());
      graphics.fill(transformedShape);
      // Add text outline/shadow to confuse an AI's edge detection
      addTextOutline(graphics, transformedShape);
      // Update next X position
      beginX += visualBoundsWidth + 2;
    }
  }

  private static void addTextOutline(final @NotNull Graphics2D graphics,
                                     final @NotNull Shape transformedShape) {
    // Create a stroked copy of the text and slightly offset/distort it
    final Shape strokedShape = new BasicStroke().createStrokedShape(transformedShape);

    final double tx = 0.5 + RANDOM.nextDouble();
    final double ty = 0.5 + RANDOM.nextDouble();

    // Draw the stroked shape
    final AffineTransform transform = AffineTransform.getTranslateInstance(tx, ty);
    graphics.fill(transform.createTransformedShape(strokedShape));
  }
}
