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
  `swift-application`
}

tasks.withType<SwiftCompile> {
  // Define a preprocessor macro for every binary
  macros.add("NDEBUG")

  // Define a compiler options
  compilerArgs.add("-O")
}

application {
  targetMachines.set(listOf(
    machines.macOS,
    machines.linux,
  ))
}