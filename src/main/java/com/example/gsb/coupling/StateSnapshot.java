package com.example.gsb.coupling;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link StateStore} 在某一时刻的不可变内容快照，用于测试前后恢复状态。
 */
public final class StateSnapshot {

    private final Map<StateKey, Object> values;

    StateSnapshot(Map<StateKey, Object> values) {
        this.values = Map.copyOf(values);
    }

    Map<StateKey, Object> values() {
        return values;
    }

    public static StateSnapshot empty() {
        return new StateSnapshot(new HashMap<>());
    }
}
