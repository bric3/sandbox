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