/*
 * Copyright (C) 2023, jones
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

import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Vector;

public final class FallbackHolder implements Fallback {
  public static final FallbackHolder INSTANCE = new FallbackHolder();

  @Getter
  private final Collection<InetAddress> connected = new Vector<>();
  @Getter
  private final Collection<InetAddress> verified = new Vector<>();
  @Getter
  private final Collection<InetAddress> blacklisted = new Vector<>();
  @Getter
  private final FallbackQueue queue = new FallbackQueue();
  @Getter
  @Setter
  private FallbackFilter attemptLimiter = inetAddress -> true;
}
