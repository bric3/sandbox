/*
 * sandbox
 *
 * Copyright (c) 2021, today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

// Required for nokee...
// See
// * https://github.com/nokeedev/gradle-native/issues/349
// * https://github.com/nokeedev/gradle-native/issues/350
pluginManagement {
  repositories {
    maven {
      name = "Nokee Release Repository"
      url = uri("https://repo.nokee.dev/release")
      mavenContent {
        includeGroupByRegex("dev\\.nokee.*")
        includeGroupByRegex("dev\\.gradleplugins.*")
      }
    }
    maven {
      name = "Nokee Snapshot Repository"
      url = uri("https://repo.nokee.dev/snapshot")
      mavenContent {
        includeGroupByRegex("dev\\.nokee.*")
        includeGroupByRegex("dev\\.gradleplugins.*")
      }
    }
    gradlePluginPortal()
  }
  val nokeeVersion = "0.4.3129-202303171612.d413fb13"  // found on https://services.nokee.dev/versions/latest-snapshot.json
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("dev.nokee.")) {
        useModule("${requested.id.id}:${requested.id.id}.gradle.plugin:${nokeeVersion}")
      }
    }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

// Doc https://docs.gradle.org/7.2/userguide/platforms.html
// API https://docs.gradle.org/7.2/javadoc/org/gradle/api/initialization/dsl/VersionCatalogBuilder.html

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "sandbox"

includeBuild("conventions")
include("kotlin")
include("jmh-stuff", "jmh-panama")
include("jdk11", "jdk17", "jdk18", "jdk21", "jdk23")
include("native:cmem", "native:dlopen")
include("jmh-panama")
include("graal:run-with-graal", "graal:run-with-libgraal")
include("swing")
include("sa-agent")

// Swift language broken on sequoia (swift 6)
// See https://github.com/gradle/gradle-native/issues/1116
// val os = DefaultNativePlatform.getCurrentOperatingSystem()
// if (os.isMacOsX) {
//   include(
//     "swift-app",
//     "swift-library",
//   )
// }
