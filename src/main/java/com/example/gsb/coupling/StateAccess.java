package com.example.gsb.coupling;

import java.util.Objects;

/**
 * 一次被记录下来的共享状态访问：某个测试对某个状态执行了读或写。
 */
public record StateAccess(String testId, StateKey state, AccessMode mode) {

    public StateAccess {
        Objects.requireNonNull(testId, "testId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(mode, "mode");
    }
}
