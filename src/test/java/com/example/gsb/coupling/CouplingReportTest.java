package com.example.gsb.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CouplingReportTest {

    private static final StateKey CACHE = new StateKey(StateKind.STATIC_VARIABLE, "Cache");

    private final StateAccessLog log = new StateAccessLog();
    private final StateStore store = new StateStore(log);
    private final TestRunner runner = new TestRunner(store);

    private final TestCase victim = TestCase.of("victimExpectsCleanCache", () -> {
        if (store.read(CACHE).isPresent()) {
            throw new AssertionError("cache polluted");
        }
    });
    private final TestCase polluter = TestCase.of("polluterLeavesDirtyCache",
            () -> store.write(CACHE, "dirty"));

    @Test
    void rendersCouplingsStatesAffectedTestsAttributionDiffsAndSuggestions() {
        List<TestCase> tests = List.of(victim, polluter);
        OrderDependencyReport orderReport =
                new OrderDependencyVerifier(runner, log).verify(tests);
        List<CouplingPair> couplings = new CouplingDetector().detect(log);
        List<IsolationDiff> diffs = new IsolationVerifier(runner, store)
                .verify(List.of(polluter, victim), StateSnapshot.empty());
        Statistics statistics = Statistics.of(tests, log, couplings, orderReport, diffs, 0);

        String rendered = new CouplingReport(couplings, orderReport, diffs, statistics).render();

        assertThat(rendered)
                .contains("潜在耦合对")
                .contains("polluterLeavesDirtyCache --[STATIC_VARIABLE:Cache]--> victimExpectsCleanCache")
                .contains("涉及的共享状态")
                .contains("STATIC_VARIABLE:Cache")
                .contains("受影响测试")
                .contains("顺序敏感测试")
                .contains("victimExpectsCleanCache 被 [polluterLeavesDirtyCache] 污染")
                .contains("隔离验证差异")
                .contains("victimExpectsCleanCache: shared=FAIL")
                .contains("isolated=PASS")
                .contains("建议的隔离措施")
                .contains("重置静态字段")
                .contains("测试数: 2");
    }

    @Test
    void rendersEmptySectionsWhenNothingDetected() {
        OrderDependencyReport emptyOrder = new OrderDependencyReport(
                List.of(), Map.of(), Map.of(), Map.of(), Map.of());
        Statistics statistics = new Statistics(0, 0, 0, 0, 0, 0);

        String rendered = new CouplingReport(List.of(), emptyOrder, List.of(), statistics).render();

        assertThat(rendered)
                .contains("（无）")
                .contains("未发现顺序敏感测试")
                .contains("共享与隔离执行结果一致")
                .contains("无需隔离措施");
    }

    @Test
    void suggestionsCoverEveryStateKind() {
        assertThat(CouplingReport.suggestionFor(StateKind.STATIC_VARIABLE)).contains("静态字段");
        assertThat(CouplingReport.suggestionFor(StateKind.SINGLETON)).contains("单例");
        assertThat(CouplingReport.suggestionFor(StateKind.SYSTEM_PROPERTY)).contains("系统属性");
        assertThat(CouplingReport.suggestionFor(StateKind.TEMP_FILE)).contains("@TempDir");
    }
}
