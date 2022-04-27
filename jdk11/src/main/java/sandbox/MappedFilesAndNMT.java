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