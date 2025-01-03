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

package xyz.jonesdev.sonar.api.fingerprint;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class FingerprintingUtil {

  /**
   * Returns a hex string representing a hash of the username and IP address
   */
  public @NotNull String getFingerprint(final @NotNull String username,
                                        final @NotNull String hostAddress) {
    final int hash0 = username.hashCode();
    final int hash1 = hostAddress.hashCode();
    final int combined = hash0 + hash1;
    return Integer.toHexString(hash0 >> 4)
      + Integer.toHexString(combined << 2)
      + Integer.toHexString(hash1 >> 4);
  }
}
