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
  `cpp-application`
}

application {
  binaries.configureEach(CppBinary::class.java) {
    compileTask.get().apply {
      // Configure C sources from custom location
      source.from(fileTree("src/main/c") {
        include("**/*.c")
      })

      macros["NDEBUG"] = null

      compilerArgs.addAll(toolChain.map { toolChain ->
        when (toolChain) {
          is Gcc, is Clang -> listOf("-x", "c", "-std=c11", "-Wall", "-O2")
          is VisualCpp -> listOf("/TC", "/W3", "/Zi")
          else -> listOf()
        }
      })
    }
  }
}