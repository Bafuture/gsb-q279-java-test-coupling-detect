package com.example.gsb.coupling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 共享状态访问日志。测试运行期间所有 {@link StateAccess} 都登记到这里，
 * 是耦合识别的数据来源。
 */
public final class StateAccessLog {

    private final List<StateAccess> accesses = new ArrayList<>();

    public void record(StateAccess access) {
        accesses.add(access);
    }

    public void record(String testId, StateKey state, AccessMode mode) {
        record(new StateAccess(testId, state, mode));
    }

    public List<StateAccess> all() {
        return Collections.unmodifiableList(accesses);
    }

    public List<StateAccess> forTest(String testId) {
        return accesses.stream()
                .filter(a -> a.testId().equals(testId))
                .collect(Collectors.toList());
    }

    public int accessCount() {
        return accesses.size();
    }

    public void clear() {
        accesses.clear();
    }
}
