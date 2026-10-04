package com.example.coupling;

/**
 * 一次测试执行的结果。
 */
public record TestRunResult(String testId, TestStatus status, Throwable error) {

  public static TestRunResult passed(String testId) {
    return new TestRunResult(testId, TestStatus.PASSED, null);
  }

  public static TestRunResult failed(String testId, Throwable error) {
    return new TestRunResult(testId, TestStatus.FAILED, error);
  }

  public boolean isPassed() {
    return status == TestStatus.PASSED;
  }
}
