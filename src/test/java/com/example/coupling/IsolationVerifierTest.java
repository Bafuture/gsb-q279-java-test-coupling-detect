package com.example.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 隔离对比：独立快照下运行与共享状态下运行的结果差异。 */
class IsolationVerifierTest {

  private static final StateKey COUNTER =
      new StateKey(SharedStateKind.STATIC_VARIABLE, "counter");

  private final AtomicInteger counter = new AtomicInteger(0);

  private DefaultStateSnapshotter snapshotter() {
    StateHolder<Integer> holder =
        new StateHolder<>(COUNTER, counter::get, counter::set);
    return new DefaultStateSnapshotter(List.of(holder), false, null);
  }

  @Test
  void detectsDifferenceBetweenIsolatedAndSharedRuns() {
    TestCase writer = new TestCase("writer", () -> counter.set(7));
    TestCase reader = new TestCase("reader", () -> {
      if (counter.get() != 0) {
        throw new AssertionError("expected 0 but was " + counter.get());
      }
    });

    IsolationReport report =
        new IsolationVerifier(snapshotter()).verify(List.of(writer, reader));

    assertThat(report.differences()).hasSize(1);
    IsolationDifference difference = report.differences().get(0);
    assertThat(difference.testId()).isEqualTo("reader");
    assertThat(difference.isolatedStatus()).isEqualTo(TestStatus.PASSED);
    assertThat(difference.sharedStatus()).isEqualTo(TestStatus.FAILED);
    assertThat(difference.sharedErrorMessage()).contains("expected 0");
  }

  @Test
  void compatibleTestsProduceNoDifference() {
    TestCase a = new TestCase("a", () -> counter.incrementAndGet());
    TestCase b = new TestCase("b", () -> counter.incrementAndGet());

    IsolationReport report =
        new IsolationVerifier(snapshotter()).verify(List.of(a, b));

    assertThat(report.hasDifferences()).isFalse();
  }

  @Test
  void restoresOriginalStateAfterVerification() {
    counter.set(5);
    TestCase mutator = new TestCase("mutator", () -> counter.set(99));

    new IsolationVerifier(snapshotter()).verify(List.of(mutator));

    assertThat(counter.get()).isEqualTo(5);
  }

  @Test
  void snapshotsAndRestoresSystemProperties() {
    System.clearProperty("coupling.test.prop");
    TestCase setter = new TestCase("setter",
        () -> System.setProperty("coupling.test.prop", "dirty"));

    new IsolationVerifier(DefaultStateSnapshotter.systemPropertiesOnly())
        .verify(List.of(setter));

    assertThat(System.getProperty("coupling.test.prop")).isNull();
  }

  @Test
  void snapshotsAndRestoresTempDirectory(@TempDir Path tempDir) throws Exception {
    Path shared = tempDir.resolve("shared");
    Files.createDirectories(shared);
    Files.writeString(shared.resolve("seed.txt"), "seed");

    DefaultStateSnapshotter snapshotter =
        new DefaultStateSnapshotter(List.of(), false, shared);
    TestCase messy = new TestCase("messy", () -> {
      Files.writeString(shared.resolve("extra.txt"), "garbage");
      Files.writeString(shared.resolve("seed.txt"), "overwritten");
    });

    new IsolationVerifier(snapshotter).verify(List.of(messy));

    assertThat(shared.resolve("extra.txt")).doesNotExist();
    assertThat(Files.readString(shared.resolve("seed.txt"))).isEqualTo("seed");
  }
}
