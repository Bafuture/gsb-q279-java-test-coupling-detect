package com.example.gsb.coupling;

import java.util.Objects;

/**
 * 一对潜在耦合：{@code writerTest} 写入 {@code state}，{@code readerTest} 读取它。
 *
 * @param writerTest 写入共享状态的测试（可能的污染源）
 * @param readerTest 读取共享状态的测试（可能被污染的测试）
 * @param state      涉及的共享状态
 */
public record CouplingPair(String writerTest, String readerTest, StateKey state) {

    public CouplingPair {
        Objects.requireNonNull(writerTest, "writerTest");
        Objects.requireNonNull(readerTest, "readerTest");
        Objects.requireNonNull(state, "state");
        if (writerTest.equals(readerTest)) {
            throw new IllegalArgumentException("测试自身的读写不构成耦合: " + writerTest);
        }
    }

    @Override
    public String toString() {
        return writerTest + " --[" + state + "]--> " + readerTest;
    }
}
