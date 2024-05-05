/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.fallback.protocol.captcha;

import xyz.jonesdev.capja.CaptchaGenerator;
import xyz.jonesdev.capja.CaptchaHolder;
import xyz.jonesdev.capja.config.CaptchaConfiguration;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CaptchaPreparer {
  private static final ExecutorService PREPARATION_SERVICE = Executors.newSingleThreadExecutor();
  private static final Random RANDOM = new Random();

  private MapCaptchaInfo[] cached;
  private int preparedAmount;

  public void prepare() {
    // Make sure we're not preparing when Sonar is already preparing answers
    if (cached != null && preparedAmount != cached.length) return;
    preparedAmount = 0;

    final SystemTimer timer = new SystemTimer();
    Sonar.get().getLogger().info("Asynchronously preparing CAPTCHA answers...");
    Sonar.get().getLogger().info("Players will be able to join even if the preparation isn't finished");

    // Prepare cache
    final SonarConfiguration.Verification.Map config = Sonar.get().getConfig().getVerification().getMap();
    cached = new MapCaptchaInfo[config.getPrecomputeAmount()];

    // Prepare everything asynchronously
    PREPARATION_SERVICE.execute(() -> {
      final List<String> fonts = config.getFonts();
      if (fonts.isEmpty()) {
        throw new IllegalArgumentException("No fonts specified!");
      }

      // Create the configuration for the image generation
      final String[] fontNames = fonts.toArray(new String[0]);
      final CaptchaConfiguration configuration = new CaptchaConfiguration(
        128, 128, config.getDictionary().toCharArray(), 5,
        config.isFlare(), config.isScratches(), config.isRipple(), config.isSmear(), config.isPinch(),
        config.getSaturation(), config.getDistortion(), new int[]{Font.PLAIN, Font.BOLD}, fontNames);

      for (int i = 0; i < config.getPrecomputeAmount(); i++) {
        final CaptchaGenerator captchaGenerator = new CaptchaGenerator(configuration);
        final CaptchaHolder captchaHolder = captchaGenerator.generate();
        final byte[] buffer = MapColorPalette.getBufferFromImage(captchaHolder.getImage());
        cached[i] = new MapCaptchaInfo(captchaHolder.getAnswer(), buffer);
        preparedAmount++;
      }

      Sonar.get().getLogger().info("Finished preparing {} CAPTCHA answers ({}s)!", preparedAmount, timer);
    });
  }

  public boolean isCaptchaAvailable() {
    return cached != null && cached[0] != null;
  }

  public MapCaptchaInfo getRandomCaptcha() {
    // Give the player a random CAPTCHA out of the ones that we've already prepared
    return cached[RANDOM.nextInt(preparedAmount)];
  }
}
