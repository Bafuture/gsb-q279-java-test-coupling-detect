package com.example.gsb.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OrderDependencyVerifierTest {

    private static final StateKey CACHE = new StateKey(StateKind.STATIC_VARIABLE, "com.foo.Cache#dirty");
    private static final StateKey FLAG = new StateKey(StateKind.STATIC_VARIABLE, "com.foo.Config#flag");

    @Test
    void locatesPolluterWhenSwappingOrderBreaksReader() {
        StateAccessLog log = new StateAccessLog();
        StateStore store = new StateStore(log);
        TestRunner runner = new TestRunner(store);

        TestCase reader = TestCase.of("readerExpectsCleanCache", () -> {
            Object dirty = store.read(CACHE).orElse(null);
            if (dirty != null) {
                throw new AssertionError("cache must be clean but was " + dirty);
            }
        });
        TestCase polluter = TestCase.of("polluterLeavesDirtyCache",
                () -> store.write(CACHE, "dirty"));
        TestCase independent = TestCase.of("independentTest", () -> {
            store.write(new StateKey(StateKind.TEMP_FILE, "own.txt"), "ok");
            assertThat(store.read(new StateKey(StateKind.TEMP_FILE, "own.txt"))).isPresent();
        });

        List<TestCase> originalOrder = List.of(reader, polluter, independent);
        OrderDependencyReport report =
                new OrderDependencyVerifier(runner, log).verify(originalOrder);

        assertThat(report.baselineOutcomes().get("readerExpectsCleanCache").passed()).isTrue();
        assertThat(report.swappedOutcomes().get("readerExpectsCleanCache").passed()).isFalse();
        assertThat(report.orderSensitiveTests()).containsExactly("readerExpectsCleanCache");
        assertThat(report.polluters().get("readerExpectsCleanCache"))
                .containsExactly("polluterLeavesDirtyCache");
        assertThat(report.missingDependencies().get("readerExpectsCleanCache")).isEmpty();
    }

    @Test
    void locatesMissingSetupWhenWriterMovesAfterReader() {
        StateAccessLog log = new StateAccessLog();
        StateStore store = new StateStore(log);
        TestRunner runner = new TestRunner(store);

        TestCase setup = TestCase.of("setupWritesFlag", () -> store.write(FLAG, "on"));
        TestCase dependent = TestCase.of("dependentNeedsFlag", () -> {
            String value = (String) store.read(FLAG).orElse(null);
            if (!"on".equals(value)) {
                throw new AssertionError("flag not set");
            }
        });
        TestCase other = TestCase.of("unrelatedTest", () -> store.write(
                new StateKey(StateKind.SINGLETON, "other"), 1));

        OrderDependencyReport report =
                new OrderDependencyVerifier(runner, log).verify(List.of(setup, dependent, other));

        assertThat(report.orderSensitiveTests()).containsExactly("dependentNeedsFlag");
        assertThat(report.polluters().get("dependentNeedsFlag")).isEmpty();
        assertThat(report.missingDependencies().get("dependentNeedsFlag"))
                .containsExactly("setupWritesFlag");
    }

    @Test
    void reportsNothingWhenTestsAreOrderIndependent() {
        StateAccessLog log = new StateAccessLog();
        StateStore store = new StateStore(log);
        TestRunner runner = new TestRunner(store);

        TestCase a = TestCase.of("a", () -> store.write(
                new StateKey(StateKind.TEMP_FILE, "a.txt"), 1));
        TestCase b = TestCase.of("b", () -> store.write(
                new StateKey(StateKind.TEMP_FILE, "b.txt"), 2));

        OrderDependencyReport report =
                new OrderDependencyVerifier(runner, log).verify(List.of(a, b));

        assertThat(report.hasOrderSensitiveTests()).isFalse();
        assertThat(report.orderSensitiveTests()).isEmpty();
    }
}
