/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.protocol.map;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.awt.Font.*;
import static xyz.jonesdev.sonar.common.fallback.protocol.map.PreparedMapInfo.DIMENSIONS;

@UtilityClass
public class MapInfoPreparer {
  private final Random RANDOM = new Random();

  private final ExecutorService PREPARATION_SERVICE = Executors.newSingleThreadExecutor();

  private final int[] FONT_STYLES = new int[]{
    PLAIN,
    BOLD,
    ITALIC,
    ITALIC | BOLD
  };

  private final int[][] COLOR_PALETTE = new int[][]{
    new int[]{ // Blue
      48,
      49,
      50,
      51
    },
    new int[]{ // Orange
      60,
      61,
      62,
      63,
    },
    new int[]{ // Green
      4,
      28,
      29,
      30,
    },
    new int[]{ // Black
      27,
      45,
      46,
      47,
    },
    new int[]{ // Red
      16,
      17,
      18,
      19
    }
  };

  private PreparedMapInfo[] cached;
  @Getter
  private int preparedCAPTCHAs;
  private boolean currentlyPreparing;

  public PreparedMapInfo getRandomCaptcha() {
    return cached[RANDOM.nextInt(preparedCAPTCHAs)];
  }

  public void prepare() {
    final SystemTimer timer = new SystemTimer();
    Sonar.get().getLogger().info("Precomputing map captcha answers...");

    // Make sure we're not running into concurrency issues
    if (currentlyPreparing) {
      Sonar.get().getLogger().warn("Did not precompute map captcha answers as another task is running.");
      return;
    }

    // Reset all statistics
    preparedCAPTCHAs = 0;
    currentlyPreparing = true;
    final SonarConfiguration.Verification.Map config = Sonar.get().getConfig().getVerification().getMap();
    cached = null;
    cached = new PreparedMapInfo[config.getPrecomputeAmount()];

    // Prepare fonts from config
    final String dictionary = config.getDictionary();
    final List<String> fonts = config.getFonts();
    if (fonts.isEmpty()) {
      Sonar.get().getLogger().warn("No fonts found, using fallback font...");
      fonts.add(DIALOG);
    }
    final String[] fontTypes = fonts.toArray(new String[0]);

    // Prepare distortions values from config
    final double distortionsFactorX = config.getDistortionsFactorX();
    final double distortionsFactorY = config.getDistortionsFactorY();
    final double halfDistortionsFactorX = distortionsFactorX / 2D;
    final double halfDistortionsFactorY = distortionsFactorY / 2D;

    for (int _i = 0; _i < cached.length; _i++) {
      final int currentIndex = _i;
      final byte[] buffer = new byte[PreparedMapInfo.SCALE];
      PREPARATION_SERVICE.execute(() -> {
        // Create image
        final BufferedImage image = new BufferedImage(DIMENSIONS, DIMENSIONS, BufferedImage.TYPE_3BYTE_BGR);
        final Graphics2D graphics = (Graphics2D) image.getGraphics();
        try {
          // Create random font
          final String fontType = fontTypes[RANDOM.nextInt(fontTypes.length)];
          final int fontStyle = FONT_STYLES[RANDOM.nextInt(FONT_STYLES.length)];
          final int fontSize = 30
            + (config.isRandomizeFontSize()
            ? RANDOM.nextInt(7) - 3 : 3);
          @SuppressWarnings("all") final Font answerFont = new Font(fontType, fontStyle, fontSize);
          graphics.setFont(answerFont);

          // Build answer to the captcha
          final StringBuilder answerBuilder = new StringBuilder();
          for (int _j = 0; _j < 5; _j++) {
            answerBuilder.append(dictionary.charAt(RANDOM.nextInt(dictionary.length())));
          }
          final String answer = answerBuilder.toString();

          // Calculate text position
          final int stringWidth = graphics.getFontMetrics().stringWidth(answer);
          final int halfWidth = image.getWidth() / 2;
          final int halfHeight = image.getHeight() / 2;
          final int spacing = 5;
          double _x = halfWidth - stringWidth / 2f - spacing;
          double _y = halfHeight + fontSize / 3f;

          // Draw each character one by one
          final FontRenderContext fontRenderContext = graphics.getFontRenderContext();
          for (final char c : answer.toCharArray()) {
            // Randomize x and y
            if (config.isRandomizePositions()) {
              _x += RANDOM.nextInt(2) - 1;
              _y += RANDOM.nextInt(8) - 4;
            }

            // Apply random distortion
            final double distortionFactorX = RANDOM.nextDouble() * distortionsFactorX - halfDistortionsFactorX;
            final double distortionFactorY = RANDOM.nextDouble() * distortionsFactorY - halfDistortionsFactorY;

            // Create a GlyphVector for the character
            final String character = String.valueOf(c);
            final GlyphVector glyphVector = graphics.getFont().createGlyphVector(fontRenderContext, character);

            // Apply the distortion to the GlyphVector using AffineTransform
            final AffineTransform transform = AffineTransform.getTranslateInstance(_x, _y);
            transform.shear(distortionFactorX, distortionFactorY);
            final Shape distortedCharacter = glyphVector.getOutline();
            final Shape distorted = transform.createTransformedShape(distortedCharacter);

            // Draw the distorted character
            graphics.fill(distorted);

            // Update x by width
            _x += glyphVector.getVisualBounds().getWidth() + spacing;
          }

          // TODO: Color converter
          graphics.setColor(Color.RED);

          // Draw random lines
          for (int i = 0; i < config.getRandomLinesAmount(); i++) {
            final int startX = RANDOM.nextInt(halfWidth);
            final int startY = RANDOM.nextInt(halfHeight);
            final int endX = halfWidth + RANDOM.nextInt(halfWidth);
            final int endY = halfHeight + RANDOM.nextInt(halfHeight);

            graphics.drawLine(startX, startY, endX, endY);
          }

          // Draw random ovals
          for (int i = 0; i < config.getRandomOvalsAmount(); i++) {
            final int startX = RANDOM.nextInt(halfWidth);
            final int startY = RANDOM.nextInt(halfHeight);
            final int endX = halfWidth + RANDOM.nextInt(halfWidth);
            final int endY = halfHeight + RANDOM.nextInt(halfHeight);

            graphics.drawOval(startX, startY, endX, endY);
          }

          // Select random color palette
          final int[] colorPalette = COLOR_PALETTE[currentIndex % COLOR_PALETTE.length];
          // Select the next color palette for geometry
          final int[] nextColorPalette = COLOR_PALETTE[(currentIndex + 1) % COLOR_PALETTE.length];
          // Clear background
          Arrays.fill(buffer, (byte) 57);
          // Color every pixel individually
          final int spacingY = halfHeight - fontSize;
          for (int x = spacing; x < image.getWidth() - spacing; x++) {
            for (int y = spacingY; y < image.getHeight() - spacingY; y++) {
              final int pixel = image.getRGB(x, y);
              final int index = y * image.getWidth() + x;
              if (pixel == -16777216 && RANDOM.nextInt(100) < 97) continue;
              // Set color of pixel to random color from the palette
              final byte color = (byte) (pixel == -16777216 ? RANDOM.nextInt(100) < 75 ? 14 : index
                : pixel != -65536 ? colorPalette[RANDOM.nextInt(colorPalette.length)]
                : nextColorPalette[RANDOM.nextInt(nextColorPalette.length)]);
              // Write pixel color to buffer
              buffer[index] = color;
            }
          }
          // Cache buffer to map
          cached[currentIndex] = new PreparedMapInfo(answer, image.getWidth(), image.getHeight(), buffer);
        } finally {
          // Dispose graphics
          graphics.dispose();
          // Finished?
          if (currentIndex == cached.length - 1) {
            Sonar.get().getLogger().info("Successfully precomputed map captcha answers in {}s!", timer);
            currentlyPreparing = false;
          }
          preparedCAPTCHAs++;
        }
      });
    }
  }
}
