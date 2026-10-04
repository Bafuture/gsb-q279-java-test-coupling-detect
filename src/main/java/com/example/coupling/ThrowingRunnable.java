package com.example.coupling;

/**
 * 允许抛出异常的测试体。
 */
@FunctionalInterface
public interface ThrowingRunnable {
  void run() throws Exception;
}
