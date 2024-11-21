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
  id("sandbox.kotlin-conventions")
  id("sandbox.test-conventions")
  alias(libs.plugins.download)
}

javaConvention {
  languageVersion = 21
}

// repositories {
//   // https://github.com/weisJ/JavaNativeFoundation/packages/877408
//   maven("https://maven.pkg.github.com/weisJ/JavaNativeFoundation") {
//     credentials {
//       username = "bric3"
//       password = providers.environmentVariable("GITHUB_TOKEN").get()
//     }
//   }
// }

// Downloaded on 2024-04-05 from https://www.jhlabs.com/ip/filters/download.html
sourceSets {
  create("jhLabs")
}

dependencies {
  // implementation("com.jhlabs:filters:2.0.235-1")
  implementation(sourceSets["jhLabs"].output)

  implementation("org.violetlib:vaqua:13")
  // implementation("com.github.weisj:java-native-foundation:1.1.0:zip")
  implementation(libs.miglayout)
  implementation(libs.bundles.flatlaf)
}

val jnfLocation = layout.buildDirectory.map { "$it/jnf" }

tasks {
  // Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
  withType<JavaExec>().configureEach {
    group = "class-with-main"

    environment = mapOf(
      "DYLD_FRAMEWORK_PATH" to "${jnfLocation.get()}:/System/Library/Frameworks:/System/Library/Frameworks/JavaVM.framework/Frameworks"
    )
  }

  // Todo rework as repository ?
  val downloadAndExtractJnfFramework by registering {
    doLast {
      val downloadDestination = layout.buildDirectory.map { "$it/jnf/java-native-foundation-1.1.0.framework.zip" }

      download.run {
        src("https://maven.pkg.github.com/weisJ/JavaNativeFoundation/com/github/weisj/java-native-foundation/1.1.0/java-native-foundation-1.1.0.framework.zip")
        dest(downloadDestination)
        onlyIfModified(true)
        useETag("all") // Use the ETag on GH

        // auth
        // export GITHUB_TOKEN=$(op item get "GitHub" --fields "gh-cli")
        username("bric3")
        password(providers.environmentVariable("GITHUB_TOKEN").get())
      }

      // from https://maven.pkg.github.com/weisJ/JavaNativeFoundation/com/github/weisj/java-native-foundation/1.1.0/java-native-foundation-1.1.0.framework.zip.sha1
      verifyChecksum.run {
        src(downloadDestination)
        algorithm("SHA-1")
        checksum("81167dd0d2f63512ac920c0854af24b4f2561196")
      }

      copy {
        from(zipTree(downloadDestination))
        into(jnfLocation.map { "$it/JavaNativeFoundation.framework" })
      }
    }
  }

  // cannot be automated, because the dep is on GitHub packages
  // processResources {
  //   dependsOn(downloadAndExtractJnfFramework)
  // }
}