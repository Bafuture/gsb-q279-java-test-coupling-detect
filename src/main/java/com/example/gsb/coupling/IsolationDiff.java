package com.example.gsb.coupling;

/**
 * 隔离验证差异：同一测试在共享状态下与独立状态快照下结果不一致。
 *
 * @param testId          测试标识
 * @param sharedOutcome   共享状态（与其它测试连续执行）下的结果
 * @param isolatedOutcome 独立状态快照下的结果
 */
public record IsolationDiff(String testId, TestOutcome sharedOutcome, TestOutcome isolatedOutcome) {

    @Override
    public String toString() {
        return testId + ": shared=" + sharedOutcome + ", isolated=" + isolatedOutcome;
    }
}
