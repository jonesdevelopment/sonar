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

package xyz.jonesdev.sonar.api;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class SonarSupplier {
  Sonar sonar;

  public void set(final @NotNull Sonar _sonar) {
    if (sonar != null) {
      throw new IllegalStateException("Sonar is already initialized");
    }
    sonar = _sonar;
  }

  public Sonar get() {
    if (sonar == null) {
      throw new IllegalStateException("Sonar is not initialized yet");
    }
    return sonar;
  }
}
