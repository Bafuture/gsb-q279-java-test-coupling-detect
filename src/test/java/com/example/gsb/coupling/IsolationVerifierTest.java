package com.example.gsb.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class IsolationVerifierTest {

    private static final StateKey SINGLETON =
            new StateKey(StateKind.SINGLETON, "com.foo.Registry#INSTANCE");

    @Test
    void showsDiffWhenTestOnlyFailsUnderSharedState() {
        StateAccessLog log = new StateAccessLog();
        StateStore store = new StateStore(log);
        TestRunner runner = new TestRunner(store);

        TestCase polluter = TestCase.of("polluterRegistersInstance",
                () -> store.write(SINGLETON, "leaked"));
        TestCase victim = TestCase.of("victimExpectsEmptyRegistry", () -> {
            if (store.read(SINGLETON).isPresent()) {
                throw new AssertionError("registry must be empty");
            }
        });

        List<IsolationDiff> diffs = new IsolationVerifier(runner, store)
                .verify(List.of(polluter, victim), StateSnapshot.empty());

        assertThat(diffs).hasSize(1);
        IsolationDiff diff = diffs.get(0);
        assertThat(diff.testId()).isEqualTo("victimExpectsEmptyRegistry");
        assertThat(diff.sharedOutcome().passed()).isFalse();
        assertThat(diff.isolatedOutcome().passed()).isTrue();
        assertThat(diff.toString()).contains("shared=FAIL", "isolated=PASS");
    }

    @Test
    void reportsNoDiffWhenIsolatedAndSharedAgree() {
        StateAccessLog log = new StateAccessLog();
        StateStore store = new StateStore(log);
        TestRunner runner = new TestRunner(store);

        TestCase independent1 = TestCase.of("t1",
                () -> store.write(new StateKey(StateKind.TEMP_FILE, "1.txt"), "a"));
        TestCase independent2 = TestCase.of("t2",
                () -> store.write(new StateKey(StateKind.TEMP_FILE, "2.txt"), "b"));

        List<IsolationDiff> diffs = new IsolationVerifier(runner, store)
                .verify(List.of(independent1, independent2), StateSnapshot.empty());

        assertThat(diffs).isEmpty();
        assertThat(store.read(new StateKey(StateKind.TEMP_FILE, "1.txt"))).isEmpty();
    }
}
