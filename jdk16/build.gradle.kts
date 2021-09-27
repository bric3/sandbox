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
    languageVersion.set(JavaLanguageVersion.of(16))
  }
}

// For some reasons, IJ fails with this, instead run the built class directly
// java -cp build/classes/java/main -Dforeign.restricted=permit --add-modules=jdk.incubator.foreign sandbox.TryStuff
// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>() {
  dependsOn(tasks.compileJava)
  group = "class-with-main"
  classpath(configurations.runtimeClasspath)

  // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(16)) })
  jvmArgs(
          "-Dforeign.restricted=permit",
          "--add-modules=jdk.incubator.foreign"
  )
}

tasks.withType<JavaCompile>() {
  options.release.set(16)
  options.compilerArgs = listOf("--add-modules=jdk.incubator.foreign")
}

tasks.register<JavaExec>("innerClassStaticFields") {
  mainClass.set("sandbox.StaticFieldInnerClass")
}
