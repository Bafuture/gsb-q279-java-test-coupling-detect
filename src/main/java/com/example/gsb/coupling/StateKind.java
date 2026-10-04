package com.example.gsb.coupling;

/**
 * 可被多个测试共享的状态类别。
 */
public enum StateKind {
    /** 静态变量（static field / static cache）。 */
    STATIC_VARIABLE,
    /** 单例对象内部的可变状态。 */
    SINGLETON,
    /** {@code System.getProperty/setProperty} 访问的系统属性。 */
    SYSTEM_PROPERTY,
    /** 写入文件系统的临时文件 / 临时目录。 */
    TEMP_FILE
}
