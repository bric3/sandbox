/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
  id("java")
  alias(libs.plugins.jmh)
}

repositories {
  mavenCentral()
}


dependencies {
  implementation("net.java.dev.jna:jna-platform:5.14.0")
  implementation("com.github.jnr:jnr-ffi:2.2.16")

  // https://github.com/joshjdevl/libsodium-jni
  // Note this lib doesn't include the JNI glue library, see https://github.com/joshjdevl/libsodium-jni/issues/66
  implementation("com.github.joshjdevl.libsodiumjni:libsodium-jni:2.0.2")

  // https://github.com/muquit/libsodium-jna
  implementation("com.muquit.libsodiumjna:libsodium-jna:1.0.4")

  // libsodium with JNR
  implementation("org.apache.tuweni:tuweni-crypto:2.3.1")

  implementation("org.apache.commons:commons-lang3:3.16.0")
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
  jmhVersion.set(libs.versions.jmh)
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