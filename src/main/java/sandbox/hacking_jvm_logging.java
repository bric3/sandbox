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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Set;

public class hacking_jvm_logging {
  /**
   * Logging Level
   * <p>
   * public enum LogLevel {
   * TRACE(1),
   * DEBUG(2),
   * INFO(3),
   * WARN(4),
   * ERROR(5);
   */
  public static final int LOG_LEVEL_INFO = 3;

  /**
   * Id matching declared tag sets
   * <p>
   * public enum LogTag {
   * /**
   * * Covers
   * * <ul>
   * * <li>Initialization of Flight Recorder
   * * <li> recording life cycle (start, stop and dump)
   * * <li> repository life cycle
   * * <li>loading of configuration files.
   * * </ul>
   * * Target audience: operations
   * *\/
   * JFR(0),
   */
  public static final int LOG_TAG_SET_JFR = 0;

  public static void main(String[] args) throws Throwable {
    ProcessHandle.current().info().arguments()
        .filter(argList -> Arrays.asList(argList).containsAll(Set.of("-Xlog:jfr=info", "--add-opens", "jdk.jfr/jdk.jfr.internal=ALL-UNNAMED")))
        .orElseThrow(() -> new IllegalArgumentException("not working without JVM arguments"));

    var jvmClass = hacking_jvm_logging.class.getClassLoader().loadClass("jdk.jfr.internal.JVM");
    var lookup = MethodHandles.lookup();
    var privateLookup = MethodHandles.privateLookupIn(jvmClass, lookup);

    var signature = MethodType.methodType(void.class, int.class, int.class, String.class);
    var mh = privateLookup.findStatic(jvmClass, "log", signature);
    // backed by https://github.com/AdoptOpenJDK/openjdk-jdk11u/blob/75e6fdb4dfc5d95674b321e77f42a4097d54c571/src/hotspot/share/jfr/utilities/jfrJavaLog.cpp#L122-L140
    mh.invokeExact(LOG_TAG_SET_JFR, LOG_LEVEL_INFO, "hello jfr");
  }
}