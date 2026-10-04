package com.example.gsb.coupling;

import java.util.List;
import java.util.Map;

/**
 * 顺序依赖验证结果。
 *
 * @param orderSensitiveTests 交换顺序后结果发生变化的测试
 * @param polluters           每个新失败测试对应的污染者列表
 *                            （在失败顺序中先于它执行、且写入了它所读状态的测试）
 * @param missingDependencies 每个新失败测试对应的缺失前置列表
 *                            （在原顺序中先于它执行、交换后落到它之后的写入者）
 * @param baselineOutcomes    原始顺序下的执行结果
 * @param swappedOutcomes     交换顺序后的执行结果
 */
public record OrderDependencyReport(
        List<String> orderSensitiveTests,
        Map<String, List<String>> polluters,
        Map<String, List<String>> missingDependencies,
        Map<String, TestOutcome> baselineOutcomes,
        Map<String, TestOutcome> swappedOutcomes) {

    public boolean hasOrderSensitiveTests() {
        return !orderSensitiveTests.isEmpty();
    }
}
