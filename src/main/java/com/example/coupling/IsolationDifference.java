package com.example.coupling;

/**
 * 同一个测试在隔离状态下与共享状态下的结果差异。
 */
public record IsolationDifference(
    String testId,
    TestStatus isolatedStatus,
    TestStatus sharedStatus,
    String isolatedErrorMessage,
    String sharedErrorMessage) {

  public String describe() {
    return testId + ": 隔离运行=" + isolatedStatus + ", 共享运行=" + sharedStatus;
  }
}
