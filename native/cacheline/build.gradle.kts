/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */
plugins {
  `cpp-library`
  `jvm-toolchains`

  // Cannot use `java` plugin or `sandbox.java-conventions` because they conflict with `cpp-library`:
  // Both plugins try to create an "implementation" configuration, causing a build error.
  // Solution: Use `java-base` (minimal Java support without configurations) and manually configure
  // Java compilation tasks with custom configurations (javaImplementation, javaTestImplementation, etc.)
  `java-base`
}

library {
  binaries.configureEach(CppBinary::class.java) {
    compileTask.get().apply {
      // Configure C sources from custom location
      source.from(fileTree("src/main/c") {
        include("**/*.c")
      })

      macros["NDEBUG"] = null

      compilerArgs.addAll(toolChain.map { toolChain ->
        when (toolChain) {
          is Gcc, is Clang -> listOf("-x", "c", "-std=c11", "-Wall", "-O2", "-fPIC")
          is VisualCpp -> listOf("/TC", "/W3", "/Zi")
          else -> listOf()
        }
      })
    }
  }

  // Additional configuration for shared library linking
  binaries.configureEach(CppSharedLibrary::class.java) {
    linkTask.get().apply {
      linkerArgs.addAll(toolChain.map { toolChain ->
        when (toolChain) {
          is Gcc, is Clang -> listOf("-shared")
          else -> listOf()
        }
      })
    }
  }
}

repositories {
  mavenCentral()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val javaLanguageVersion = JavaLanguageVersion.of(25)
val javaToolchainCompiler = javaToolchains.compilerFor {
  languageVersion = javaLanguageVersion
}
val javaToolchainLauncher = javaToolchains.launcherFor {
  languageVersion = javaLanguageVersion
}

// Manually create configurations for Java dependencies to avoid conflicts with cpp-library
val javaImplementation by configurations.registering
val javaTestImplementation by configurations.registering {
  extendsFrom(javaImplementation.get())
}
val javaTestRuntimeOnly by configurations.registering {
  extendsFrom(javaImplementation.get())
}

dependencies {
  javaTestImplementation(libs.findBundle("junit6.jupiter").get())
  javaTestRuntimeOnly(libs.findLibrary("junit6.platform.launcher").get())
}

// Manually create Java compile tasks (can't use java plugin due to conflict with cpp-library)
val compileJava by tasks.registering(JavaCompile::class) {
  source = fileTree("src/main/java") {
    include("**/*.java")
  }
  classpath = files()
  javaCompiler.set(javaToolchainCompiler)
  destinationDirectory.set(layout.buildDirectory.dir("classes/java/main"))
  options.release.set(javaLanguageVersion.asInt())
  options.encoding = "UTF-8"
  dependsOn("linkDebug")
}

val compileTestJava by tasks.registering(JavaCompile::class) {
  source = fileTree("src/test/java") {
    include("**/*.java")
  }
  classpath = files(
    compileJava.get().destinationDirectory,
    javaTestImplementation
  )
  javaCompiler.set(javaToolchainCompiler)
  destinationDirectory.set(layout.buildDirectory.dir("classes/java/test"))
  options.release.set(javaLanguageVersion.asInt())
  options.encoding = "UTF-8"
  dependsOn(compileJava, "linkDebug")
}

// Create test task
val test by tasks.registering(Test::class) {
  testClassesDirs = files(compileTestJava.get().destinationDirectory)
  classpath = files(
    compileJava.get().destinationDirectory,
    compileTestJava.get().destinationDirectory,
    javaTestImplementation,
    javaTestRuntimeOnly
  )

  useJUnitPlatform()
  javaLauncher.set(javaToolchainLauncher)

  val nativeLibPath = layout.buildDirectory
    .dir("lib/main/debug")
    .get()
    .asFile
    .absolutePath

  systemProperty("java.library.path", nativeLibPath)
  jvmArgs("--enable-native-access=ALL-UNNAMED")

  dependsOn(compileTestJava, "linkDebug")
}

// Configure JavaExec tasks
tasks.withType<JavaExec>().configureEach {
  group = "class-with-main"
  classpath = files(compileJava.get().destinationDirectory)
  javaLauncher.set(javaToolchainLauncher)

  val nativeLibPath = layout.buildDirectory
    .dir("lib/main/debug")
    .get()
    .asFile
    .absolutePath

  systemProperty("java.library.path", nativeLibPath)
  jvmArgs("--enable-native-access=ALL-UNNAMED")

  dependsOn(compileJava, "linkDebug")
}

// Add test to check task
tasks.named("check") {
  dependsOn(test)
}
