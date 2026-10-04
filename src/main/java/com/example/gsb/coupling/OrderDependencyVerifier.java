package com.example.gsb.coupling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 顺序依赖验证器：先按原始顺序执行一遍作为基线，再按逆序执行一遍。
 * 结果发生变化的测试即为顺序敏感测试；对新失败的测试，
 * 结合访问日志定位"是被谁污染"（或缺失了哪个前置写入者）。
 *
 * <p>注意：{@link #verify} 会清空并重建访问日志，以拿到干净的访问模型。
 */
public final class OrderDependencyVerifier {

    private final TestRunner runner;
    private final StateAccessLog log;

    public OrderDependencyVerifier(TestRunner runner, StateAccessLog log) {
        this.runner = runner;
        this.log = log;
    }

    public OrderDependencyReport verify(List<? extends TestCase> tests) {
        StateSnapshot cleanSnapshot = runner.store().snapshot();
        log.clear();
        Map<String, TestOutcome> baseline = runner.runAll(tests);
        AccessModel model = AccessModel.from(log);

        // 交换顺序的执行必须从与基线相同的初始状态出发，否则上一轮残留会干扰归因
        runner.store().restore(cleanSnapshot);
        List<TestCase> swapped = new ArrayList<>(tests);
        Collections.reverse(swapped);
        Map<String, TestOutcome> swappedOutcomes = runner.runAll(swapped);

        List<String> orderSensitive = new ArrayList<>();
        Map<String, List<String>> polluters = new LinkedHashMap<>();
        Map<String, List<String>> missingDependencies = new LinkedHashMap<>();

        Map<String, Integer> originalIndex = indexOf(tests);
        Map<String, Integer> swappedIndex = indexOf(swapped);

        for (TestCase test : tests) {
            String id = test.id();
            TestOutcome before = baseline.get(id);
            TestOutcome after = swappedOutcomes.get(id);
            if (before.passed() == after.passed()) {
                continue;
            }
            orderSensitive.add(id);
            if (before.passed()) {
                // 原来通过、交换后失败：定位污染者与缺失前置
                polluters.put(id, findAttributions(id, model, originalIndex, swappedIndex, true));
                missingDependencies.put(id, findAttributions(id, model, originalIndex, swappedIndex, false));
            }
        }
        return new OrderDependencyReport(
                List.copyOf(orderSensitive),
                polluters,
                missingDependencies,
                baseline,
                swappedOutcomes);
    }

    /**
     * 定位与失败测试 {@code testId} 存在写-读关系的写入者。
     *
     * @param pollution {@code true} 时找"交换后才跑到它前面"的写入者（污染源）；
     *                  {@code false} 时找"原本在它前面、交换后落到它后面"的写入者（缺失前置）。
     */
    private List<String> findAttributions(String testId, AccessModel model,
                                          Map<String, Integer> originalIndex,
                                          Map<String, Integer> swappedIndex,
                                          boolean pollution) {
        Set<StateKey> readsOfVictim = model.readsOf(testId);
        Set<String> suspects = new TreeSet<>();
        for (Map.Entry<String, Set<StateKey>> entry : model.writesByTest().entrySet()) {
            String writer = entry.getKey();
            if (writer.equals(testId) || Collections.disjoint(entry.getValue(), readsOfVictim)) {
                continue;
            }
            boolean beforeInOriginal = originalIndex.get(writer) < originalIndex.get(testId);
            boolean beforeInSwapped = swappedIndex.get(writer) < swappedIndex.get(testId);
            if (pollution ? (!beforeInOriginal && beforeInSwapped) : (beforeInOriginal && !beforeInSwapped)) {
                suspects.add(writer);
            }
        }
        return List.copyOf(suspects);
    }

    private static Map<String, Integer> indexOf(List<? extends TestCase> tests) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < tests.size(); i++) {
            index.put(tests.get(i).id(), i);
        }
        return index;
    }

    /** 从访问日志提炼出的静态读写模型。 */
    private record AccessModel(Map<String, Set<StateKey>> readsByTest,
                               Map<String, Set<StateKey>> writesByTest) {

        static AccessModel from(StateAccessLog log) {
            Map<String, Set<StateKey>> reads = new HashMap<>();
            Map<String, Set<StateKey>> writes = new HashMap<>();
            for (StateAccess access : log.all()) {
                Map<String, Set<StateKey>> target = access.mode() == AccessMode.READ ? reads : writes;
                target.computeIfAbsent(access.testId(), k -> new java.util.HashSet<>()).add(access.state());
            }
            return new AccessModel(reads, writes);
        }

        Set<StateKey> readsOf(String testId) {
            return readsByTest.getOrDefault(testId, Set.of());
        }
    }
}
