package com.example.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** 报告内容：耦合对、涉及状态、受影响测试、顺序定位、隔离差异与建议措施。 */
class CouplingReportTest {

  private static final StateKey MODE =
      new StateKey(SharedStateKind.SYSTEM_PROPERTY, "app.mode");

  private final AtomicInteger holder = new AtomicInteger(0);

  private CouplingReport runReport(CouplingDetectionFramework framework) {
    TestCase writer = new TestCase("writesMode", () -> {
      framework.registry().write(MODE.kind(), MODE.name());
      System.setProperty("app.mode", "integration");
    });
    TestCase reader = new TestCase("readsMode", () -> {
      framework.registry().read(MODE.kind(), MODE.name());
      if (System.getProperty("app.mode") != null) {
        throw new AssertionError("app.mode should be unset");
      }
    });
    return framework.analyze(List.of(writer, reader));
  }

  @Test
  void reportContainsCouplingStateAffectedTestsAndSuggestion() {
    System.clearProperty("app.mode");
    CouplingDetectionFramework framework = new CouplingDetectionFramework();

    CouplingReport report = runReport(framework);
    String text = report.render();

    assertThat(report.couplings()).hasSize(1);
    assertThat(report.affectedTests()).containsExactly("readsMode");
    assertThat(report.involvedStates()).containsExactly(MODE);

    assertThat(text).contains("writesMode", "readsMode", "app.mode", "SYSTEM_PROPERTY");
    assertThat(text).contains("建议隔离措施");
    assertThat(text).contains("系统属性");
    assertThat(text).contains("测试间耦合检测报告");
    assertThat(System.getProperty("app.mode")).isNull();
  }

  @Test
  void reportContainsOrderAndIsolationFindings() {
    System.clearProperty("app.mode");
    CouplingDetectionFramework framework = new CouplingDetectionFramework();

    CouplingReport report = runReport(framework);

    assertThat(report.orderReport().orderSensitiveTests())
        .extracting(OrderSensitiveTest::testId)
        .containsExactly("readsMode");
    assertThat(report.isolationReport().differences())
        .extracting(IsolationDifference::testId)
        .containsExactly("readsMode");

    String text = report.render();
    assertThat(text).contains("顺序敏感测试").contains("污染源=[writesMode]");
    assertThat(text).contains("隔离验证差异").contains("隔离运行=PASSED", "共享运行=FAILED");
    assertThat(text).contains("单独运行通过、共享运行失败");
  }

  @Test
  void reportRendersEmptySectionsWhenNothingFound() {
    CouplingDetectionFramework framework = new CouplingDetectionFramework();
    framework.includeSystemProperties(false);

    CouplingReport report = framework.analyze(
        List.of(new TestCase("clean1", () -> { }), new TestCase("clean2", () -> { })));
    String text = report.render();

    assertThat(text).contains("(无)");
    assertThat(report.couplings()).isEmpty();
    assertThat(report.orderReport().hasOrderSensitiveTests()).isFalse();
    assertThat(report.isolationReport().hasDifferences()).isFalse();
  }
}
