package com.example.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 端到端：门面串联检测、顺序、隔离、白名单、统计与临时文件状态。 */
class CouplingDetectionFrameworkTest {

  @Test
  void endToEndFlowDetectsAndReportsAllFindings() {
    CouplingDetectionFramework framework = new CouplingDetectionFramework();
    System.clearProperty("e2e.flag");

    TestCase producer = new TestCase("producer", () -> {
      framework.registry().write(SharedStateKind.SYSTEM_PROPERTY, "e2e.flag");
      System.setProperty("e2e.flag", "on");
    });
    TestCase consumer = new TestCase("consumer", () -> {
      framework.registry().read(SharedStateKind.SYSTEM_PROPERTY, "e2e.flag");
      assertThat(System.getProperty("e2e.flag"))
          .as("consumer expects a clean JVM")
          .isNull();
    });

    CouplingReport report = framework.analyze(List.of(producer, consumer));

    assertThat(report.couplings()).hasSize(1);
    assertThat(report.couplings().get(0).writerTest()).isEqualTo("producer");
    assertThat(report.couplings().get(0).readerTest()).isEqualTo("consumer");
    assertThat(report.orderReport().orderSensitiveTests())
        .extracting(OrderSensitiveTest::testId)
        .containsExactly("consumer");
    assertThat(report.isolationReport().differences())
        .extracting(IsolationDifference::testId)
        .containsExactly("consumer");
    assertThat(report.statistics().testCount()).isEqualTo(2);
    assertThat(report.statistics().totalAccessCount()).isEqualTo(2);
    assertThat(report.statistics().couplingPairCount()).isEqualTo(1);
    assertThat(report.statistics().orderSensitiveTestCount()).isEqualTo(1);
    assertThat(System.getProperty("e2e.flag")).isNull();
  }

  @Test
  void whitelistedCouplingsDoNotAppearInReport() {
    CouplingDetectionFramework framework = new CouplingDetectionFramework();
    System.clearProperty("e2e.flag");

    TestCase producer = new TestCase("producer", () -> {
      framework.registry().write(SharedStateKind.SYSTEM_PROPERTY, "e2e.flag");
      System.setProperty("e2e.flag", "on");
    });
    TestCase consumer = new TestCase("consumer", () -> {
      framework.registry().read(SharedStateKind.SYSTEM_PROPERTY, "e2e.flag");
      if (System.getProperty("e2e.flag") != null) {
        throw new AssertionError("polluted");
      }
    });

    framework.ignoreList().ignore(new Coupling(
        new StateKey(SharedStateKind.SYSTEM_PROPERTY, "e2e.flag"),
        "producer", "consumer"));
    CouplingReport report = framework.analyze(List.of(producer, consumer));

    assertThat(report.couplings()).isEmpty();
    assertThat(report.affectedTests()).isEmpty();
    assertThat(report.statistics().couplingPairCount()).isZero();
    assertThat(report.ignoredCount()).isEqualTo(1);
    // 白名单只影响告警，不影响顺序/隔离验证本身
    assertThat(report.orderReport().hasOrderSensitiveTests()).isTrue();
  }

  @Test
  void detectsTempFilePollutionAndRestoresDirectory(@TempDir Path tempDir) throws IOException {
    Path inbox = tempDir.resolve("inbox");
    Files.createDirectories(inbox);

    CouplingDetectionFramework framework = new CouplingDetectionFramework()
        .includeSystemProperties(false)
        .tempDirectory(inbox);

    TestCase fileWriter = new TestCase("fileWriter", () -> {
      framework.registry().write(SharedStateKind.TEMP_FILE, "result.txt");
      Files.writeString(inbox.resolve("result.txt"), "done");
    });
    TestCase fileReader = new TestCase("fileReader", () -> {
      framework.registry().read(SharedStateKind.TEMP_FILE, "result.txt");
      if (Files.exists(inbox.resolve("result.txt"))) {
        throw new AssertionError("inbox must be empty at start");
      }
    });

    CouplingReport report = framework.analyze(List.of(fileWriter, fileReader));
    String text = report.render();

    assertThat(report.couplings()).hasSize(1);
    assertThat(report.couplings().get(0).state().kind())
        .isEqualTo(SharedStateKind.TEMP_FILE);
    assertThat(report.orderReport().orderSensitiveTests())
        .extracting(OrderSensitiveTest::testId)
        .containsExactly("fileReader");
    assertThat(report.isolationReport().differences())
        .extracting(IsolationDifference::testId)
        .containsExactly("fileReader");
    assertThat(text).contains("@TempDir");
    assertThat(inbox.resolve("result.txt")).doesNotExist();
  }
}
