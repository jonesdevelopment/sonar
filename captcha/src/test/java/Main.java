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

import lombok.experimental.UtilityClass;
import xyz.jonesdev.sonar.api.fallback.captcha.CaptchaGenerator;
import xyz.jonesdev.sonar.captcha.complex.ComplexCaptchaGenerator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

@UtilityClass
public class Main {
  public void main(final String... args) throws IOException {
    final Random random = new Random();
    final CaptchaGenerator standardCaptchaGenerator = new ComplexCaptchaGenerator(null);

    final long start = System.currentTimeMillis();
    final char[] dictionary = {'a', 'b', 'c', 'd', 'e', 'f'/*, 'g'*/, 'h'/*, 'i'*/, 'j',
      'k', 'm', 'n', 'o', 'p'/*, 'q'*/, 'r', 's', 't', 'u'/*, 'v', 'w'*/, 'x', 'y', 'z'};

    // Generate image
    final int amount = 1;
    for (int i = 0; i < amount; i++) {
      final char[] answer = new char[3 + random.nextInt(2)];
      for (int j = 0; j < answer.length; j++) {
        answer[j] = dictionary[random.nextInt(dictionary.length)];
      }
      final BufferedImage bufferedImage = standardCaptchaGenerator.createImage(answer);

      // Save image
      ImageIO.write(bufferedImage, "png", new File(i + ".png"));
    }
    System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to generate " + amount + " image(s)");
  }
}
