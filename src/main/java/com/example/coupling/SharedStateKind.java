package com.example.coupling;

/**
 * 可被多个测试共享的状态类别。
 */
public enum SharedStateKind {
  STATIC_VARIABLE,
  SINGLETON,
  SYSTEM_PROPERTY,
  TEMP_FILE
}
