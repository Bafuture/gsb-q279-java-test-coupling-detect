package com.example.coupling;

import java.util.List;

/**
 * 耦合检测统计信息。
 *
 * @param testCount               登记的测试数
 * @param totalAccessCount        共享状态访问总次数
 * @param readCount               读取次数
 * @param writeCount              写入次数
 * @param couplingPairCount       检测到的耦合对数（白名单除外）
 * @param orderSensitiveTestCount 顺序敏感测试数
 * @param isolationDifferenceCount 隔离/共享结果不一致的测试数
 */
public record CouplingStatistics(
    int testCount,
    int totalAccessCount,
    int readCount,
    int writeCount,
    int couplingPairCount,
    int orderSensitiveTestCount,
    int isolationDifferenceCount) {

  public static CouplingStatistics from(StateAccessRegistry registry,
                                        List<Coupling> couplings,
                                        OrderDependencyReport orderReport,
                                        IsolationReport isolationReport) {
    int reads = 0;
    int writes = 0;
    for (StateAccess access : registry.allAccesses()) {
      if (access.type() == AccessType.READ) {
        reads++;
      } else {
        writes++;
      }
    }
    return new CouplingStatistics(
        registry.registeredTestCount(),
        registry.allAccesses().size(),
        reads,
        writes,
        couplings.size(),
        orderReport.orderSensitiveTests().size(),
        isolationReport.differences().size());
  }
}
