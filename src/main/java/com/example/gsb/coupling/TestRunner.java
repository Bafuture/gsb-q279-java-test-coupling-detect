package com.example.gsb.coupling;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按给定顺序执行测试并捕获结果；执行前通过 {@link StateStore#enterTest}
 * 登记当前测试，使访问日志能归属到具体用例。
 */
public final class TestRunner {

    private final StateStore store;

    public TestRunner(StateStore store) {
        this.store = store;
    }

    StateStore store() {
        return store;
    }

    public TestOutcome runOne(TestCase test) {
        store.enterTest(test.id());
        try {
            test.run();
            return TestOutcome.pass();
        } catch (Throwable t) {
            return TestOutcome.fail(t.getMessage());
        }
    }

    /** 按列表顺序执行，返回保持执行顺序的结果表。 */
    public Map<String, TestOutcome> runAll(List<? extends TestCase> tests) {
        Map<String, TestOutcome> outcomes = new LinkedHashMap<>();
        for (TestCase test : tests) {
            outcomes.put(test.id(), runOne(test));
        }
        return outcomes;
    }
}
