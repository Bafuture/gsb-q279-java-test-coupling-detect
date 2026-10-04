package com.example.gsb.coupling;

import java.util.Objects;

/**
 * 一个共享状态条目的逻辑标识：类别 + 在该类别下唯一的名称，
 * 例如 {@code (STATIC_VARIABLE, "com.foo.Config#counter")}
 * 或 {@code (SYSTEM_PROPERTY, "user.timezone")}。
 */
public record StateKey(StateKind kind, String name) {

    public StateKey {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String toString() {
        return kind + ":" + name;
    }
}
