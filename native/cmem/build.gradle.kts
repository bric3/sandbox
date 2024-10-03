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
  `cpp-application`
}

application {
  baseName = "test-cmem"

  binaries.configureEach {
    this.compileTask.get()
      .source
      .from(fileTree("src/main/c") { include("**/*.c") })
  }
}

tasks.withType<CppCompile>().configureEach {
  // Define a preprocessor macro for every binary
  macros.put("NDEBUG", null)

  // Define a compiler options
  compilerArgs.add("-W3")

  // Define toolchain-specific compiler options
  compilerArgs.addAll(toolChain.map { toolChain ->
    when (toolChain) {
      is Gcc, is Clang -> listOf("-O2", "-fno-access-control")
      is VisualCpp -> listOf("/Zi")
      else -> listOf()
    }
  })
}

// With nokee (not working since Gradle 8.3, see https://github.com/nokeedev/gradle-native/issues/853)
// import dev.nokee.platform.nativebase.ExecutableBinary
// import dev.nokee.platform.nativebase.internal.linking.NativeLinkTaskUtils
//
// plugins {
//   id("dev.nokee.c-application")
// }
//
// application {
//   baseName.set("test-cmem")
// //    cSources.setFrom(fileTree("srcs") { include("**/*.c") })		// <2>
// //    privateHeaders.setFrom(fileTree("hdrs") { include("**/*.h") })	// <3>
// //    publicHeaders.setFrom(fileTree("incs") { include("**/*.h") })	// <4>
// }
