package com.example.gsb.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StatisticsTest {

    @Test
    void aggregatesCountsFromAllFrameworkParts() {
        StateAccessLog log = new StateAccessLog();
        StateKey s1 = new StateKey(StateKind.STATIC_VARIABLE, "S1");
        StateKey s2 = new StateKey(StateKind.SINGLETON, "S2");
        log.record("testA", s1, AccessMode.WRITE);
        log.record("testB", s1, AccessMode.READ);
        log.record("testC", s2, AccessMode.WRITE);

        List<TestCase> tests = List.of(
                TestCase.of("testA", () -> { }),
                TestCase.of("testB", () -> { }),
                TestCase.of("testC", () -> { }));

        List<CouplingPair> couplings = new CouplingDetector().detect(log);
        OrderDependencyReport orderReport = new OrderDependencyReport(
                List.of("testB"),
                Map.of("testB", List.of("testA")),
                Map.of(), Map.of(), Map.of());
        List<IsolationDiff> diffs = List.of(
                new IsolationDiff("testB", TestOutcome.fail("polluted"), TestOutcome.pass()));

        Statistics stats = Statistics.of(tests, log, couplings, orderReport, diffs, 2);

        assertThat(stats.testCount()).isEqualTo(3);
        assertThat(stats.stateAccessCount()).isEqualTo(3);
        assertThat(stats.couplingPairCount()).isEqualTo(1);
        assertThat(stats.orderSensitiveTestCount()).isEqualTo(1);
        assertThat(stats.isolationDiffCount()).isEqualTo(1);
        assertThat(stats.ignoredCouplingCount()).isEqualTo(2);
    }

    @Test
    void countsZeroWhenNothingHappened() {
        Statistics stats = Statistics.of(
                List.of(), new StateAccessLog(), List.of(),
                new OrderDependencyReport(List.of(), Map.of(), Map.of(), Map.of(), Map.of()),
                List.of(), 0);

        assertThat(stats.testCount()).isZero();
        assertThat(stats.stateAccessCount()).isZero();
        assertThat(stats.couplingPairCount()).isZero();
        assertThat(stats.orderSensitiveTestCount()).isZero();
        assertThat(stats.isolationDiffCount()).isZero();
        assertThat(stats.ignoredCouplingCount()).isZero();
    }
}
