package com.example.gsb.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CouplingDetectorTest {

    private static final StateKey CACHE = new StateKey(StateKind.STATIC_VARIABLE, "com.foo.Cache#instance");
    private static final StateKey FLAG = new StateKey(StateKind.SYSTEM_PROPERTY, "feature.flag");

    private final StateAccessLog log = new StateAccessLog();
    private final CouplingDetector detector = new CouplingDetector();

    @Test
    void flagsWriteThenReadAcrossDifferentTests() {
        log.record("testA", CACHE, AccessMode.WRITE);
        log.record("testB", CACHE, AccessMode.READ);

        List<CouplingPair> pairs = detector.detect(log);

        assertThat(pairs).containsExactly(new CouplingPair("testA", "testB", CACHE));
    }

    @Test
    void doesNotFlagReadReadWriteWriteOrSelfAccess() {
        StateKey own = new StateKey(StateKind.SINGLETON, "com.foo.Own#state");
        log.record("testA", CACHE, AccessMode.READ);
        log.record("testB", CACHE, AccessMode.READ);
        log.record("testA", FLAG, AccessMode.WRITE);
        log.record("testB", FLAG, AccessMode.WRITE);
        log.record("testC", own, AccessMode.WRITE);
        log.record("testC", own, AccessMode.READ);

        assertThat(detector.detect(log)).isEmpty();
    }

    @Test
    void detectsMultiplePairsOnSameStateDeterministically() {
        log.record("writer1", FLAG, AccessMode.WRITE);
        log.record("writer2", FLAG, AccessMode.WRITE);
        log.record("reader1", FLAG, AccessMode.READ);
        log.record("reader2", FLAG, AccessMode.READ);

        assertThat(detector.detect(log)).containsExactly(
                new CouplingPair("writer1", "reader1", FLAG),
                new CouplingPair("writer1", "reader2", FLAG),
                new CouplingPair("writer2", "reader1", FLAG),
                new CouplingPair("writer2", "reader2", FLAG));
    }

    @Test
    void affectedTestsContainsBothSidesOfEachPair() {
        log.record("testA", CACHE, AccessMode.WRITE);
        log.record("testB", CACHE, AccessMode.READ);

        List<CouplingPair> pairs = detector.detect(log);

        assertThat(detector.affectedTests(pairs)).containsExactlyInAnyOrder("testA", "testB");
    }
}
