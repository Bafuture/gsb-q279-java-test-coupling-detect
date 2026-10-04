package com.example.coupling;

/**
 * 共享状态快照器：捕获当前状态并在之后完整恢复，用于隔离验证。
 */
public interface StateSnapshotter<S> {

  S capture();

  void restore(S snapshot);
}
