package com.example.coupling;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 默认状态快照器，可同时覆盖：
 * <ul>
 *   <li>已登记的 {@link StateHolder}（静态变量、单例字段）</li>
 *   <li>JVM 系统属性</li>
 *   <li>共享临时目录中的文件</li>
 * </ul>
 */
public final class DefaultStateSnapshotter implements StateSnapshotter<DefaultStateSnapshotter.Snapshot> {

  /** 一次捕获的完整状态快照。 */
  public static final class Snapshot {
    private final Map<StateHolder<?>, Object> holderValues;
    private final Properties systemProperties;
    private final Map<Path, byte[]> tempFiles;

    private Snapshot(Map<StateHolder<?>, Object> holderValues,
                     Properties systemProperties,
                     Map<Path, byte[]> tempFiles) {
      this.holderValues = holderValues;
      this.systemProperties = systemProperties;
      this.tempFiles = tempFiles;
    }
  }

  private final List<StateHolder<?>> holders;
  private final boolean includeSystemProperties;
  private final Path tempDirectory;

  public DefaultStateSnapshotter(List<StateHolder<?>> holders,
                                 boolean includeSystemProperties,
                                 Path tempDirectory) {
    this.holders = List.copyOf(holders);
    this.includeSystemProperties = includeSystemProperties;
    this.tempDirectory = tempDirectory;
  }

  public static DefaultStateSnapshotter systemPropertiesOnly() {
    return new DefaultStateSnapshotter(List.of(), true, null);
  }

  @Override
  public Snapshot capture() {
    Map<StateHolder<?>, Object> holderValues = new LinkedHashMap<>();
    for (StateHolder<?> holder : holders) {
      holderValues.put(holder, holder.captureValue());
    }
    Properties properties = null;
    if (includeSystemProperties) {
      properties = new Properties();
      properties.putAll(System.getProperties());
    }
    Map<Path, byte[]> tempFiles = null;
    if (tempDirectory != null) {
      tempFiles = captureTempFiles();
    }
    return new Snapshot(holderValues, properties, tempFiles);
  }

  @Override
  public void restore(Snapshot snapshot) {
    for (Map.Entry<StateHolder<?>, Object> entry : snapshot.holderValues.entrySet()) {
      entry.getKey().restoreValue(entry.getValue());
    }
    if (snapshot.systemProperties != null) {
      restoreSystemProperties(snapshot.systemProperties);
    }
    if (tempDirectory != null) {
      restoreTempFiles(snapshot.tempFiles);
    }
  }

  private Map<Path, byte[]> captureTempFiles() {
    Map<Path, byte[]> files = new LinkedHashMap<>();
    if (!Files.isDirectory(tempDirectory)) {
      return files;
    }
    try {
      Files.walkFileTree(tempDirectory, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {
          files.put(tempDirectory.relativize(file), Files.readAllBytes(file));
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException("failed to snapshot temp directory: " + tempDirectory, e);
    }
    return files;
  }

  private void restoreTempFiles(Map<Path, byte[]> files) {
    if (!Files.exists(tempDirectory)) {
      try {
        Files.createDirectories(tempDirectory);
      } catch (IOException e) {
        throw new UncheckedIOException("failed to create temp directory: " + tempDirectory, e);
      }
    }
    wipeDirectory(tempDirectory);
    for (Map.Entry<Path, byte[]> entry : files.entrySet()) {
      Path target = tempDirectory.resolve(entry.getKey());
      try {
        Files.createDirectories(target.getParent());
        Files.write(target, entry.getValue());
      } catch (IOException e) {
        throw new UncheckedIOException("failed to restore file: " + target, e);
      }
    }
  }

  private static void wipeDirectory(Path directory) {
    List<Path> paths;
    try (var stream = Files.walk(directory)) {
      paths = new ArrayList<>(stream.toList());
    } catch (IOException e) {
      throw new UncheckedIOException("failed to walk temp directory: " + directory, e);
    }
    paths.sort(Comparator.reverseOrder());
    for (Path path : paths) {
      if (path.equals(directory)) {
        continue;
      }
      try {
        Files.deleteIfExists(path);
      } catch (IOException e) {
        throw new UncheckedIOException("failed to delete: " + path, e);
      }
    }
  }

  private static void restoreSystemProperties(Properties snapshot) {
    Properties current = System.getProperties();
    List<String> extra = current.stringPropertyNames().stream()
        .filter(name -> !snapshot.containsKey(name))
        .toList();
    extra.forEach(current::remove);
    for (String name : snapshot.stringPropertyNames()) {
      System.setProperty(name, snapshot.getProperty(name));
    }
  }
}
