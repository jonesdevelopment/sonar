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

package jones.sonar.common.geyser;

import io.netty.channel.Channel;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Just a very small utility to determine if
 * a connection is coming from Geyser or not
 */

@UtilityClass
public class GeyserValidator {
  public boolean isGeyser(final @NotNull Channel channel) {
    final Channel parent = channel.parent();
    if (parent == null) return false;

    final Class<? extends Channel> clazz = parent.getClass();
    return clazz.getCanonicalName().startsWith("org.geysermc");
  }
}
