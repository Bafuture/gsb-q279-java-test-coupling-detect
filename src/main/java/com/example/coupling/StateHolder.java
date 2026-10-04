package com.example.coupling;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 一个可快照/可恢复的共享状态句柄，用于接入静态变量或单例字段：
 * getter 读取当前值，setter 写回原值。
 */
public record StateHolder<T>(StateKey key, Supplier<T> getter, Consumer<T> setter) {

  public StateHolder {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(getter, "getter");
    Objects.requireNonNull(setter, "setter");
  }

  public Object captureValue() {
    return getter.get();
  }

  @SuppressWarnings("unchecked")
  public void restoreValue(Object value) {
    setter.accept((T) value);
  }
}
