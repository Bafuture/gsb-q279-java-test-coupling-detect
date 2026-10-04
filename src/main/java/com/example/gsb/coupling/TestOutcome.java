package com.example.gsb.coupling;

/** 一次测试执行的结果。 */
public record TestOutcome(boolean passed, String failureMessage) {

    public static TestOutcome pass() {
        return new TestOutcome(true, null);
    }

    public static TestOutcome fail(String message) {
        return new TestOutcome(false, message == null ? "<no message>" : message);
    }

    @Override
    public String toString() {
        return passed ? "PASS" : "FAIL(" + failureMessage + ")";
    }
}
