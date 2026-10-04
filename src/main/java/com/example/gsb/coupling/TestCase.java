package com.example.gsb.coupling;

/**
 * 可被框架调度执行的测试用例。{@link #run()} 抛出任何异常即视为失败。
 */
public interface TestCase {

    String id();

    void run() throws Exception;

    static TestCase of(String id, ThrowingRunnable body) {
        return new TestCase() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public void run() throws Exception {
                body.run();
            }

            @Override
            public String toString() {
                return id;
            }
        };
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
