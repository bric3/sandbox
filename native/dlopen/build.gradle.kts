/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import org.gradle.internal.os.OperatingSystem

// inspiration from libgdx/packr PackrLauncher/packrLauncher.gradle.kts

plugins {
  `cpp-application`
  xcode
  `jvm-toolchains`
}

application {
  targetMachines = listOf(
    // machines.windows.x86_64,
    machines.linux.x86_64,
    machines.macOS.x86_64,
    machines.macOS.architecture("aarch64")
  )
  baseName = "test-dlopen"

  binaries.configureEach(CppExecutable::class) {
    val binaryCompileTask = compileTask.get()
    val binaryLinkTask: LinkExecutable = linkTask.get()

    // Create a single special publication from lipo on MacOS since that allows combining multiple architectures into a single binary
    val publicationName = buildString {
      append(baseName.get())
      append(targetMachine.operatingSystemFamily.name)
      append(if (!targetMachine.operatingSystemFamily.isMacOs) "-${targetMachine.architecture.name}" else "")
    }
    // from(executableFile) {
    //   rename(".*", publicationName)
    // }


    println(toolChain)

    when (toolChain) {
      is Clang -> {
        binaryCompileTask.compilerArgs.add("-fPIC")
        binaryCompileTask.compilerArgs.add("-c")
        binaryCompileTask.compilerArgs.add("-fmessage-length=0")
        binaryCompileTask.compilerArgs.add("-Wwrite-strings")

        binaryLinkTask.linkerArgs.add("-ldl")
        binaryLinkTask.linkerArgs.add("-no-pie")
        binaryLinkTask.linkerArgs.add("-fno-pie")
        binaryCompileTask.compilerArgs.add("-std=c++14")

        if (targetPlatform.targetMachine.operatingSystemFamily.isMacOs) {
          binaryLinkTask.linkerArgs.add("-framework")
          binaryLinkTask.linkerArgs.add("CoreFoundation")
        }
      }

      is Gcc -> {
        binaryCompileTask.compilerArgs.add("-fPIC")
        binaryCompileTask.compilerArgs.add("-c")
        binaryCompileTask.compilerArgs.add("-fmessage-length=0")
        binaryCompileTask.compilerArgs.add("-Wwrite-strings")
        binaryCompileTask.compilerArgs.add("-std=c++14")
        binaryCompileTask.compilerArgs.add("-no-pie")
        binaryCompileTask.compilerArgs.add("-fno-pie")
        binaryCompileTask.compilerArgs.add("-m64")

        // compiler osx
        if (targetPlatform.targetMachine.operatingSystemFamily.isMacOs) {
          binaryCompileTask.includes(file("/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers"))
        }

        binaryLinkTask.linkerArgs.add("-no-pie")
        binaryLinkTask.linkerArgs.add("-fno-pie")
      }
    }
  }
}

tasks.withType<CppCompile>().configureEach {
  source.from(fileTree(file("src/main/c")) {
    include("**/*.c")
  })
}

tasks.withType<LinkExecutable>().configureEach {
  // dynamic loader
  // https://blog.stephenmarz.com/2020/06/22/dynamic-linking/
  linkerArgs.add("-ldl")
}

tasks.withType<InstallExecutable> {

}

val javaHomeProvider = javaToolchains.launcherFor {
  languageVersion = JavaLanguageVersion.of(21)
}.map { it.metadata.installationPath }


tasks {
  register<Exec>("run-test-dlopen") {
    dependsOn(assemble)
    group = "other"

    val os = OperatingSystem.current()
    environment = mapOf(
      when {
        os.isLinux -> "LD_LIBRARY_PATH"
        os.isMacOsX -> "DYLD_LIBRARY_PATH"
        else -> throw UnsupportedOperationException("LD LIBRARY PATH is not supported on current os $os")
      } to "${javaHomeProvider.get()}/lib/server",
    )

    val installDebugTask = this@tasks.withType(InstallExecutable::class.java).filter {
      it.name.contains("debug", ignoreCase = true)
    }.first()

    installDebugTask.executableFile.get().asFile.let {
      executable(it.absolutePath)
    }
  }
}

// import dev.nokee.platform.nativebase.ExecutableBinary
//
// plugins {
//     id("dev.nokee.c-application")
//     java
// }
//
// application {
//     baseName.set("test-dlopen")
//     binaries.configureEach(ExecutableBinary::class.java) {
//         linkTask {
//             // dynamic loader
//             // https://blog.stephenmarz.com/2020/06/22/dynamic-linking/
//             linkerArgs.add("-ldl")
//         }
//     }
// }
//
//
// val javaHomeProvider = javaToolchains.launcherFor {
//   languageVersion = JavaLanguageVersion.of(21)
// }.map { it.metadata.installationPath }
//
//
// tasks {
//   register<Exec>("run-test-dlopen") {
//     dependsOn(assemble)
//     group = "other"
//
//
//     val os = OperatingSystem.current()
//     environment = mapOf(
//       when {
//         os.isLinux -> "LD_LIBRARY_PATH"
//         os.isMacOsX -> "DYLD_LIBRARY_PATH"
//         else -> throw UnsupportedOperationException("LD LIBRARY PATH is not supported on current os $os")
//       } to "${javaHomeProvider.get()}/lib/server",
//     )
//
//     val binary = application.binaries.withType(ExecutableBinary::class.java).get().single()
//     executable(
//       layout.buildDirectory.file("exes/${sourceSets.main.name}/${binary.baseName.get()}").get().toString()
//     )
//   }
// }