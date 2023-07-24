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

package jones.sonar.api.fallback;

import jones.sonar.api.Sonar;
import jones.sonar.api.logger.Logger;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.cappuchino.Cache;
import xyz.jonesdev.cappuchino.Cappuchino;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class FallbackHolder implements Fallback {
  public static final @NotNull FallbackHolder INSTANCE = new FallbackHolder();

  @Getter
  @NotNull
  private final Map<String, InetAddress> connected = new ConcurrentHashMap<>();
  @Getter
  @NotNull
  private final Collection<String> verified = new Vector<>(1);
  @Getter
  @NotNull
  private final Cache<String> blacklisted = Cappuchino.buildExpiring(
    10L, TimeUnit.MINUTES // Only block the player for a few minutes to avoid issues
  );
  @Getter
  @NotNull
  private final FallbackQueue queue = new FallbackQueue();
  @Getter
  @Setter
  @NotNull
  private FallbackFilter attemptLimiter = inetAddress -> true;
  @Getter
  @NotNull
  private final Sonar sonar = Sonar.get();
  @Getter
  @NotNull
  private final Logger logger = new Logger() {

    @Override
    public void info(final String message, final Object... args) {
      sonar.getLogger().info("[Fallback] " + message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      sonar.getLogger().warn("[Fallback] " + message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      sonar.getLogger().error("[Fallback] " + message, args);
    }
  };
}
