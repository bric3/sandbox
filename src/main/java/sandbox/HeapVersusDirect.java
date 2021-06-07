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
 * $ cat > /tmp/sandbox.HeapVersusDirect.java <<EOF
 * > this file content...
 * > ...
 * > EOF
 * <p>
 * $ env -u JDK_JAVA_OPTIONS java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED /tmp/sandbox.HeapVersusDirect.java
 */
public class HeapVersusDirect {
  public static void main(String[] args) throws Exception {
    System.out.printf("max: %d%n", Runtime.getRuntime().maxMemory());

    var classLoader = HeapVersusDirect.class.getClassLoader();
    var internalUnsafeClass = classLoader.loadClass("jdk.internal.misc.Unsafe");
    var method = internalUnsafeClass.getDeclaredMethod("getUnsafe");
    var unsafe = method.invoke(null);
    var allocateUninitializedArray = unsafe.getClass()
        .getDeclaredMethod("allocateUninitializedArray", Class.class, int.class);

    System.out.printf("Java heap buffers");
    for (int i = 0; i < 30; i++) {
      byte[] arena = (byte[]) allocateUninitializedArray.invoke(unsafe, byte.class, 16 * 1024 * 1024);
      arena[0] = 0x01;
      arena[4096] = 0x01;
      arena[4096 * 2] = 0x01;
      arena[4096 * 3] = 0x01;
      arena[4096 * 4] = 0x01;
      System.out.printf("%s%n", Long.toHexString(addressOf(unsafe, arena)));
    }

    var address = Buffer.class.getDeclaredField("address");
    address.setAccessible(true);
    System.out.printf("native heap (pmap shows [heap] mapping");
    for (var i = 0; i < 30; i++) {
      var byteBuffer = ByteBuffer.allocateDirect(16 * 1024 * 1024);
      byteBuffer.putInt(0, 0x01);
      byteBuffer.putInt(4096, 0x01);
      byteBuffer.putInt(4096 * 2, 0x01);
      byteBuffer.putInt(4096 * 3, 0x01);
      byteBuffer.putInt(4096 * 4, 0x01);
      System.out.printf("%s%n", Long.toHexString(address.getLong(byteBuffer)));
    }

    new ProcessBuilder("pmap", "-X", Long.toString(ProcessHandle.current().pid()))
        .redirectOutput(Redirect.INHERIT)
        .start();
  }

  // Based on this SO answer https://stackoverflow.com/a/7060500/48136
  public static long addressOf(Object unsafe, Object object) throws Exception {

    var array = new Object[]{object};

    var baseOffset = (int) unsafe.getClass()
        .getDeclaredMethod("arrayBaseOffset", Class.class)
        .invoke(unsafe, Object[].class);
    var addressSize = (int) unsafe.getClass()
        .getDeclaredMethod("addressSize")
        .invoke(unsafe);
    long objectAddress;
    switch (addressSize) {
      case 4:
        objectAddress = (int) unsafe.getClass()
            .getDeclaredMethod("getInt", Object.class, long.class)
            .invoke(unsafe, array, baseOffset);
        break;
      case 8:
        objectAddress = (long) unsafe.getClass()
            .getDeclaredMethod("getLong", Object.class, long.class)
            .invoke(unsafe, array, baseOffset);
        break;
      default:
        throw new Error("unsupported address size: " + addressSize);
    }

    return (objectAddress);
  }
}