import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.nio.file.Files
import java.nio.file.Path

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
  id("sandbox.java-conventions")
}

javaConvention {
  languageVersion = 21
  openedModules = mapOf(
    "java.base/jdk.internal.vm" to "ALL-UNNAMED"
  )
  enablePreview = true
}

dependencies {
  implementation(libs.bundles.flightRecorder)
  implementation(libs.flexmark.all)
  testImplementation(libs.bundles.junit.jupiter)
  testImplementation(libs.bundles.mockito)
}

// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>().configureEach {
  group = "class-with-main"
  dependsOn(tasks.compileJava, "swiftLib") // for IntelliJ run main class

  environment = mapOf(
    "JAVA_LIBRARY_PATH" to sourceSets.main.get().output.resourcesDir!!, // for IntelliJ run main class
    // "JAVA_LIBRARY_PATH" to ".:/usr/local/lib",
    "FINNHUB_TOKEN" to System.getenv("FINNHUB_TOKEN"),
    "HTTP_CLIENT_CARRIER_THREADS" to System.getenv("HTTP_CLIENT_CARRIER_THREADS"),
  )

  // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
  jvmArgs(
    "--enable-native-access=ALL-UNNAMED",
    "-Xlog:os+container"
  )
}

// See https://docs.gradle.org/current/userguide/cross_project_publications.html
val swiftLib by tasks.registering {
  val os = DefaultNativePlatform.getCurrentOperatingSystem()
  if (!os.isMacOsX) {
    logger.warn("Swift compilation is only working on macOS and not configured on Linux")
    // throw GradleException("Swift compilation is only working on macOS and not configured on Linux")
    return@registering
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

// JAVA_LIBRARY_PATH=jdk17/build/resources/main/ java --enable-native-access=ALL-UNNAMED --add-modules=jdk.incubator.foreign -cp jdk17/build/classes/java/main sandbox.TouchId
val touchId by tasks.registering(JavaExec::class) {
  dependsOn(tasks.compileJava, swiftLib)
  mainClass.set("sandbox.panama.TouchId")
  environment = mapOf(
    "JAVA_LIBRARY_PATH" to sourceSets.main.get().output.resourcesDir!!
//          "JAVA_LIBRARY_PATH" to ".:/usr/local/lib"
  )
}

sourceSets {
  val jextract by creating  {
    java.srcDirs(layout.buildDirectory.map { it.dir("/generated/sources/jextract/java") })
    resources.srcDirs(layout.buildDirectory.map { it.dir("/generated/sources/jextract/resources") })
  }
  @Suppress("UNUSED_VARIABLE")
  val main by getting {
    java.srcDirs(jextract.java.srcDirs)
    resources.srcDirs(jextract.resources.srcDirs)
//    compileClasspath += sourceSets["jextract"].output
//    runtimeClasspath += sourceSets["jextract"].output
  }
}

// dynamic agent loading
tasks.test {
  jvmArgs(
    // Prevents the following warning:
    // WARNING: A Java agent has been loaded dynamically (/Users/brice.dutheil/.gradle/caches/modules-2/files-2.1/net.bytebuddy/byte-buddy-agent/1.14.4/3bf5ac1104554908cc623e40e58a00be37c35f36/byte-buddy-agent-1.14.4.jar)
    // WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
    // WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
    // WARNING: Dynamic loading of agents will be disallowed by default in a future release
    "-XX:+EnableDynamicAgentLoading",
  )
}

val jextractSDLHeaders = tasks.registering(Exec::class) {
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