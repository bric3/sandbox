package sandbox;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

public class MappedFiles {
  public static void main(String[] args) throws Exception {
    System.out.printf("nmt baseline: %n");
    new ProcessBuilder("jcmd", Long.toString(ProcessHandle.current().pid()), "VM.native_memory", "baseline")
        .start()
        .waitFor();

    // /usr/lib/jvm/java-11-amazon-corretto/lib/src.zip
    // /usr/lib/jvm/java-11-amazon-corretto/jmods/*.jmod

    Path src = Paths.get("/usr/lib/jvm/java-11-amazon-corretto/lib/src.zip");
    try (var fileChannel = (FileChannel) Files.newByteChannel(src, StandardOpenOption.READ)) {
      var mappedByteBuffer = fileChannel.map(
          FileChannel.MapMode.READ_ONLY,
          0,
          fileChannel.size());
      mappedByteBuffer.load();

//      if (mappedByteBuffer != null) {
//        CharBuffer charBuffer = Charset.forName("UTF-8")
//            .newDecoder()
//            .onMalformedInput(CodingErrorAction.REPORT)
//            .onUnmappableCharacter(CodingErrorAction.REPORT)
//            .decode(mappedByteBuffer);
//      }
      System.out.printf("nmt summary.diff: %n");
      new ProcessBuilder("jcmd", Long.toString(ProcessHandle.current().pid()), "VM.native_memory", "summary.diff")
          .redirectOutput(ProcessBuilder.Redirect.INHERIT)
          .redirectError(ProcessBuilder.Redirect.INHERIT)
          .start()
          .waitFor();

      new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
          .redirectOutput(ProcessBuilder.Redirect.INHERIT)
          .start()
          .waitFor();
    }
  }
}
