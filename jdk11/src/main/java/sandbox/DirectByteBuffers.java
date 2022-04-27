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
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Usage:
 * <p>
 * $ cat > /tmp/sandbox.DirectByteBuffers.java <<EOF
 * > this file content...
 * > ...
 * > EOF
 * <p>
 * $ env -u JDK_JAVA_OPTIONS java --add-opens java.base/java.nio=ALL-UNNAMED /tmp/sandbox.DirectByteBuffers.java
 */
public class DirectByteBuffers {
  public static void main(String[] args) throws Exception {
    System.out.printf("max: %d%n", Runtime.getRuntime().maxMemory());

    new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
        .redirectOutput(Redirect.INHERIT)
        .start();

    var address = Buffer.class.getDeclaredField("address");
    address.setAccessible(true);
    System.out.printf("native heap (pmap shows [heap] mapping");
    for (var i = 0; i < 30; i++) {
      var byteBuffer = ByteBuffer.allocateDirect(16 * 1024 * 1024)
          .putInt(0, 0x01);
      System.out.printf("%s%n", Long.toHexString(address.getLong(byteBuffer)));
    }

    new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
        .redirectOutput(Redirect.INHERIT)
        .start();
  }
}