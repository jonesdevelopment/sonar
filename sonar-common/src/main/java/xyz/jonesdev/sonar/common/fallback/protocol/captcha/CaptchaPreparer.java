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

import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.capja.SimpleCaptchaGenerator;
import xyz.jonesdev.capja.filter.SimpleRippleFilter;
import xyz.jonesdev.capja.filter.SimpleScratchFilter;
import xyz.jonesdev.capja.libs.jhlabs.image.AbstractBufferedImageOp;
import xyz.jonesdev.capja.libs.jhlabs.image.BumpFilter;
import xyz.jonesdev.capja.libs.jhlabs.image.SmearFilter;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.timer.SystemTimer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

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
      // Create the images using capja
      final @Nullable File backgroundImage = Sonar.get().getConfig().getVerification().getMap().getBackgroundImage();
      final SimpleCaptchaGenerator generator = new SimpleCaptchaGenerator(128, 128, backgroundImage);
      final char[] dictionary = config.getDictionary().toCharArray();

      // Prepare the filters for the CAPTCHA
      final List<AbstractBufferedImageOp> filters = new ArrayList<>();
      if (Sonar.get().getConfig().getVerification().getMap().isScratches()) {
        filters.add(new SimpleScratchFilter(5));
      }
      if (Sonar.get().getConfig().getVerification().getMap().isRipple()) {
        final SimpleRippleFilter rippleFilter = new SimpleRippleFilter();
        rippleFilter.setXAmplitude(0);
        float yAmplitude = 10 - ThreadLocalRandom.current().nextInt(20);
        if (Math.abs(yAmplitude) < 3) {
          yAmplitude = yAmplitude >= 0 ? 3 : -3;
        }
        rippleFilter.setYAmplitude(yAmplitude);
        filters.add(rippleFilter);
      }
      if (Sonar.get().getConfig().getVerification().getMap().getDistortion().isEnabled()) {
        final SmearFilter smearFilter = new SmearFilter();
        smearFilter.setShape(Sonar.get().getConfig().getVerification().getMap().getDistortion().getShape());
        smearFilter.setMix(Sonar.get().getConfig().getVerification().getMap().getDistortion().getMix());
        smearFilter.setDensity(Sonar.get().getConfig().getVerification().getMap().getDistortion().getDensity());
        smearFilter.setDistance(Sonar.get().getConfig().getVerification().getMap().getDistortion().getDistance());
        filters.add(smearFilter);
      }
      if (Sonar.get().getConfig().getVerification().getMap().isBump()) {
        filters.add(new BumpFilter());
      }

      for (preparedAmount = 0; preparedAmount < config.getPrecomputeAmount(); preparedAmount++) {
        if (!Sonar.get().getConfig().getVerification().getMap().isAutoColor()) {
          // Generate a random gradient color if automatic coloring is disabled
          final Color color0 = Color.getHSBColor((float) Math.random(), 1, 1);
          final Color color1 = Color.getHSBColor((float) Math.random(), 1, 0.5f);
          final GradientPaint gradient = new GradientPaint(0, 0, color0,
            generator.getWidth(), generator.getHeight(), color1);
          generator.setGradient(gradient);
        }
        // Generate CAPTCHA answer
        final char[] answer = new char[5];
        for (int j = 0; j < answer.length; j++) {
          answer[j] = dictionary[ThreadLocalRandom.current().nextInt(dictionary.length)];
        }
        final BufferedImage image = generator.createImage(answer, filters);
        // Convert and cache converted Minecraft map bytes
        final int[] buffer = MapColorPalette.getBufferFromImage(image);
        cached[preparedAmount] = new MapCaptchaInfo(new String(answer), buffer);
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
