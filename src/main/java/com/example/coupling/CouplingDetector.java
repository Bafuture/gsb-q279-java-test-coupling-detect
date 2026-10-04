package com.example.coupling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 耦合检测器：分析登记表中的访问记录，
 * 凡测试 A 写入某共享状态且测试 B 读取该状态，即标记为潜在耦合对。
 */
public final class CouplingDetector {

  public List<Coupling> detect(StateAccessRegistry registry, CouplingIgnoreList ignoreList) {
    Map<StateKey, Set<String>> writers = new LinkedHashMap<>();
    Map<StateKey, Set<String>> readers = new LinkedHashMap<>();
    for (StateAccess access : registry.allAccesses()) {
      Map<StateKey, Set<String>> target =
          access.type() == AccessType.WRITE ? writers : readers;
      target.computeIfAbsent(access.state(), k -> new LinkedHashSet<>()).add(access.testId());
    }

    List<Coupling> couplings = new ArrayList<>();
    for (Map.Entry<StateKey, Set<String>> entry : writers.entrySet()) {
      StateKey state = entry.getKey();
      for (String writer : entry.getValue()) {
        for (String reader : readers.getOrDefault(state, Set.of())) {
          if (writer.equals(reader)) {
            continue;
          }
          Coupling coupling = new Coupling(state, writer, reader);
          if (!ignoreList.isIgnored(coupling)) {
            couplings.add(coupling);
          }
        }
      }
    }
    return couplings;
  }
}
