package com.example.coupling;

/**
 * 一次共享状态访问记录：哪个测试、以何种方式、访问了哪个状态。
 * sequence 记录访问的全局先后顺序。
 */
public record StateAccess(String testId, StateKey state, AccessType type, long sequence) {

  public StateAccess {
    if (testId == null || testId.isBlank()) {
      throw new IllegalArgumentException("testId must not be blank");
    }
    if (state == null) {
      throw new IllegalArgumentException("state must not be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
  }
}
