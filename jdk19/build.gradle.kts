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
  // Playing with graal compiler
  id("org.graalvm.plugin.compiler") version "0.1.0-alpha2"
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of("19"))
    // my build
    // bash configure --with-vendor-name=bric3
    // vendor.set(JvmVendorSpec.matching("bric3"))
    // loom
    // vendor.set(JvmVendorSpec.AMAZON)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.bundles.flightRecorder)
  implementation(libs.flexmark.all)
}

// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>().configureEach {
  group = "class-with-main"
  classpath(sourceSets.main.get().runtimeClasspath)

  // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
  javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
  jvmArgs(
    "-ea",
    // "--enable-native-access=ALL-UNNAMED",
    // "--add-modules=jdk.incubator.foreign",
    "--add-modules=jdk.incubator.concurrent",
    "--enable-preview",
  )

  environment.putAll(
    mapOf(
      "JAVA_LIBRARY_PATH" to sourceSets.main.get().output.resourcesDir!!, // for IntelliJ run main class
      // "JAVA_LIBRARY_PATH" to ".:/usr/local/lib",
      "FINNHUB_TOKEN" to System.getenv("FINNHUB_TOKEN"),
      "HTTP_CLIENT_CARRIER_THREADS" to System.getenv("HTTP_CLIENT_CARRIER_THREADS"),
    )
  )
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(19)
  options.compilerArgs = listOf(
    // "--add-modules=jdk.incubator.foreign",
    "--add-modules=jdk.incubator.concurrent",
    "--enable-preview",
    "-Xlint:preview",
  )
}


tasks.create("showToolchain") {
  doLast {
    val launcher = javaToolchains.launcherFor(java.toolchain).get()

    println(launcher.executablePath)
    println(launcher.metadata.installationPath)
  }
}


graal {
    version = libs.versions.graalvm.get()
}
