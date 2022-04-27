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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Indicates if {@code stdin}, {@code stdout}, {@code stderr} are connected to a terminal.
 *
 * <p>{@code System.console()} requires that standard streams are all connected to a terminal
 * to be not null, this is inconvenient as it might be useful to toggle some behaviors,
 * if these stream are piped or displayed on the terminal, eg ANSI escape sequences.</p>
 * <p>
 * Or read the terminal attributes, like dimensions, color support. Etc.
 * </p>
 *
 * <pre>
 * $ java src/main/java/sandbox/IsATTY.java
 * stdin : true
 * stdout: true
 * stderr: true
 *
 * $ java src/main/java/sandbox/IsATTY.java 2> /dev/null
 * stdin : true
 * stdout: true
 * stderr: false
 *
 * $ java src/main/java/sandbox/IsATTY.java | tee /dev/null
 * stdin : true
 * stdout: false
 * stderr: true
 *
 * $ echo 1 | java src/main/java/sandbox/IsATTY.java
 * stdin : false
 * stdout: true
 * stderr: true
 * ❯ ./gradlew runIsATTY
 *
 * > Task :runIsATTY
 * stdin : false
 * stdout: false
 * stderr: false
 * </pre>
 *
 * <p>This class also has uses a neat trick to call the isatty c function like
 * JEP-389 / JEP-412 (Foreign Linker API) as it dynamically generates its JNI
 * wrapper. Which makes this class usable on JDK 11.</p>
 */
public class IsATTY {
  static {
    Path libPath = compileJni(
        "#include <jni.h>\n" +
        "#include <unistd.h>\n" +
        "\n" +
        "JNIEXPORT jboolean JNICALL Java_" + IsATTY.class.getName().replace('.', '_') + "_isatty\n" +
        "          (JNIEnv *env, jclass cls, jint fileDescriptor) {\n" +
        "    return isatty(fileDescriptor)? JNI_TRUE: JNI_FALSE;\n" +
        "}\n");

    System.load(libPath.toString());
  }

  private static Path compileJni(String isatty) {
    String javaHome = System.getProperty("java.home");

    try {
      Path isattyPath = Files.createTempDirectory("isatty");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          new ProcessBuilder("rm", "-rf", isattyPath.toString())
              .start().waitFor();
        } catch (InterruptedException | IOException e) {
          e.printStackTrace();
        }
      }));


      Files.writeString(isattyPath.resolve("isatty.c"), isatty, CREATE, TRUNCATE_EXISTING);

      Process p = new ProcessBuilder("gcc",
                                     "-I", javaHome + "/include", // jni.h
                                     "-I", javaHome + "/include/darwin", // jni_md.h
                                     "-I", javaHome + "/include/linux", // jni_md.h
                                     "-fPIC",  // toggle Position Independent Code, suitable for libraries
                                     "-shared", // shared object, ie no main
                                     isattyPath.resolve("isatty.c").toString(),
                                     "-o",
                                     isattyPath.resolve("isatty.so").toString())
          .redirectOutput(ProcessBuilder.Redirect.INHERIT)
          .redirectInput(ProcessBuilder.Redirect.INHERIT)
          .redirectError(ProcessBuilder.Redirect.INHERIT)
          .start();

      p.waitFor();
      return isattyPath.resolve("isatty.so");
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }


  public static void main(String[] args) {
    System.out.printf("stdin : %s%n", isStdinConnectedToTty());
    System.out.printf("stdout: %s%n", isStdoutConnectedToTty());
    System.out.printf("stderr: %s%n", isStderrConnectedToTty());
  }

  public native static boolean isatty(int fileDescriptor);

  public static boolean isStdinConnectedToTty() {
    return isatty(0);
  }

  public static boolean isStdoutConnectedToTty() {
    return isatty(1);
  }

  public static boolean isStderrConnectedToTty() {
    return isatty(2);
  }
}