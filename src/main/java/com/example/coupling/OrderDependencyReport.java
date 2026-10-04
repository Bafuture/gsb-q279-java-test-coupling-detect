package com.example.coupling;

import java.util.List;

/**
 * 顺序依赖验证结果。
 */
public record OrderDependencyReport(
    List<OrderSensitiveTest> orderSensitiveTests,
    List<TestRunResult> baselineResults,
    List<TestRunResult> swappedResults) {

  public OrderDependencyReport {
    orderSensitiveTests = List.copyOf(orderSensitiveTests);
    baselineResults = List.copyOf(baselineResults);
    swappedResults = List.copyOf(swappedResults);
  }

  public boolean hasOrderSensitiveTests() {
    return !orderSensitiveTests.isEmpty();
  }
}
