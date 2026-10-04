package com.example.coupling;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 顺序依赖验证器。
 *
 * 先按原始顺序执行一遍测试，再按相反顺序执行一遍。
 * 若提供了 {@link StateSnapshotter}，两轮执行之间会恢复到干净快照，
 * 避免第一轮遗留的状态干扰第二轮结果。
 * 结果发生变化的测试即为顺序敏感测试；对失败的测试，
 * 结合共享状态访问记录定位污染源：在它失败的那一轮执行顺序中先于它执行、
 * 且写入了它所读取状态的测试，即为污染它的测试。
 */
public final class OrderDependencyVerifier {

  private final StateAccessRegistry registry;
  private final StateSnapshotter<?> snapshotter;

  public OrderDependencyVerifier(StateAccessRegistry registry) {
    this(registry, null);
  }

  public OrderDependencyVerifier(StateAccessRegistry registry,
                                 StateSnapshotter<?> snapshotter) {
    this.registry = registry;
    this.snapshotter = snapshotter;
  }

  public OrderDependencyReport verify(List<TestCase> tests) {
    return verifyWith(snapshotter, tests);
  }

  private <S> OrderDependencyReport verifyWith(StateSnapshotter<S> snapshotter,
                                               List<TestCase> tests) {
    S cleanSnapshot = snapshotter != null ? snapshotter.capture() : null;

    List<TestRunResult> baseline = runAll(tests, true);

    if (snapshotter != null) {
      snapshotter.restore(cleanSnapshot);
    }

    List<TestCase> swapped = new ArrayList<>(tests);
    java.util.Collections.reverse(swapped);
    List<TestRunResult> swappedResults = runAll(swapped, false);

    if (snapshotter != null) {
      snapshotter.restore(cleanSnapshot);
    }

    Map<String, TestRunResult> baselineById = baseline.stream()
        .collect(Collectors.toMap(TestRunResult::testId, Function.identity()));
    Map<String, TestRunResult> swappedById = swappedResults.stream()
        .collect(Collectors.toMap(TestRunResult::testId, Function.identity()));

    List<String> baselineOrder = tests.stream().map(TestCase::id).toList();
    List<String> swappedOrder = swapped.stream().map(TestCase::id).toList();

    List<OrderSensitiveTest> sensitive = new ArrayList<>();
    for (TestCase test : tests) {
      String id = test.id();
      TestStatus baselineStatus = baselineById.get(id).status();
      TestStatus swappedStatus = swappedById.get(id).status();
      if (baselineStatus == swappedStatus) {
        continue;
      }
      List<String> failingOrder =
          swappedStatus == TestStatus.FAILED ? swappedOrder : baselineOrder;
      List<String> predecessors =
          failingOrder.subList(0, failingOrder.indexOf(id));
      Set<String> polluters = new LinkedHashSet<>();
      Set<String> suspects = new LinkedHashSet<>();
      Set<StateKey> readStates = registry.statesReadBy(id);
      for (String candidate : predecessors) {
        Set<StateKey> written = registry.statesWrittenBy(candidate);
        boolean evidence = written.stream().anyMatch(readStates::contains);
        (evidence ? polluters : suspects).add(candidate);
      }
      sensitive.add(new OrderSensitiveTest(id, baselineStatus, swappedStatus,
          List.copyOf(polluters), List.copyOf(suspects)));
    }
    return new OrderDependencyReport(sensitive, baseline, swappedResults);
  }

  private List<TestRunResult> runAll(List<TestCase> tests, boolean recordAccesses) {
    List<TestRunResult> results = new ArrayList<>();
    registry.setRecording(recordAccesses);
    try {
      for (TestCase test : tests) {
        registry.beginTest(test.id());
        try {
          results.add(test.execute());
        } finally {
          registry.endTest();
        }
      }
    } finally {
      registry.setRecording(true);
    }
    return results;
  }

}
