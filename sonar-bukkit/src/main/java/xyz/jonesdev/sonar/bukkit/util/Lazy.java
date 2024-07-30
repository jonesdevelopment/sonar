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
