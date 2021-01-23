// env -u JDK_JAVA_OPTIONS java -XX:NativeMemoryTracking=summary DBB.java 1 1

package sandbox;

import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;

public class DBB {
  public static void main(String[] args) throws Exception {
    System.out.printf("nmt baseline: %n");
    new ProcessBuilder("jcmd", Long.toString(ProcessHandle.current().pid()), "VM.native_memory", "baseline")
        .redirectOutput(Redirect.INHERIT)
        .redirectError(Redirect.INHERIT)
        .start()
        .waitFor();

    var bbCount = Integer.parseInt(args[0]);
    var bbSizeMiB = Integer.parseInt(args[1]);
    for (var i = 0; i < bbCount; i++) {
      var byteBuffer = ByteBuffer.allocateDirect(bbSizeMiB * 1024 * 1024)
          .putInt(0, 0x01);
    }

    System.out.printf("nmt summary.diff: %n");
    new ProcessBuilder("jcmd", Long.toString(ProcessHandle.current().pid()), "VM.native_memory", "summary.diff")
        .redirectOutput(Redirect.INHERIT)
        .redirectError(Redirect.INHERIT)
        .start()
        .waitFor();
  }
}
