package com.example.gsb.coupling;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 被测试代码共享、且可被框架追踪的状态存储。
 *
 * <p>实际接入时，它是对静态变量 / 单例 / 系统属性 / 临时文件的统一视图：
 * 每次 {@link #read} / {@link #write} 都会在 {@link StateAccessLog} 中留下一条访问记录，
 * 同时支持 {@link #snapshot()} / {@link #restore(StateSnapshot)} 以实现隔离验证。
 */
public final class StateStore {

    private final StateAccessLog log;
    private final Map<StateKey, Object> values = new HashMap<>();
    private String currentTestId = "<unregistered>";

    public StateStore(StateAccessLog log) {
        this.log = log;
    }

    /** 声明当前正在执行的测试，之后的读写都归到该测试名下。 */
    public void enterTest(String testId) {
        this.currentTestId = testId;
    }

    public void write(StateKind kind, String name, Object value) {
        write(new StateKey(kind, name), value);
    }

    public void write(StateKey state, Object value) {
        values.put(state, value);
        log.record(currentTestId, state, AccessMode.WRITE);
    }

    public Optional<Object> read(StateKind kind, String name) {
        return read(new StateKey(kind, name));
    }

    public Optional<Object> read(StateKey state) {
        log.record(currentTestId, state, AccessMode.READ);
        return Optional.ofNullable(values.get(state));
    }

    public StateSnapshot snapshot() {
        return new StateSnapshot(new HashMap<>(values));
    }

    public void restore(StateSnapshot snapshot) {
        values.clear();
        values.putAll(snapshot.values());
    }

    public void reset() {
        values.clear();
    }
}
