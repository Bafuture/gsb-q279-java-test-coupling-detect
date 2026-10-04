package com.example.coupling;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 耦合白名单：已确认无害的耦合登记在此，检测时不再告警。
 *
 * 支持三种粒度：
 * <ul>
 *   <li>精确耦合（状态 + 写入者 + 读取者）</li>
 *   <li>整个共享状态（该状态上的所有耦合）</li>
 *   <li>测试对（两个测试之间的所有耦合）</li>
 * </ul>
 */
public final class CouplingIgnoreList {

  private final Set<Coupling> exactCouplings = new LinkedHashSet<>();
  private final Set<StateKey> states = new LinkedHashSet<>();
  private final Set<String> testPairs = new LinkedHashSet<>();

  public void ignore(Coupling coupling) {
    exactCouplings.add(coupling);
  }

  public void ignoreState(StateKey state) {
    states.add(state);
  }

  public void ignorePair(String writerTest, String readerTest) {
    testPairs.add(pairKey(writerTest, readerTest));
  }

  public boolean isIgnored(Coupling coupling) {
    return exactCouplings.contains(coupling)
        || states.contains(coupling.state())
        || testPairs.contains(pairKey(coupling.writerTest(), coupling.readerTest()));
  }

  public int size() {
    return exactCouplings.size() + states.size() + testPairs.size();
  }

  private static String pairKey(String writerTest, String readerTest) {
    return writerTest + "->" + readerTest;
  }
}
