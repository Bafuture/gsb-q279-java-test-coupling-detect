package com.example.gsb.coupling;

import java.util.List;
import java.util.TreeSet;
import java.util.Set;

/**
 * 耦合检测报告：汇总耦合对、涉及状态、受影响测试、顺序依赖定位、
 * 隔离差异、统计信息与建议的隔离措施。
 */
public final class CouplingReport {

    private final List<CouplingPair> couplings;
    private final OrderDependencyReport orderReport;
    private final List<IsolationDiff> isolationDiffs;
    private final Statistics statistics;

    public CouplingReport(List<CouplingPair> couplings,
                          OrderDependencyReport orderReport,
                          List<IsolationDiff> isolationDiffs,
                          Statistics statistics) {
        this.couplings = List.copyOf(couplings);
        this.orderReport = orderReport;
        this.isolationDiffs = List.copyOf(isolationDiffs);
        this.statistics = statistics;
    }

    public List<CouplingPair> couplings() {
        return couplings;
    }

    public OrderDependencyReport orderReport() {
        return orderReport;
    }

    public List<IsolationDiff> isolationDiffs() {
        return isolationDiffs;
    }

    public Statistics statistics() {
        return statistics;
    }

    /** 渲染为纯文本报告。 */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 测试间耦合检测报告 =====\n\n");

        sb.append("1. 统计概览\n");
        sb.append("   - 测试数: ").append(statistics.testCount()).append('\n');
        sb.append("   - 状态访问次数: ").append(statistics.stateAccessCount()).append('\n');
        sb.append("   - 检测到的耦合对: ").append(statistics.couplingPairCount()).append('\n');
        sb.append("   - 顺序敏感测试数: ").append(statistics.orderSensitiveTestCount()).append('\n');
        sb.append("   - 隔离不一致测试数: ").append(statistics.isolationDiffCount()).append('\n');
        sb.append("   - 白名单忽略的耦合数: ").append(statistics.ignoredCouplingCount()).append('\n');
        sb.append('\n');

        sb.append("2. 潜在耦合对 (写入者 --[共享状态]--> 读取者)\n");
        if (couplings.isEmpty()) {
            sb.append("   （无）\n");
        } else {
            for (CouplingPair pair : couplings) {
                sb.append("   - ").append(pair.writerTest())
                        .append(" --[").append(pair.state()).append("]--> ")
                        .append(pair.readerTest()).append('\n');
            }
        }
        sb.append('\n');

        sb.append("3. 涉及的共享状态\n");
        Set<StateKey> states = new TreeSet<>((a, b) -> {
            int c = a.kind().name().compareTo(b.kind().name());
            return c != 0 ? c : a.name().compareTo(b.name());
        });
        for (CouplingPair pair : couplings) {
            states.add(pair.state());
        }
        if (states.isEmpty()) {
            sb.append("   （无）\n");
        } else {
            for (StateKey state : states) {
                sb.append("   - ").append(state).append('\n');
            }
        }
        sb.append('\n');

        sb.append("4. 受影响测试\n");
        Set<String> affected = new TreeSet<>();
        for (CouplingPair pair : couplings) {
            affected.add(pair.writerTest());
            affected.add(pair.readerTest());
        }
        if (affected.isEmpty()) {
            sb.append("   （无）\n");
        } else {
            for (String testId : affected) {
                sb.append("   - ").append(testId).append('\n');
            }
        }
        sb.append('\n');

        sb.append("5. 顺序依赖定位\n");
        if (!orderReport.hasOrderSensitiveTests()) {
            sb.append("   （未发现顺序敏感测试）\n");
        } else {
            sb.append("   顺序敏感测试: ").append(orderReport.orderSensitiveTests()).append('\n');
            for (String testId : orderReport.orderSensitiveTests()) {
                List<String> polluters = orderReport.polluters().getOrDefault(testId, List.of());
                List<String> missing = orderReport.missingDependencies().getOrDefault(testId, List.of());
                if (!polluters.isEmpty()) {
                    sb.append("   - ").append(testId).append(" 被 [").append(String.join(", ", polluters))
                            .append("] 污染（交换执行顺序后这些测试先写入了它读取的状态）\n");
                }
                if (!missing.isEmpty()) {
                    sb.append("   - ").append(testId).append(" 依赖前置 [").append(String.join(", ", missing))
                            .append("]（交换顺序后该写入者不再先于它执行）\n");
                }
            }
        }
        sb.append('\n');

        sb.append("6. 隔离验证差异\n");
        if (isolationDiffs.isEmpty()) {
            sb.append("   （共享与隔离执行结果一致）\n");
        } else {
            for (IsolationDiff diff : isolationDiffs) {
                sb.append("   - ").append(diff).append('\n');
            }
        }
        sb.append('\n');

        sb.append("7. 建议的隔离措施\n");
        if (states.isEmpty()) {
            sb.append("   （无需隔离措施）\n");
        } else {
            for (StateKey state : states) {
                sb.append("   - ").append(state.kind()).append(": ")
                        .append(suggestionFor(state.kind())).append('\n');
            }
        }
        sb.append('\n');

        return sb.toString();
    }

    static String suggestionFor(StateKind kind) {
        return switch (kind) {
            case STATIC_VARIABLE -> "在 @BeforeEach/@AfterEach 中重置静态字段，或重构为实例级状态";
            case SINGLETON -> "为单例提供 reset 钩子，或用依赖注入替代全局单例";
            case SYSTEM_PROPERTY -> "在测试前后保存并恢复系统属性（try/finally 或扩展），避免全局残留";
            case TEMP_FILE -> "使用 @TempDir 为每个测试分配独立临时目录，测试结束自动清理";
        };
    }
}
