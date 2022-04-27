/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */



tasks.withType<JavaExec>().configureEach {
  group = "class-with-main"
  classpath(sourceSets.main.get().runtimeClasspath)

  val libgraalLocation =
    (project.findProperty("libgraalLocation") as String).replace("\$HOME", System.getProperty("user.home"))

  jvmArgs(
    "-ea",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+EnableJVMCI",
    "-XX:JVMCILibPath=${libgraalLocation}",
    "-XX:+UseJVMCICompiler",
    "-XX:+UseJVMCINativeLibrary",
  )
}
