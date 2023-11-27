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
  id("sandbox.graal-jvmci-conventions")
}

tasks.withType<JavaExec>().configureEach {
  group = "class-with-main"
  classpath(sourceSets.main.get().runtimeClasspath)

  jvmArgs(
    "-ea"
  )
}

/* Note when changing versions this can happen:
 *
 * Error occurred during initialization of boot layer
 * java.lang.module.FindException: Two versions of module org.graalvm.sdk found in .../graal/run-with-graal/build/graalCompiler (graal-sdk-22.0.0.2.jar and graal-sdk-22.1.0.jar)
 */
