package com.example.coupling;

import java.util.List;

/**
 * 一个顺序敏感测试：交换执行顺序后结果发生变化。
 *
 * @param polluters 有共享状态证据的污染源测试：在本测试失败的那一轮执行顺序中
 *                  先于本测试执行，且写入了本测试所读取的状态
 * @param suspects  在本测试失败的那一轮中先于本测试执行、但无直接状态证据的嫌疑测试
 */
public record OrderSensitiveTest(
    String testId,
    TestStatus baselineStatus,
    TestStatus swappedStatus,
    List<String> polluters,
    List<String> suspects) {

  public OrderSensitiveTest {
    polluters = List.copyOf(polluters);
    suspects = List.copyOf(suspects);
  }
}
