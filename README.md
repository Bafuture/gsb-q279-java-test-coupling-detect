# 测试间耦合检测框架

Pair-wise GSB 标注任务仓库（第 17 批 / 279）。

| 项目 | 内容 |
|------|------|
| 任务类型 | Feature 迭代 |
| 任务难度 | 困难 |
| 语言/框架 | Java, Maven, JUnit 5 |
| 环境可复现等级 | 无外部依赖 |
| 构建方式 | Maven（含 mvnw wrapper，无需本机安装 Maven） |

> 本仓库是**初始环境快照**：只有工程骨架，不含任何实现代码。
> 分支说明：`main` 为初始环境；`A`、`B` 为两次独立执行各自的工作分支，均从 `main` 的同一个提交拉出。

## 运行方式

```bash
./mvnw -q verify
```

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的测试单独跑全绿，一起跑就偶发失败，怀疑测试之间互相污染。请从零实现一个测试间耦合检测框架。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持登记测试用例及其共享状态访问（静态变量、单例、系统属性、临时文件），形成依赖关系；2) 支持检测耦合：若测试 A 写入某共享状态且测试 B 读取它，则标记为潜在耦合；3) 支持顺序依赖验证：交换执行顺序后失败的测试要能定位到"是被谁污染"；4) 支持隔离验证：每个测试在独立状态快照下运行与在共享状态下的结果对比，输出差异；5) 支持报告：耦合对、涉及的状态、受影响测试与建议的隔离措施；6) 支持忽略清单：已确认无害的耦合可加入白名单，不参与告警；7) 提供统计：测试数、状态访问次数、检测到的耦合对与顺序敏感测试数；8) 测试覆盖耦合识别、顺序依赖定位、隔离对比、报告内容、白名单与统计；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。

---

## 框架使用说明（本次实现）

源码位于 `src/main/java/com/example/coupling/`，核心入口为 `CouplingDetectionFramework`：

```java
CouplingDetectionFramework framework = new CouplingDetectionFramework()
    .registerHolder(new StateHolder<>(
        new StateKey(SharedStateKind.STATIC_VARIABLE, "counter"),
        MyClass::getCounter, MyClass::setCounter))   // 静态变量/单例接入快照
    .tempDirectory(sharedTempDir)                    // 可选：共享临时目录
    .includeSystemProperties(true);                  // 可选：系统属性快照（默认开）

TestCase a = new TestCase("testA", () -> {
  framework.registry().write(SharedStateKind.SYSTEM_PROPERTY, "app.mode");
  // ...测试逻辑...
});
TestCase b = new TestCase("testB", () -> {
  framework.registry().read(SharedStateKind.SYSTEM_PROPERTY, "app.mode");
  // ...测试逻辑...
});

CouplingReport report = framework.analyze(List.of(a, b));
System.out.println(report.render());
```

### 能力一览

| 能力 | 类 |
|------|-----|
| 登记测试与共享状态访问（静态变量/单例/系统属性/临时文件） | `StateAccessRegistry`, `StateKey`, `StateAccess` |
| 耦合检测（A 写、B 读即潜在耦合） | `CouplingDetector`, `Coupling` |
| 顺序依赖验证与污染源定位 | `OrderDependencyVerifier`, `OrderSensitiveTest` |
| 隔离验证（独立快照 vs 共享状态对比） | `IsolationVerifier`, `DefaultStateSnapshotter`, `StateHolder` |
| 报告（耦合对/涉及状态/受影响测试/隔离建议） | `CouplingReport`, `IsolationMeasureSuggester` |
| 忽略清单（白名单） | `CouplingIgnoreList` |
| 统计（测试数/访问次数/耦合对/顺序敏感数） | `CouplingStatistics` |

### 运行

```bash
mvn -q verify   # 或 ./mvnw -q verify
```
