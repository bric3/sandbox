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