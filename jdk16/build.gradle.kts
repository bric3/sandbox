/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
