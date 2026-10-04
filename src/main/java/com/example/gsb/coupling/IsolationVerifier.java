package com.example.gsb.coupling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 隔离验证器：先让所有测试在共享状态下连续执行一遍，再让每个测试各自从
 * 同一个（通常是空的）状态快照单独执行，对比两边结果。
 *
 * <p>共享时失败而隔离时通过，基本可以断定该测试被前面的测试污染。
 */
public final class IsolationVerifier {

    private final TestRunner runner;
    private final StateStore store;

    public IsolationVerifier(TestRunner runner, StateStore store) {
        this.runner = runner;
        this.store = store;
    }

    /**
     * @param tests             待验证测试
     * @param baselineSnapshot  独立执行前恢复到的状态快照（通常是干净状态）
     */
    public List<IsolationDiff> verify(List<? extends TestCase> tests, StateSnapshot baselineSnapshot) {
        Map<String, TestOutcome> shared = runner.runAll(tests);

        List<IsolationDiff> diffs = new ArrayList<>();
        for (TestCase test : tests) {
            store.restore(baselineSnapshot);
            TestOutcome isolated = runner.runOne(test);
            TestOutcome sharedOutcome = shared.get(test.id());
            if (sharedOutcome.passed() != isolated.passed()) {
                diffs.add(new IsolationDiff(test.id(), sharedOutcome, isolated));
            }
        }
        store.restore(baselineSnapshot);
        return List.copyOf(diffs);
    }
}
