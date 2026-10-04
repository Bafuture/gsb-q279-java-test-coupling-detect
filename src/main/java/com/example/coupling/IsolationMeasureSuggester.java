package com.example.coupling;

/**
 * 根据共享状态类别给出建议的隔离措施。
 */
public final class IsolationMeasureSuggester {

  private IsolationMeasureSuggester() {
  }

  public static String suggest(SharedStateKind kind) {
    return switch (kind) {
      case STATIC_VARIABLE ->
          "在 @BeforeEach/@AfterEach 中重置静态变量，或将其重构为实例状态/依赖注入";
      case SINGLETON ->
          "为单例提供 reset() 方法并在每个测试后调用，或通过依赖注入替换为独立实例";
      case SYSTEM_PROPERTY ->
          "测试前备份并在测试后恢复系统属性（例如使用 JUnit 5 的 RestoreSystemProperties 扩展）";
      case TEMP_FILE ->
          "为每个测试使用独立临时目录（例如 JUnit 5 的 @TempDir），避免共享文件路径";
    };
  }
}
