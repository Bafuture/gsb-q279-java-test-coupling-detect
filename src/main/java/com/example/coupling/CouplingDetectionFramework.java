package com.example.coupling;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试间耦合检测框架门面：一次分析即完成访问登记、耦合检测、
 * 顺序依赖验证、隔离验证、统计与报告。
 *
 * <pre>{@code
 * CouplingDetectionFramework framework = new CouplingDetectionFramework();
 * framework.registerHolder(new StateHolder<>(key, MyClass::getValue, MyClass::setValue));
 * CouplingReport report = framework.analyze(List.of(testA, testB));
 * System.out.println(report.render());
 * }</pre>
 */
public final class CouplingDetectionFramework {

  private final StateAccessRegistry registry = new StateAccessRegistry();
  private final CouplingIgnoreList ignoreList = new CouplingIgnoreList();
  private final List<StateHolder<?>> holders = new ArrayList<>();
  private boolean includeSystemProperties = true;
  private Path tempDirectory;

  /** 登记一个可快照的共享状态（静态变量 / 单例字段）。 */
  public <T> CouplingDetectionFramework registerHolder(StateHolder<T> holder) {
    holders.add(holder);
    return this;
  }

  /** 是否在隔离验证中快照并恢复系统属性，默认开启。 */
  public CouplingDetectionFramework includeSystemProperties(boolean include) {
    this.includeSystemProperties = include;
    return this;
  }

  /** 指定参与隔离验证的共享临时目录。 */
  public CouplingDetectionFramework tempDirectory(Path directory) {
    this.tempDirectory = directory;
    return this;
  }

  public StateAccessRegistry registry() {
    return registry;
  }

  public CouplingIgnoreList ignoreList() {
    return ignoreList;
  }

  /**
   * 对给定测试集执行完整分析：
   * 原始/交换顺序验证、隔离/共享对比、耦合检测，最终产出报告。
   */
  public CouplingReport analyze(List<TestCase> tests) {
    registry.clear();

    DefaultStateSnapshotter snapshotter =
        new DefaultStateSnapshotter(holders, includeSystemProperties, tempDirectory);

    OrderDependencyVerifier orderVerifier =
        new OrderDependencyVerifier(registry, snapshotter);
    OrderDependencyReport orderReport = orderVerifier.verify(tests);

    IsolationReport isolationReport =
        new IsolationVerifier(snapshotter, registry).verify(tests);

    List<Coupling> couplings =
        new CouplingDetector().detect(registry, ignoreList);

    CouplingStatistics statistics =
        CouplingStatistics.from(registry, couplings, orderReport, isolationReport);

    return new CouplingReport(couplings, orderReport, isolationReport,
        statistics, ignoreList.size());
  }
}
