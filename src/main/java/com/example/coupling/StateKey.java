package com.example.coupling;

import java.util.Objects;

/**
 * 共享状态的唯一标识：状态类别 + 状态名称。
 */
public record StateKey(SharedStateKind kind, String name) {

  public StateKey {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("state name must not be blank");
    }
  }
}
