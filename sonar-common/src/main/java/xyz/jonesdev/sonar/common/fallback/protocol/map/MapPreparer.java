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

import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.Sonar;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

@UtilityClass
public class MapPreparer {
  private final Random RANDOM = new Random();

  private final String[] FONT_TYPES = new String[] {
    Font.DIALOG_INPUT,
    Font.DIALOG,
    Font.SANS_SERIF,
    Font.SERIF
  };

  private final int[] FONT_STYLES = new int[] {
    Font.PLAIN,
    Font.BOLD,
    Font.ITALIC,
    Font.ITALIC | Font.BOLD
  };

  private final int[][] COLOR_PALETTE = new int[][] {
    new int[] { // Blue
      48,
      49,
      50,
      51
    },
    new int[] { // Gray
      45,
      46,
      47,
    }
  };

  private MapInfo[] cached;

  public void prepare() {
    cached = new MapInfo[Sonar.get().getConfig().getVerification().getMap().getPrecomputeAmount()];

    final String dictionary = Sonar.get().getConfig().getVerification().getMap().getDictionary();

    for (int i = 0; i < cached.length; i++) {
      // Send map data
      final BufferedImage image = new BufferedImage(MapInfo.DIMENSIONS, MapInfo.DIMENSIONS, BufferedImage.TYPE_INT_RGB);
      final Graphics2D graphics = image.createGraphics();

      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setColor(Color.WHITE);

      final String fontType = FONT_TYPES[RANDOM.nextInt(FONT_TYPES.length)];
      final int fontStyle = FONT_STYLES[RANDOM.nextInt(FONT_STYLES.length)];
      final int fontSize = 32 + RANDOM.nextInt(10);
      @SuppressWarnings("all")
      final Font answerFont = new Font(fontType, fontStyle, fontSize);
      graphics.setFont(answerFont);

      final char[] captcha = new char[4];
      for (int _i = 0; _i < captcha.length; _i++) {
        captcha[_i] = dictionary.charAt(RANDOM.nextInt(dictionary.length()));
      }
      final String answer = new String(captcha);

      // Calculate text position
      final int stringWidth = graphics.getFontMetrics().stringWidth(answer);
      int _x = image.getWidth() / 2 - stringWidth / 2;
      int _y = image.getHeight() / 2 + fontSize / 3;

      // Draw each character one by one
      for (final char c : captcha) {
        final String character = String.valueOf(c);
        final int randomOffsetX = -3 + RANDOM.nextInt(6);
        final int randomOffsetY = -7 + RANDOM.nextInt(14);

        graphics.drawString(character, _x, _y);
        _x += graphics.getFontMetrics().stringWidth(character);
        _x += randomOffsetX;
        _y += randomOffsetY;
      }

      // Select random color palette
      final int randomColorPalette = RANDOM.nextInt(COLOR_PALETTE.length);
      // Store image in buffer
      final byte[] buffer = new byte[MapInfo.SCALE];
      for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
          final int colorIndex = y * image.getWidth() + x;
          final int pixel = image.getRGB(x, y);
          if (pixel == -16777216) continue;
          final int randomColor = COLOR_PALETTE[randomColorPalette][RANDOM.nextInt(COLOR_PALETTE[randomColorPalette].length)];
          buffer[colorIndex] = (byte) randomColor;
        }
      }

      cached[i] = new MapInfo(answer, image.getWidth(), image.getHeight(), 0, 0, buffer);
    }
  }

  public MapInfo getRandomCaptcha() {
    return cached[RANDOM.nextInt(cached.length)];
  }
}
