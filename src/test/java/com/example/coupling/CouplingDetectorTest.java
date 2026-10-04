package com.example.coupling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 耦合识别：A 写入共享状态、B 读取同一状态即构成潜在耦合。 */
class CouplingDetectorTest {

  private final StateAccessRegistry registry = new StateAccessRegistry();
  private final CouplingDetector detector = new CouplingDetector();
  private final CouplingIgnoreList ignoreList = new CouplingIgnoreList();

  private void access(String testId, AccessType type, SharedStateKind kind, String name) {
    registry.beginTest(testId);
    try {
      if (type == AccessType.READ) {
        registry.read(kind, name);
      } else {
        registry.write(kind, name);
      }
    } finally {
      registry.endTest();
    }
  }

  @Test
  void detectsWriteReadPairAsCoupling() {
    access("testA", AccessType.WRITE, SharedStateKind.STATIC_VARIABLE, "counter");
    access("testB", AccessType.READ, SharedStateKind.STATIC_VARIABLE, "counter");

    assertThat(detector.detect(registry, ignoreList))
        .containsExactly(new Coupling(
            new StateKey(SharedStateKind.STATIC_VARIABLE, "counter"), "testA", "testB"));
  }

  @Test
  void readOnlyOrWriteOnlyAccessesDoNotCouple() {
    access("testA", AccessType.READ, SharedStateKind.SINGLETON, "cache");
    access("testB", AccessType.READ, SharedStateKind.SINGLETON, "cache");
    access("testC", AccessType.WRITE, SharedStateKind.TEMP_FILE, "out.txt");
    access("testD", AccessType.WRITE, SharedStateKind.TEMP_FILE, "out.txt");

    assertThat(detector.detect(registry, ignoreList)).isEmpty();
  }

  @Test
  void accessesOnDifferentStatesDoNotCouple() {
    access("testA", AccessType.WRITE, SharedStateKind.SYSTEM_PROPERTY, "app.mode");
    access("testB", AccessType.READ, SharedStateKind.SYSTEM_PROPERTY, "app.locale");

    assertThat(detector.detect(registry, ignoreList)).isEmpty();
  }

  @Test
  void sameTestWritingAndReadingDoesNotCoupleWithItself() {
    access("testA", AccessType.WRITE, SharedStateKind.STATIC_VARIABLE, "x");
    access("testA", AccessType.READ, SharedStateKind.STATIC_VARIABLE, "x");

    assertThat(detector.detect(registry, ignoreList)).isEmpty();
  }

  @Test
  void oneWriterCouplesWithEveryReader() {
    access("writer", AccessType.WRITE, SharedStateKind.SINGLETON, "config");
    access("reader1", AccessType.READ, SharedStateKind.SINGLETON, "config");
    access("reader2", AccessType.READ, SharedStateKind.SINGLETON, "config");

    assertThat(detector.detect(registry, ignoreList))
        .extracting(Coupling::readerTest)
        .containsExactlyInAnyOrder("reader1", "reader2");
  }

  @Test
  void coversAllFourStateKinds() {
    for (SharedStateKind kind : SharedStateKind.values()) {
      StateAccessRegistry fresh = new StateAccessRegistry();
      fresh.beginTest("w");
      fresh.write(kind, "state");
      fresh.endTest();
      fresh.beginTest("r");
      fresh.read(kind, "state");
      fresh.endTest();

      assertThat(detector.detect(fresh, ignoreList))
          .as("kind %s should be detectable", kind)
          .hasSize(1);
    }
  }

  @Test
  void registryRejectsAccessOutsideTestContext() {
    assertThatThrownBy(() -> registry.read(SharedStateKind.STATIC_VARIABLE, "x"))
        .isInstanceOf(IllegalStateException.class);
  }
}
