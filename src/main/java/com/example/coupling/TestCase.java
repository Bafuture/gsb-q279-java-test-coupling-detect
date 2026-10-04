package com.example.coupling;

import java.util.Objects;

/**
 * 一个可执行的测试用例：唯一标识 + 测试体。
 */
public record TestCase(String id, ThrowingRunnable action) {

  public TestCase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(action, "action");
    if (id.isBlank()) {
      throw new IllegalArgumentException("test id must not be blank");
    }
  }

  /** 执行测试体并捕获结果，永不抛出异常。 */
  public TestRunResult execute() {
    try {
      action.run();
      return TestRunResult.passed(id);
    } catch (Throwable t) {
      return TestRunResult.failed(id, t);
    }
  }
}
