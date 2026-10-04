package com.example.coupling;

/**
 * 一对潜在耦合：writerTest 写入了某共享状态，readerTest 读取了同一状态。
 */
public record Coupling(StateKey state, String writerTest, String readerTest) {

  public Coupling {
    if (state == null || writerTest == null || readerTest == null) {
      throw new IllegalArgumentException("coupling fields must not be null");
    }
    if (writerTest.equals(readerTest)) {
      throw new IllegalArgumentException("writer and reader must be different tests");
    }
  }

  @Override
  public String toString() {
    return writerTest + " --[" + state.kind() + " '" + state.name() + "']--> " + readerTest;
  }
}
