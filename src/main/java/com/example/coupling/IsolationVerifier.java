package com.example.coupling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 隔离验证器。
 *
 * 先捕获一份干净的共享状态快照：
 * <ol>
 *   <li>隔离运行：每个测试执行前都恢复到干净快照，彼此完全隔离；</li>
 *   <li>共享运行：恢复一次快照后顺序执行所有测试，状态相互累积；</li>
 * </ol>
 * 最后对比两种模式下每个测试的结果，输出存在差异的测试。
 * 验证结束后会恢复初始快照，不污染外部环境。
 */
public final class IsolationVerifier {

  private final StateSnapshotter<?> snapshotter;
  private final StateAccessRegistry registry;

  public IsolationVerifier(StateSnapshotter<?> snapshotter) {
    this(snapshotter, null);
  }

  /**
   * @param registry 可选；提供时会为每个执行的测试建立登记上下文（暂停记录，
   *                 避免重复执行污染访问统计）
   */
  public IsolationVerifier(StateSnapshotter<?> snapshotter, StateAccessRegistry registry) {
    this.snapshotter = snapshotter;
    this.registry = registry;
  }

  public IsolationReport verify(List<TestCase> tests) {
    return verifyWith(snapshotter, tests);
  }

  private <S> IsolationReport verifyWith(StateSnapshotter<S> snapshotter,
                                         List<TestCase> tests) {
    S baseline = snapshotter.capture();

    List<TestRunResult> isolated = new ArrayList<>();
    for (TestCase test : tests) {
      snapshotter.restore(baseline);
      isolated.add(execute(test));
    }

    snapshotter.restore(baseline);
    List<TestRunResult> shared = new ArrayList<>();
    for (TestCase test : tests) {
      shared.add(execute(test));
    }

    snapshotter.restore(baseline);

    Map<String, TestRunResult> isolatedById = isolated.stream()
        .collect(Collectors.toMap(TestRunResult::testId, Function.identity()));
    List<IsolationDifference> differences = new ArrayList<>();
    for (TestRunResult sharedResult : shared) {
      TestRunResult isolatedResult = isolatedById.get(sharedResult.testId());
      if (isolatedResult.status() != sharedResult.status()) {
        differences.add(new IsolationDifference(
            sharedResult.testId(),
            isolatedResult.status(),
            sharedResult.status(),
            errorMessage(isolatedResult),
            errorMessage(sharedResult)));
      }
    }
    return new IsolationReport(isolated, shared, differences);
  }

  private TestRunResult execute(TestCase test) {
    if (registry == null) {
      return test.execute();
    }
    registry.setRecording(false);
    registry.beginTest(test.id());
    try {
      return test.execute();
    } finally {
      registry.endTest();
      registry.setRecording(true);
    }
  }

  private static String errorMessage(TestRunResult result) {
    if (result.error() == null) {
      return null;
    }
    Throwable error = result.error();
    return error.getClass().getSimpleName()
        + (error.getMessage() != null ? ": " + error.getMessage() : "");
  }
}
