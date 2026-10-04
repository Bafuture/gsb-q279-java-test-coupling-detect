package com.example.coupling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** 顺序依赖定位：交换执行顺序后失败的测试能定位到污染源。 */
class OrderDependencyVerifierTest {

  private static final StateKey COUNTER =
      new StateKey(SharedStateKind.STATIC_VARIABLE, "counter");

  private final StateAccessRegistry registry = new StateAccessRegistry();
  private final AtomicInteger counter = new AtomicInteger(0);

  private TestCase writer() {
    return new TestCase("writer", () -> {
      registry.write(COUNTER.kind(), COUNTER.name());
      counter.set(42);
    });
  }

  private TestCase reader() {
    return new TestCase("reader", () -> {
      registry.read(COUNTER.kind(), COUNTER.name());
      if (counter.get() != 0) {
        throw new AssertionError("counter polluted: " + counter.get());
      }
    });
  }

  @Test
  void locatesPolluterAfterOrderSwap() {
    counter.set(0);
    OrderDependencyReport report =
        new OrderDependencyVerifier(registry).verify(List.of(reader(), writer()));

    assertThat(report.orderSensitiveTests()).hasSize(1);
    OrderSensitiveTest sensitive = report.orderSensitiveTests().get(0);
    assertThat(sensitive.testId()).isEqualTo("reader");
    assertThat(sensitive.baselineStatus()).isEqualTo(TestStatus.PASSED);
    assertThat(sensitive.swappedStatus()).isEqualTo(TestStatus.FAILED);
    assertThat(sensitive.polluters()).containsExactly("writer");
    assertThat(sensitive.suspects()).isEmpty();
  }

  @Test
  void independentTestsAreNotOrderSensitive() {
    TestCase a = new TestCase("a", () -> { });
    TestCase b = new TestCase("b", () -> { });

    OrderDependencyReport report =
        new OrderDependencyVerifier(registry).verify(List.of(a, b));

    assertThat(report.hasOrderSensitiveTests()).isFalse();
    assertThat(report.baselineResults()).allMatch(TestRunResult::isPassed);
    assertThat(report.swappedResults()).allMatch(TestRunResult::isPassed);
  }

  @Test
  void predecessorWithoutStateEvidenceIsListedAsSuspect() {
    TestCase noisy = new TestCase("noisy", () -> { });
    TestCase fragile = new TestCase("fragile", () -> {
      if (counter.get() != 0) {
        throw new AssertionError("polluted");
      }
    });
    TestCase invisibleWriter = new TestCase("invisibleWriter", () -> counter.set(1));

    counter.set(0);
    OrderDependencyReport report = new OrderDependencyVerifier(registry)
        .verify(List.of(noisy, fragile, invisibleWriter));

    assertThat(report.orderSensitiveTests()).hasSize(1);
    OrderSensitiveTest sensitive = report.orderSensitiveTests().get(0);
    assertThat(sensitive.testId()).isEqualTo("fragile");
    assertThat(sensitive.polluters()).isEmpty();
    assertThat(sensitive.suspects()).containsExactly("invisibleWriter");
  }

  @Test
  void recordsAccessesDuringBaselineRun() {
    counter.set(0);
    new OrderDependencyVerifier(registry).verify(List.of(reader(), writer()));

    assertThat(registry.statesWrittenBy("writer")).containsExactly(COUNTER);
    assertThat(registry.statesReadBy("reader")).containsExactly(COUNTER);
  }
}
