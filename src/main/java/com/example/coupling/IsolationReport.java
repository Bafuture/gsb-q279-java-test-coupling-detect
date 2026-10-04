package com.example.coupling;

import java.util.List;

/**
 * 隔离验证结果。
 */
public record IsolationReport(
    List<TestRunResult> isolatedResults,
    List<TestRunResult> sharedResults,
    List<IsolationDifference> differences) {

  public IsolationReport {
    isolatedResults = List.copyOf(isolatedResults);
    sharedResults = List.copyOf(sharedResults);
    differences = List.copyOf(differences);
  }

  public boolean hasDifferences() {
    return !differences.isEmpty();
  }
}
