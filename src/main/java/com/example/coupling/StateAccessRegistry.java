package com.example.coupling;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试共享状态访问登记表。
 *
 * 测试执行期间通过 {@link #read(SharedStateKind, String)} /
 * {@link #write(SharedStateKind, String)} 登记对共享状态的访问，
 * 访问会归属到当前线程正在执行的测试用例，形成测试 -> 状态的依赖关系。
 */
public final class StateAccessRegistry {

  private final List<StateAccess> accesses = new CopyOnWriteArrayList<>();
  private final Set<String> testIds = new LinkedHashSet<>();
  private final AtomicLong sequence = new AtomicLong();
  private final ThreadLocal<String> currentTest = new ThreadLocal<>();
  private volatile boolean recording = true;

  /** 标记当前线程开始执行指定测试，之后登记的访问都归属该测试。 */
  public void beginTest(String testId) {
    if (testId == null || testId.isBlank()) {
      throw new IllegalArgumentException("testId must not be blank");
    }
    synchronized (testIds) {
      testIds.add(testId);
    }
    currentTest.set(testId);
  }

  /** 标记当前线程的测试执行结束。 */
  public void endTest() {
    currentTest.remove();
  }

  /** 登记一次读取访问。 */
  public void read(SharedStateKind kind, String name) {
    record(kind, name, AccessType.READ);
  }

  /** 登记一次写入访问。 */
  public void write(SharedStateKind kind, String name) {
    record(kind, name, AccessType.WRITE);
  }

  private void record(SharedStateKind kind, String name, AccessType type) {
    if (!recording) {
      return;
    }
    String testId = currentTest.get();
    if (testId == null) {
      throw new IllegalStateException("no active test; call beginTest(...) first");
    }
    accesses.add(new StateAccess(testId, new StateKey(kind, name), type,
        sequence.getAndIncrement()));
  }

  public List<StateAccess> allAccesses() {
    return List.copyOf(accesses);
  }

  public List<StateAccess> accessesOf(String testId) {
    return accesses.stream().filter(a -> a.testId().equals(testId)).toList();
  }

  public Set<StateKey> statesReadBy(String testId) {
    return statesOf(testId, AccessType.READ);
  }

  public Set<StateKey> statesWrittenBy(String testId) {
    return statesOf(testId, AccessType.WRITE);
  }

  private Set<StateKey> statesOf(String testId, AccessType type) {
    LinkedHashSet<StateKey> result = new LinkedHashSet<>();
    for (StateAccess access : accesses) {
      if (access.testId().equals(testId) && access.type() == type) {
        result.add(access.state());
      }
    }
    return result;
  }

  public List<String> registeredTests() {
    synchronized (testIds) {
      return List.copyOf(testIds);
    }
  }

  public int registeredTestCount() {
    synchronized (testIds) {
      return testIds.size();
    }
  }

  public void clear() {
    accesses.clear();
    synchronized (testIds) {
      testIds.clear();
    }
    sequence.set(0);
  }

  /** 暂停/恢复访问记录（测试上下文仍然有效，用于重复执行同一测试时避免重复登记）。 */
  public void setRecording(boolean recording) {
    this.recording = recording;
  }

  public boolean isRecording() {
    return recording;
  }
}
