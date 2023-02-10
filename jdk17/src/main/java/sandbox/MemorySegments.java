/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package sandbox;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

import java.lang.ProcessBuilder.Redirect;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Usage:
 * <p>
 * $ cat > /tmp/MemorySegments.java <<EOF
 * > this file content...
 * > ...
 * > EOF
 * <p>
 * $ env -u JDK_JAVA_OPTIONS java -Dforeign.restricted=permit --add-modules jdk.incubator.foreign -XX:MaxDirectMemorySize=3g src/main/java/sandbox/MemorySegments.java 
 * $ env -u JDK_JAVA_OPTIONS java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -Dforeign.restricted=permit --add-modules jdk.incubator.foreign -XX:MaxDirectMemorySize=3g src/main/java/sandbox/MemorySegments.java
 * <p>
 * -XX:MaxDirectMemorySize=<size>
 */
public class MemorySegments {
  public static void main(String[] args) throws Exception {
    //var maxMemory = Runtime.getRuntime().maxMemory();
    //var direct = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
    //    .stream()
    //    .filter(p -> p.getName().equals("direct"))
    //    .findFirst()
    //    .get();
    //
    //System.out.printf("max: %d, direct: %d%n", maxMemory, direct.getTotalCapacity());
    //2097152L;
    var nativeSegmentSize = 200L * 1024 * 1024; // 200 MiB

    //System.out.printf("max: %d, direct: %d%n", maxMemory, direct.getTotalCapacity());
    //if (maxMemory < nativeSegmentSize &&
    //    ProcessHandle.current().info().arguments().stream().flatMap(Arrays::stream)
    //        .anyMatch(arg -> arg.contains("-XX:MaxDirectMemorySize"))) {
    //  System.err.printf(
    //      "Native allocation cannot go over Runtime.getRuntime().maxMemory() (currently : %d), %n" +
    //          "if this number is not enough, one can use -XX:MaxDirectMemorySize=%d%n",
    //      maxMemory,
    //      nativeSegmentSize
    //  );
    //  System.exit(1);
    //}

    try (var scope = ResourceScope.newConfinedScope()) {
      var path = Path.of("cpu-11.0.8-start-graal.svg");
      var mmaped = MemorySegment.mapFile( // MemorySegment is not anymore AutoCloseable
              path,
              0,
              Files.size(path),
              FileChannel.MapMode.READ_ONLY,
              scope
      );

      // do something with the MeorySegment
    }


    new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
        .redirectOutput(Redirect.INHERIT)
        .start()
        .waitFor();

    try(var scope = ResourceScope.newConfinedScope()) {
      // With API lifecycle PR https://github.com/openjdk/panama-foreign/pull/466
      // - NativeScope is gone
      // - MemorySegment is not anymore AutoCloseable
      // - Access modes are gone, remains only asReadOnly()
      // - Introduction of the SegmentAllocator API
      var memorySegment = MemorySegment.allocateNative(nativeSegmentSize, scope);

      System.out.println("address: " + memorySegment.address());
      System.out.println("address: " + memorySegment.address().toRawLongValue());
      System.out.println("address: " + Long.toHexString(memorySegment.address().toRawLongValue()));
      new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
          .redirectOutput(Redirect.INHERIT)
          .start()
          .waitFor();

      ResourceScope.Handle handle = scope.acquire(); // make scope uncloseable util released
      {

        var readMemorySegment = memorySegment.asReadOnly();
        readMemorySegment.asByteBuffer().getLong();
        //readMemorySegment.asByteBuffer().putLong(1L);

        var segmentAllocator = SegmentAllocator.arenaAllocator(scope);
        var s1 = segmentAllocator.allocate(16 * 1024 * 1024);
        System.out.println("s1 address: " + s1.address());
        System.out.println("s1 address: " + s1.address().toRawLongValue());
        System.out.println("s1 address: " + Long.toHexString(s1.address().toRawLongValue()));

        var s2 = segmentAllocator.allocate(16 * 1024 * 1024);
        System.out.println("s2 address: " + s2.address());
        System.out.println("s2 address: " + s2.address().toRawLongValue());
        System.out.println("s2 address: " + Long.toHexString(s2.address().toRawLongValue()));

        System.out.println("-----------------------------------------");
        System.out.println();
        System.out.println();
        System.out.printf("native heap (pmap shows [heap] mapping%n");
        System.out.println();
        System.out.println();
        System.out.println("-----------------------------------------");

        new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
                .redirectOutput(Redirect.INHERIT)
                .start()
                .waitFor();

        System.out.println("-----------------------------------------");
        System.out.println();
        System.out.println();
        System.out.printf("native heap (pmap shows [heap] mapping%n");
        System.out.println();
        System.out.println();
        System.out.println("-----------------------------------------");


        new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
                .redirectOutput(Redirect.INHERIT)
                .start()
                .waitFor();


        System.out.println("-----------------------------------------");
        System.out.println();
        System.out.println();
        System.out.printf("native heap (pmap shows [heap] mapping%n");
        System.out.println();
        System.out.println();
        System.out.println("-----------------------------------------");


        var slice = readMemorySegment.asSlice(100L * 1024 * 1024 /* 100 MiB */, 0x1000000 /* 16 MiB*/);
        System.out.println("slice address: " + slice.address());
        System.out.println("slice address: " + slice.address().toRawLongValue());
        System.out.println("slice address: " + Long.toHexString(slice.address().toRawLongValue()));


      }
      scope.release(handle);
    }
    
    new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
        .redirectOutput(Redirect.INHERIT)
        .start()
        .waitFor();

  }
}