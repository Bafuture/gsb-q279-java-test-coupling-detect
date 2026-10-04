package com.example.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 统计：测试数、状态访问次数、耦合对数与顺序敏感测试数。 */
class CouplingStatisticsTest {

  @Test
  void countsTestsAccessesCouplingsAndOrderSensitiveTests() {
    StateAccessRegistry registry = new StateAccessRegistry();
    registry.beginTest("a");
    registry.write(SharedStateKind.STATIC_VARIABLE, "v1");
    registry.write(SharedStateKind.SINGLETON, "s1");
    registry.endTest();
    registry.beginTest("b");
    registry.read(SharedStateKind.STATIC_VARIABLE, "v1");
    registry.endTest();
    registry.beginTest("c");
    registry.read(SharedStateKind.TEMP_FILE, "f1");
    registry.endTest();

    List<Coupling> couplings =
        new CouplingDetector().detect(registry, new CouplingIgnoreList());

    OrderSensitiveTest sensitive = new OrderSensitiveTest(
        "b", TestStatus.PASSED, TestStatus.FAILED, List.of("a"), List.of());
    OrderDependencyReport orderReport =
        new OrderDependencyReport(List.of(sensitive), List.of(), List.of());
    IsolationDifference difference = new IsolationDifference(
        "b", TestStatus.PASSED, TestStatus.FAILED, null, "boom");
    IsolationReport isolationReport =
        new IsolationReport(List.of(), List.of(), List.of(difference));

    CouplingStatistics stats =
        CouplingStatistics.from(registry, couplings, orderReport, isolationReport);

    assertThat(stats.testCount()).isEqualTo(3);
    assertThat(stats.totalAccessCount()).isEqualTo(4);
    assertThat(stats.writeCount()).isEqualTo(2);
    assertThat(stats.readCount()).isEqualTo(2);
    assertThat(stats.couplingPairCount()).isEqualTo(1);
    assertThat(stats.orderSensitiveTestCount()).isEqualTo(1);
    assertThat(stats.isolationDifferenceCount()).isEqualTo(1);
  }

  @Test
  void emptyRegistryProducesZeros() {
    StateAccessRegistry registry = new StateAccessRegistry();
    OrderDependencyReport order =
        new OrderDependencyReport(List.of(), List.of(), List.of());
    IsolationReport isolation =
        new IsolationReport(List.of(), List.of(), List.of());

    CouplingStatistics stats =
        CouplingStatistics.from(registry, List.of(), order, isolation);

    assertThat(stats.testCount()).isZero();
    assertThat(stats.totalAccessCount()).isZero();
    assertThat(stats.couplingPairCount()).isZero();
    assertThat(stats.orderSensitiveTestCount()).isZero();
  }
}
