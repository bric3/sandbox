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

package sandbox;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MappedFilesAndNMT {
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