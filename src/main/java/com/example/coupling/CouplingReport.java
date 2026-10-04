package com.example.coupling;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 耦合检测完整报告：耦合对、涉及状态、受影响测试、顺序依赖定位、
 * 隔离差异、建议隔离措施与统计信息。
 */
public final class CouplingReport {

  private final List<Coupling> couplings;
  private final OrderDependencyReport orderReport;
  private final IsolationReport isolationReport;
  private final CouplingStatistics statistics;
  private final int ignoredCount;

  public CouplingReport(List<Coupling> couplings,
                        OrderDependencyReport orderReport,
                        IsolationReport isolationReport,
                        CouplingStatistics statistics,
                        int ignoredCount) {
    this.couplings = List.copyOf(couplings);
    this.orderReport = orderReport;
    this.isolationReport = isolationReport;
    this.statistics = statistics;
    this.ignoredCount = ignoredCount;
  }

  public List<Coupling> couplings() {
    return couplings;
  }

  public OrderDependencyReport orderReport() {
    return orderReport;
  }

  public IsolationReport isolationReport() {
    return isolationReport;
  }

  public CouplingStatistics statistics() {
    return statistics;
  }

  public int ignoredCount() {
    return ignoredCount;
  }

  /** 所有耦合涉及的共享状态。 */
  public Set<StateKey> involvedStates() {
    Set<StateKey> states = new LinkedHashSet<>();
    for (Coupling coupling : couplings) {
      states.add(coupling.state());
    }
    return states;
  }

  /** 所有受耦合影响（读取了被写入状态）的测试。 */
  public Set<String> affectedTests() {
    Set<String> tests = new LinkedHashSet<>();
    for (Coupling coupling : couplings) {
      tests.add(coupling.readerTest());
    }
    return tests;
  }

  /** 渲染为可读的文本报告。 */
  public String render() {
    StringBuilder sb = new StringBuilder();
    sb.append("===== 测试间耦合检测报告 =====\n");
    sb.append(String.format(
        "[统计] 测试数=%d, 状态访问=%d (读=%d, 写=%d), 耦合对=%d, 顺序敏感测试=%d, 隔离差异测试=%d, 白名单=%d%n",
        statistics.testCount(), statistics.totalAccessCount(),
        statistics.readCount(), statistics.writeCount(),
        statistics.couplingPairCount(), statistics.orderSensitiveTestCount(),
        statistics.isolationDifferenceCount(), ignoredCount));

    sb.append("\n--- 耦合对 ---\n");
    if (couplings.isEmpty()) {
      sb.append("(无)\n");
    } else {
      for (Coupling coupling : couplings) {
        sb.append(String.format("- %s 写入 [%s '%s'] -> %s 读取%n",
            coupling.writerTest(),
            coupling.state().kind(),
            coupling.state().name(),
            coupling.readerTest()));
        sb.append("  建议隔离措施: ")
            .append(IsolationMeasureSuggester.suggest(coupling.state().kind()))
            .append('\n');
      }
    }

    sb.append("\n--- 顺序敏感测试 ---\n");
    if (!orderReport.hasOrderSensitiveTests()) {
      sb.append("(无)\n");
    } else {
      for (OrderSensitiveTest test : orderReport.orderSensitiveTests()) {
        sb.append(String.format("- %s: 原顺序=%s, 交换顺序后=%s, 污染源=%s",
            test.testId(), test.baselineStatus(), test.swappedStatus(),
            test.polluters()));
        if (!test.suspects().isEmpty()) {
          sb.append(", 嫌疑测试(无直接状态证据)=").append(test.suspects());
        }
        sb.append('\n');
      }
    }

    sb.append("\n--- 隔离验证差异 ---\n");
    if (!isolationReport.hasDifferences()) {
      sb.append("(无)\n");
    } else {
      for (IsolationDifference difference : isolationReport.differences()) {
        sb.append("- ").append(difference.describe()).append('\n');
        if (difference.isolatedStatus() == TestStatus.PASSED
            && difference.sharedStatus() == TestStatus.FAILED) {
          sb.append("  结论: 单独运行通过、共享运行失败，可确认存在测试间污染\n");
          if (difference.sharedErrorMessage() != null) {
            sb.append("  共享运行错误: ").append(difference.sharedErrorMessage()).append('\n');
          }
        }
      }
    }

    sb.append("\n--- 涉及状态 ---\n");
    Set<StateKey> states = involvedStates();
    if (states.isEmpty()) {
      sb.append("(无)\n");
    } else {
      for (StateKey state : states) {
        sb.append(String.format("- %s '%s'%n", state.kind(), state.name()));
      }
    }

    sb.append("\n--- 受影响测试 ---\n");
    Set<String> affected = affectedTests();
    if (affected.isEmpty()) {
      sb.append("(无)\n");
    } else {
      for (String test : affected) {
        sb.append("- ").append(test).append('\n');
      }
    }
    return sb.toString();
  }
}
