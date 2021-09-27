/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */



plugins {
  `swift-library`
  // `xctest` to support building and running test executables (linux) or bundles (macos)
}

library {
  linkage.set(listOf(Linkage.SHARED, Linkage.STATIC))
  targetMachines.set(listOf(machines.linux.x86_64, machines.macOS.x86_64))
  module.set("TouchIdDemoLib")

  // Set compiler flags here due to bug
  // https://github.com/gradle/gradle/issues/18439
  binaries.configureEach(SwiftSharedLibrary::class) {
    compileTask.get().run {
      optimized.set(false)
      debuggable.set(false)
    }
  }
  binaries.configureEach(SwiftStaticLibrary::class) {
    compileTask.get().run {
      optimized.set(false)
      debuggable.set(false)
    }
  }
}

//tasks.withType<SwiftCompile>().configureEach {
//  // Define a compiler options
//  optimized.set(false)
//}

tasks.assemble {
  tasks.findByName("assembleReleaseStaticMacos")?.let {
    dependsOn(it)
    doLast {
      println("static") // if Linkage.STATIC
      println("release " + (tasks.named("createReleaseStaticMacos").get() as CreateStaticLibrary).outputFile.get())
      println("debug " + (tasks.named("createDebugStaticMacos").get() as CreateStaticLibrary).outputFile.get())
    }
  }

  tasks.findByName("assembleReleaseSharedMacos")?.let {
    dependsOn(it)
    doLast {
      println("lib") // if Linkage.SHARED
      println("release " + (tasks.named("linkReleaseSharedMacos").get() as LinkSharedLibrary).linkedFile.get())
      println("debug " + (tasks.named("linkDebugSharedMacos").get() as LinkSharedLibrary).linkedFile.get())

//      tasks.named("linkRelease").get().outputs.files.filter { it.isFile }.forEach { println(it) }
//      println(tasks.named("linkRelease").get().outputs.files.filter { it.isDirectory }.asPath)
    }
  }
}