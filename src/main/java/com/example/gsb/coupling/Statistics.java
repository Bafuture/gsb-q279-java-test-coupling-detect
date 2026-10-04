package com.example.gsb.coupling;

import java.util.List;

/**
 * 框架统计信息。
 *
 * @param testCount               登记的测试数
 * @param stateAccessCount        共享状态访问总次数（读 + 写）
 * @param couplingPairCount       检测到的耦合对数（白名单过滤后）
 * @param orderSensitiveTestCount 顺序敏感测试数
 * @param isolationDiffCount      隔离结果不一致的测试数
 * @param ignoredCouplingCount    被白名单忽略的耦合数
 */
public record Statistics(int testCount,
                         int stateAccessCount,
                         int couplingPairCount,
                         int orderSensitiveTestCount,
                         int isolationDiffCount,
                         int ignoredCouplingCount) {

    public static Statistics of(List<? extends TestCase> tests,
                                StateAccessLog log,
                                List<CouplingPair> couplings,
                                OrderDependencyReport orderReport,
                                List<IsolationDiff> isolationDiffs,
                                int ignoredCouplingCount) {
        return new Statistics(
                tests.size(),
                log.accessCount(),
                couplings.size(),
                orderReport.orderSensitiveTests().size(),
                isolationDiffs.size(),
                ignoredCouplingCount);
    }
}
