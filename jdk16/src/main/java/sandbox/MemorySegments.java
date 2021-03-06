/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package sandbox;import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.NativeScope;

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
//    var maxMemory = Runtime.getRuntime().maxMemory();
//    var direct = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
//        .stream()
//        .filter(p -> p.getName().equals("direct"))
//        .findFirst()
//        .get();
//
//    System.out.printf("max: %d, direct: %d%n", maxMemory, direct.getTotalCapacity());
//    2097152L;
    var nativeSegmentSize = 200L * 1024 * 1024; // 200 MiB

//    System.out.printf("max: %d, direct: %d%n", maxMemory, direct.getTotalCapacity());
//    if (maxMemory < nativeSegmentSize &&
//        ProcessHandle.current().info().arguments().stream().flatMap(Arrays::stream)
//            .anyMatch(arg -> arg.contains("-XX:MaxDirectMemorySize"))) {
//      System.err.printf(
//          "Native allocation cannot go over Runtime.getRuntime().maxMemory() (currently : %d), %n" +
//              "if this number is not enough, one can use -XX:MaxDirectMemorySize=%d%n",
//          maxMemory,
//          nativeSegmentSize
//      );
//      System.exit(1);
//    }
    var path = Path.of("cpu-11.0.8-start-graal.svg");
    try (var mmaped = MemorySegment.mapFile(
        path,
        0,
        Files.size(path),
        FileChannel.MapMode.READ_ONLY
    )) {
      // ...
    }


    new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
        .redirectOutput(Redirect.INHERIT)
        .start()
        .waitFor();

    try(var scope = NativeScope.unboundedScope()) {

      var memorySegment = MemorySegment.allocateNative(nativeSegmentSize)
          .withAccessModes(MemorySegment.WRITE | MemorySegment.CLOSE);

      System.out.println("address: " + memorySegment.address());
      System.out.println("address: " + memorySegment.address().toRawLongValue());
      System.out.println("address: " + Long.toHexString(memorySegment.address().toRawLongValue()));
      new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
          .redirectOutput(Redirect.INHERIT)
          .start()
          .waitFor();


      memorySegment = memorySegment.withAccessModes(MemorySegment.READ | MemorySegment.CLOSE);
      memorySegment.asByteBuffer().getLong();
//      memorySegment.asByteBuffer().putLong(1L);

      var s1 = scope.allocate(16 * 1024 * 1024);
      System.out.println("s1 address: " + s1.address());
      System.out.println("s1 address: " + s1.address().toRawLongValue());
      System.out.println("s1 address: " + Long.toHexString(s1.address().toRawLongValue()));

      var s2 = scope.allocate(16 * 1024 * 1024);
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


      var slice = memorySegment.asSlice(100L * 1024 * 1024 /* 100 MiB */, 0x1000000 /* 16 MiB*/)
          .withAccessModes(MemorySegment.WRITE);
      System.out.println("slice address: " + slice.address());
      System.out.println("slice address: " + slice.address().toRawLongValue());
      System.out.println("slice address: " + Long.toHexString(slice.address().toRawLongValue()));

    slice.close();

      System.out.printf("alive: %s%n", memorySegment.isAlive());

      memorySegment.close();
    }
    new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
        .redirectOutput(Redirect.INHERIT)
        .start()
        .waitFor();

  }
}