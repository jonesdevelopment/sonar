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

  private final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
  private final Font CODE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 43);

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

      // Please enter captcha
      graphics.setFont(TITLE_FONT);
      graphics.drawString("Your code:", 5, 20);

      // Captcha
      graphics.setFont(CODE_FONT);

      final char[] captcha = new char[4];
      for (int _i = 0; _i < captcha.length; _i++) {
        captcha[_i] = dictionary.charAt(RANDOM.nextInt(dictionary.length()));
      }
      final String answer = new String(captcha);

      // Center captcha string
      final int stringWidth = graphics.getFontMetrics().stringWidth(answer);
      graphics.drawString(answer, image.getWidth() / 2 - stringWidth / 2, image.getHeight() / 2 + 21);

      // Store image in buffer
      final byte[] buffer = new byte[MapInfo.SCALE];
      for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
          final int colorIndex = y * image.getWidth() + x;
          final int pixel = image.getRGB(x, y);
          if (pixel != -16777216) {
            buffer[colorIndex] = (byte) 47;
          }
        }
      }

      cached[i] = new MapInfo(answer, image.getWidth(), image.getHeight(), 0, 0, buffer);
    }
  }

  public MapInfo getRandomCaptcha() {
    return cached[RANDOM.nextInt(cached.length)];
  }
}
