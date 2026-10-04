package com.example.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 白名单：已确认无害的耦合不参与告警。 */
class CouplingIgnoreListTest {

  private static final StateKey STATE =
      new StateKey(SharedStateKind.SINGLETON, "cache");
  private static final Coupling COUPLING = new Coupling(STATE, "testA", "testB");

  private final CouplingIgnoreList ignoreList = new CouplingIgnoreList();

  @Test
  void exactCouplingCanBeIgnored() {
    ignoreList.ignore(COUPLING);

    assertThat(ignoreList.isIgnored(COUPLING)).isTrue();
    assertThat(ignoreList.isIgnored(new Coupling(STATE, "testA", "testC"))).isFalse();
  }

  @Test
  void wholeStateCanBeIgnored() {
    ignoreList.ignoreState(STATE);

    assertThat(ignoreList.isIgnored(COUPLING)).isTrue();
    assertThat(ignoreList.isIgnored(new Coupling(STATE, "x", "y"))).isTrue();
    assertThat(ignoreList.isIgnored(new Coupling(
        new StateKey(SharedStateKind.TEMP_FILE, "f"), "x", "y"))).isFalse();
  }

  @Test
  void testPairCanBeIgnored() {
    ignoreList.ignorePair("testA", "testB");

    assertThat(ignoreList.isIgnored(COUPLING)).isTrue();
    assertThat(ignoreList.isIgnored(new Coupling(
        new StateKey(SharedStateKind.SYSTEM_PROPERTY, "p"), "testA", "testB"))).isTrue();
    assertThat(ignoreList.isIgnored(new Coupling(STATE, "testB", "testA"))).isFalse();
  }

  @Test
  void ignoredCouplingsAreExcludedFromDetection() {
    StateAccessRegistry registry = new StateAccessRegistry();
    registry.beginTest("testA");
    registry.write(SharedStateKind.SINGLETON, "cache");
    registry.endTest();
    registry.beginTest("testB");
    registry.read(SharedStateKind.SINGLETON, "cache");
    registry.endTest();

    CouplingDetector detector = new CouplingDetector();
    assertThat(detector.detect(registry, ignoreList)).hasSize(1);

    ignoreList.ignore(COUPLING);
    assertThat(detector.detect(registry, ignoreList)).isEmpty();
  }

  @Test
  void sizeCountsAllEntries() {
    ignoreList.ignore(COUPLING);
    ignoreList.ignoreState(STATE);
    ignoreList.ignorePair("a", "b");

    assertThat(ignoreList.size()).isEqualTo(3);
  }
}
