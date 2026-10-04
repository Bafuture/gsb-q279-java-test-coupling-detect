package com.example.gsb.coupling;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 耦合检测器：基于访问日志，若测试 A 写入某共享状态且测试 B 读取同一状态，
 * 则 {@code (A, B)} 构成一对潜在耦合。白名单中的耦合会被剔除。
 */
public final class CouplingDetector {

    public List<CouplingPair> detect(StateAccessLog log) {
        return detect(log, IgnoreList.empty());
    }

    public List<CouplingPair> detect(StateAccessLog log, IgnoreList ignoreList) {
        Map<StateKey, Set<String>> writersByState = groupByState(log, AccessMode.WRITE);
        Map<StateKey, Set<String>> readersByState = groupByState(log, AccessMode.READ);

        Set<CouplingPair> pairs = new TreeSet<>(
                Comparator.comparing(CouplingPair::writerTest)
                        .thenComparing(CouplingPair::readerTest)
                        .thenComparing(p -> p.state().toString()));

        writersByState.forEach((state, writers) -> {
            Set<String> readers = readersByState.getOrDefault(state, Set.of());
            for (String writer : writers) {
                for (String reader : readers) {
                    if (writer.equals(reader)) {
                        continue;
                    }
                    CouplingPair pair = new CouplingPair(writer, reader, state);
                    if (!ignoreList.isIgnored(pair)) {
                        pairs.add(pair);
                    }
                }
            }
        });
        return List.copyOf(pairs);
    }

    private Map<StateKey, Set<String>> groupByState(StateAccessLog log, AccessMode mode) {
        Map<StateKey, Set<String>> grouped = new HashMap<>();
        for (StateAccess access : log.all()) {
            if (access.mode() == mode) {
                grouped.computeIfAbsent(access.state(), key -> new TreeSet<>(Comparator.naturalOrder()))
                        .add(access.testId());
            }
        }
        return grouped;
    }

    /** 返回被至少一对耦合涉及的测试（写入方或读取方）。 */
    public Set<String> affectedTests(List<CouplingPair> pairs) {
        Set<String> affected = new HashSet<>();
        for (CouplingPair pair : pairs) {
            affected.add(pair.writerTest());
            affected.add(pair.readerTest());
        }
        return affected;
    }
}
