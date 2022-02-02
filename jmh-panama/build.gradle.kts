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

plugins {
  id("java")
  id("me.champeau.jmh") version "0.6.6"
}

repositories {
  mavenCentral()
}


dependencies {
  implementation("net.java.dev.jna:jna-platform:5.9.0")
  implementation("com.github.jnr:jnr-ffi:2.2.11")

  // https://github.com/joshjdevl/libsodium-jni
  // Note this lib doesn't include the JNI glue library, see https://github.com/joshjdevl/libsodium-jni/issues/66
  implementation("com.github.joshjdevl.libsodiumjni:libsodium-jni:2.0.2")

  // https://github.com/muquit/libsodium-jna
  implementation("com.muquit.libsodiumjna:libsodium-jna:1.0.4")

  // libsodium with JNR
  implementation("org.apache.tuweni:tuweni-crypto:2.0.0")

  implementation("org.apache.commons:commons-lang3:3.12.0")
}


java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}
val launcher = javaToolchains.launcherFor(java.toolchain).get()

// ./gradlew :jmh-panama:jmhJar
// taskset -c 0 env JAVA_LIBRARY_PATH=$(grealpath jmh-panama/jni) java -jar jmh-panama/build/libs/jmh-panama-jmh.jar -jvmArgs '-Djmh.separateClasspathJAR=true --add-modules=jdk.incubator.foreign --enable-native-access=ALL-UNNAMED'
jmh {
  // Strategy to apply when encountering duplicate classes during creation of the fat jar (i.e. while executing jmhJar task)
  duplicateClassesStrategy.set(DuplicatesStrategy.WARN)

//  environment("JAVA_LIBRARY_PATH", ".:${project.projectDir}/jni")

  jvmArgs.set(listOf(
          "-Djmh.separateClasspathJAR=true",
          "--add-modules=jdk.incubator.foreign",
          "--enable-native-access=ALL-UNNAMED"
  ))

  jvm.set(launcher.executablePath.asFile.absolutePath)
  jmhVersion.set("1.33")
}

tasks {
  withType<JavaCompile>() {
    options.compilerArgs = listOf(
            "--add-modules", "jdk.incubator.foreign"
    )
    options.release.set(17)
  }

  withType<JavaExec>().configureEach {
    environment("JAVA_LIBRARY_PATH", ".:${project.projectDir}/jni")
    jvmArgs("--enable-native-access=ALL-UNNAMED",
            "--add-modules", "jdk.incubator.foreign")
    javaLauncher.set(project.javaToolchains.launcherFor(java.toolchain))
  }
}