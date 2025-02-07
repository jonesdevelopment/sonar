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

package xyz.jonesdev.sonar.api.fallback;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.fallback.captcha.CaptchaGenerator;
import xyz.jonesdev.sonar.api.fallback.ratelimit.Ratelimiter;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Fallback {
  public static final Fallback INSTANCE = new Fallback();

  private final ConcurrentMap<InetAddress, Integer> online = new ConcurrentHashMap<>(128);
  private final ConcurrentMap<InetAddress, Boolean> connected = new ConcurrentHashMap<>(256);
  private final FallbackLoginQueue queue = new FallbackLoginQueue();
  @Setter
  private Cache<String, Integer> blacklist;
  @Setter
  private long blacklistTime;
  @Setter
  private CaptchaGenerator captchaGenerator;
  @Setter
  private Ratelimiter<InetAddress> ratelimiter;

  public boolean shouldVerifyNewPlayers() {
    return shouldPerform(Sonar.get0().getConfig().getVerification().getTiming());
  }

  public boolean shouldPerformCaptcha() {
    return shouldPerform(Sonar.get0().getConfig().getVerification().getMap().getTiming());
  }

  private static boolean shouldPerform(final SonarConfiguration.Verification.Timing timing) {
    return timing == SonarConfiguration.Verification.Timing.ALWAYS
      || (timing == SonarConfiguration.Verification.Timing.DURING_ATTACK
      && Sonar.get0().getAttackTracker().getCurrentAttack() != null);
  }
}
