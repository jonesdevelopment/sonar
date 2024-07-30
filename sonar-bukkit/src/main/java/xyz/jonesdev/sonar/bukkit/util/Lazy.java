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

package xyz.jonesdev.sonar.bukkit.util;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class Lazy<T> {
  private T value;
  private boolean isInitialized = false;
  private Throwable throwable = null;
  private final @NotNull LazySupplier<T> supplier;

  @SneakyThrows
  public T getValue() {
    if (throwable != null) {
      throw throwable;
    }
    if (isInitialized) {
      return value;
    } else {
      final T result;
      try {
        result = supplier.get();
      } catch (Throwable throwable) {
        this.throwable = throwable;
        throw throwable;
      }
      isInitialized = true;
      return value = result;
    }
  }

  @FunctionalInterface
  public interface LazySupplier<T> {
    T get() throws Throwable;
  }
}
