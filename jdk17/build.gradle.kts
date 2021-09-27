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

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

// TODO
// https://docs.gradle.org/current/userguide/declaring_dependencies_between_subprojects.html

// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>().configureEach {
  dependsOn(tasks.compileJava)
  group = "class-with-main"
  classpath(configurations.runtimeClasspath)

  // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
  jvmArgs(
          "--enable-native-access=ALL-UNNAMED",
          "--add-modules=jdk.incubator.foreign",
          "--enable-preview"
  )

  // env ? JAVA_LIBRARY_PATH=.:/usr/local/lib
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(17)
  options.compilerArgs = listOf(
          "--add-modules=jdk.incubator.foreign",
          "--enable-preview",
          "-Xlint:preview"
  )
}

tasks.register<JavaExec>("defineAnonymousClass") {
  mainClass.set("sandbox.DefineAnonymousClass")
  args = listOf("Goodbye Unsafe::defineAnonymousClass")
}

