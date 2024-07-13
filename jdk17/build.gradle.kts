/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.nio.file.Files
import java.nio.file.Path

plugins {
    id("sandbox.java-conventions")
}

javaConvention {
    languageVersion = 17
    addedModules = setOf("jdk.incubator.foreign")
}

dependencies {
  implementation("org.jsoup:jsoup:1.18.1")
}

// See https://docs.gradle.org/current/userguide/cross_project_publications.html
tasks.register("swiftLib") {
  val os = DefaultNativePlatform.getCurrentOperatingSystem()
  if (!os.isMacOsX && !os.isLinux) {
    throw GradleException("Swift compilation is only working on macOS and Linux")
  }

  dependsOn(":swift-library:assembleReleaseSharedMacos")
  doLast {
    val sharedLib = tasks.getByPath(":swift-library:linkReleaseSharedMacos").outputs.files.filter { it.isFile }
    copy {
      from(sharedLib.asFileTree)
      into(sourceSets.main.get().output.resourcesDir!!)
    }
  }
}

tasks.withType<JavaExec>().configureEach {
  dependsOn(tasks.compileJava, "swiftLib") // for IntelliJ run main class
  group = "class-with-main"


  jvmArgs(
          "--enable-native-access=ALL-UNNAMED",
  )

  environment = mapOf(
          "JAVA_LIBRARY_PATH" to sourceSets.main.get().output.resourcesDir!! // for IntelliJ run main class
//          "JAVA_LIBRARY_PATH" to ".:/usr/local/lib"
  )
}

tasks.register<JavaExec>("ra") {
  mainClass.set("other.RaAssembler")
  environment = System.getenv() as Map<String, Any>
}

tasks.register<JavaExec>("defineAnonymousClass") {
  mainClass.set("sandbox.DefineAnonymousClass")
  args = listOf("Goodbye Unsafe::defineAnonymousClass")
}

// JAVA_LIBRARY_PATH=jdk17/build/resources/main/ java --enable-native-access=ALL-UNNAMED --add-modules=jdk.incubator.foreign -cp jdk17/build/classes/java/main sandbox.TouchId
tasks.register<JavaExec>("touchId") {
  dependsOn(tasks.compileJava, "swiftLib")
  mainClass.set("sandbox.TouchId")
  environment = mapOf(
          "JAVA_LIBRARY_PATH" to sourceSets.main.get().output.resourcesDir!!
//          "JAVA_LIBRARY_PATH" to ".:/usr/local/lib"
  )
}

sourceSets {
  val jextract by creating  {
    java.srcDirs("$buildDir/generated/sources/jextract/java")
    resources.srcDirs("$buildDir/generated/sources/jextract/resources")
  }
  @Suppress("UNUSED_VARIABLE")
  val main by getting {
    java.srcDirs(jextract.java.srcDirs)
    resources.srcDirs(jextract.resources.srcDirs)
//    compileClasspath += sourceSets["jextract"].output
//    runtimeClasspath += sourceSets["jextract"].output
  }
}

tasks.register<Exec>("jextractSDLHeaders") {
  val jextract = (project.findProperty("jextract") as String).replace("\$HOME", System.getProperty("user.home"))
  val sdl2Headers = "/usr/local/include/SDL2"
  val platformIncludes = "/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include/"
//  val platformIncludes = "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include"

  doFirst {
    if (Files.notExists(Path.of(jextract))) {
      throw InvalidUserCodeException("jextract not found at $jextract")
    }

    if (Files.notExists(Path.of(sdl2Headers))) {
      throw InvalidUserCodeException("SDL2 headers not found at $sdl2Headers, try installing via 'brew install sdl2'")
    }

    if (Files.notExists(Path.of(platformIncludes))) {
      throw InvalidUserCodeException("Platform includes not found at $platformIncludes")
    }
  }

  val targetDirectory = sourceSets["jextract"].java.sourceDirectories.first()
  val targetPackage = "sdl2"

  workingDir = project.projectDir
  executable = jextract
  args(
          "--source",
          "-d", targetDirectory,
          "--target-package", targetPackage,
          "-I", platformIncludes,
          "-I", sdl2Headers,
          "-l", "SDL2",
          "--header-class-name", "LibSDL2",
          /* resolved via argument provider */
  )
  argumentProviders.add {
    val tmpHeader = Files.createTempFile("", "sdl-foo.h")
    Files.writeString(
            tmpHeader,
            """
            #include <SDL.h>
            #include <SDL_opengl.h>              
            """.trimIndent()
    )
    logger.info("Created temporary header file: {}", tmpHeader)
    listOf(tmpHeader.toAbsolutePath().toString())
  }

  outputs.dir(targetDirectory.toPath().resolve(targetPackage.replace('.', '/')))

  // Not using https://github.com/krakowski/gradle-jextract
  // because it relies on the toolchain api which is not able to distinguish yet
  // some between stock JDK17 and one from project panama.
  // see https://github.com/gradle/gradle/issues/18168
  // Moreover, the jextract tool is unlikely to be shipped in the JDK.
}


tasks.register<JavaExec>("runSDLFoo") {
  dependsOn("jextractSDLHeaders")
  // JAVA_LIBRARY_PATH=:/usr/local/lib java \
  //    -cp build/classes/java/main \
  //    --enable-native-access=ALL-UNNAMED \
  //    --add-modules=jdk.incubator.foreign \
  //    -XstartOnFirstThread \
  //    sandbox.SDLFoo
  mainClass.set("sandbox.SDLFoo")
  jvmArgs(
          "--enable-native-access=ALL-UNNAMED",
      //"--add-modules=jdk.incubator.foreign",
          "-XstartOnFirstThread"
  )
  environment("JAVA_LIBRARY_PATH", ":/usr/local/lib")
  classpath(sourceSets.main.get().runtimeClasspath)

  // need to set the project's toolchain explicitly see: https://github.com/gradle/gradle/issues/16791
  javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

tasks.compileJava {
    // -g:none
    options.debugOptions.debugLevel = "none"
    options.debugOptions.debugLevel

    options.isDebug
}