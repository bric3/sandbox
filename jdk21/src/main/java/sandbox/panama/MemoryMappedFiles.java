/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox.panama;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout.OfByte;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jdk.jfr.consumer.RecordingStream;

public class MemoryMappedFiles {

  public static void main(String[] args) throws InterruptedException {
    System.setProperty("java.util.logging.SimpleFormatter.format",
                       "[%1$tT] [%4$-7s] %5$s %n");

    String mode = null;
    List<Path> paths = null;
    switch (args.length) {
      case 0, 1 -> {
        System.err.println("""
        Missing parameters.
        Usage: MemoryMappedFiles <mmap|panama> <files>
        """);
        System.exit(1);
      }
      default -> {
        mode = args[0].toLowerCase();
        paths = Arrays.stream(args)
                      .skip(1)
                      .map(s -> s.replace("$HOME", System.getProperty("user.home")))
                      .map(Path::of)
                      .toList();
      }
    }

    logInfo("mode: " + mode + ", files: " + paths);
    try (var rs = configureJfrStream(true)) {
      logInfo("Starting");
      for (var i = 0; i < 100; i++) {
        String _mode = mode;
        paths.forEach(p -> {
          switch (_mode) {
            case "mbb" -> useMappedByteBuffer(p);
            case "panama" -> useMemorySegment(p);
          }
        });

        TimeUnit.MILLISECONDS.sleep(300);
      }
      System.gc();

      logInfo("Done");
    }
  }

  private static RecordingStream configureJfrStream(boolean startAsync) {
    logInfo("Configuring JFR stream");
    var rs = new RecordingStream();
    var _1sec = Duration.ofSeconds(1);
    var _2secs = Duration.ofSeconds(2);
    rs.enable("jdk.GarbageCollection").withPeriod(_1sec);
    rs.enable("jdk.GCHeapSummary").withPeriod(_1sec);
    rs.enable("jdk.PhysicalMemory").withPeriod(_2secs);
    // rs.enable("jdk.DirectBufferStatistics").withPeriod(_2secs);
    rs.enable("jdk.SystemGC").withPeriod(_1sec);
    rs.onEvent("jdk.GarbageCollection", event -> logInfo("GC   " + event.getInt("gcId") + " : " + event.getString("name") + " : " + event.getString("cause")));
    rs.onEvent("jdk.GCHeapSummary", event -> logInfo("Heap " + event.getInt("gcId") + " : " + event.getString("when") + " : " + event.getLong("heapUsed") + " : " + event.getLong("heapSpace.committedSize")));
    rs.onEvent("jdk.PhysicalMemory", event -> logInfo("Mem  " + event.getLong("usedSize") + " / " + event.getLong("totalSize")));
    rs.onEvent("jdk.SystemGC", event -> logInfo("sgc  "));
    // rs.onEvent("jdk.DirectBufferStatistics", event -> logInfo("dbuf " + event.getLong("memoryUsed") + " / " + event.getLong("totalCapacity")));

    if (startAsync) {
      rs.startAsync();
    }

    return rs;
  }

  private static void logInfo(Object monitorClass) {
    System.getLogger("main").log(Level.INFO, monitorClass);
  }


  static void useMappedByteBuffer(Path src) {
    try (var fileChannel = FileChannel.open(src, StandardOpenOption.READ)) {
      // A mapped byte buffer and the file mapping that it represents remain
      // valid until the buffer itself is garbage-collected.
      var mappedByteBuffer = fileChannel.map(
              FileChannel.MapMode.READ_ONLY,
              0,
              fileChannel.size());

      var position = 0;
      var size = 8192;

      while (position + size < fileChannel.size()) {
        // then get bytes from buffer
        var bytes = new byte[size];
        mappedByteBuffer.get(bytes);

        position += size;
        size = (int) Math.min(size, fileChannel.size() - position);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static void useMemorySegment(Path src) {
    try (var channel = FileChannel.open(src);
         var arena = Arena.ofConfined()) {
      var fileSize = Files.size(src);
      var mappedFile = channel.map(
              MapMode.READ_ONLY,
              0,
              fileSize,
              arena
      );

      var position = 0;
      var size = 8192;

      while (position + size < fileSize) {
        // then get bytes from buffer
        var memorySegment = mappedFile.asSlice(position, size);
        memorySegment.toArray(OfByte.JAVA_BYTE);

        position += size;
        size = (int) Math.min(size, fileSize - position);
      }

      mappedFile.unload();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
