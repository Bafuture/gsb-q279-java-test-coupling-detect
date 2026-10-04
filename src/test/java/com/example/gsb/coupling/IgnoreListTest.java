package com.example.gsb.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IgnoreListTest {

    private static final StateKey CACHE = new StateKey(StateKind.STATIC_VARIABLE, "Cache");
    private static final StateKey OTHER = new StateKey(StateKind.STATIC_VARIABLE, "Other");

    private final StateAccessLog log = new StateAccessLog();
    private final CouplingDetector detector = new CouplingDetector();

    @Test
    void exactRuleRemovesOnlyTheMatchingCoupling() {
        log.record("writer", CACHE, AccessMode.WRITE);
        log.record("reader", CACHE, AccessMode.READ);
        log.record("writer", OTHER, AccessMode.WRITE);
        log.record("reader", OTHER, AccessMode.READ);

        IgnoreList ignoreList = IgnoreList.empty()
                .ignore("writer", "reader", StateKind.STATIC_VARIABLE, "Cache");

        assertThat(detector.detect(log, ignoreList))
                .containsExactly(new CouplingPair("writer", "reader", OTHER));
    }

    @Test
    void wildcardStarIsTreatedAsAnyValue() {
        log.record("writer", CACHE, AccessMode.WRITE);
        log.record("reader", CACHE, AccessMode.READ);

        IgnoreList ignoreList = IgnoreList.empty().ignore("*", "*", null, "*");

        assertThat(detector.detect(log, ignoreList)).isEmpty();
        assertThat(ignoreList.isIgnored(new CouplingPair("writer", "reader", CACHE))).isTrue();
    }

    @Test
    void ignoreAllBetweenIgnoresEveryStateForOnePair() {
        log.record("w", CACHE, AccessMode.WRITE);
        log.record("r", CACHE, AccessMode.READ);

        IgnoreList ignoreList = IgnoreList.empty().ignoreAllBetween("w", "r");

        assertThat(detector.detect(log, ignoreList)).isEmpty();
        assertThat(ignoreList.isIgnored(new CouplingPair("w", "r", OTHER))).isTrue();
        assertThat(ignoreList.isIgnored(new CouplingPair("w2", "r", CACHE))).isFalse();
    }

    @Test
    void ignoreStateSuppressesAllPairsTouchingThatState() {
        log.record("w", CACHE, AccessMode.WRITE);
        log.record("r", CACHE, AccessMode.READ);

        IgnoreList ignoreList = IgnoreList.empty().ignoreState(CACHE);

        assertThat(detector.detect(log, ignoreList)).isEmpty();
        assertThat(ignoreList.size()).isEqualTo(1);
    }
}
