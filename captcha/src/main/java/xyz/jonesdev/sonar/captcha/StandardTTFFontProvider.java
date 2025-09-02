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

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.InputStream;
import java.util.Objects;

@UtilityClass
class StandardTTFFontProvider {
  private static final String[] FONT_NAMES = {"Kingthings_Trypewriter_2"};
  static final Font[] FONTS = new Font[FONT_NAMES.length];
  static final int STANDARD_FONT_SIZE = 25;

  static {
    for (int i = 0; i < FONT_NAMES.length; i++) {
      FONTS[i] = loadFont(String.format("/assets/fonts/%s.ttf", FONT_NAMES[i]));
    }
  }

  private static Font loadFont(final @NotNull String path) {
    try (final InputStream inputStream = StandardCaptchaGenerator.class.getResourceAsStream(path)) {
      // Load the font from the TTF file
      final Font customFont = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(inputStream));
      // Set the font size and style
      return customFont.deriveFont(Font.PLAIN, STANDARD_FONT_SIZE);
    } catch (Exception exception) {
      throw new IllegalStateException("Error loading font. Does your environment support fonts?", exception);
    }
  }
}
