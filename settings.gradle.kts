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
  val nokeeVersion = "0.4.556-202110111448.5620125b"  // found on https://services.nokee.dev/versions/all.json
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("dev.nokee.")) {
        useModule("${requested.id.id}:${requested.id.id}.gradle.plugin:${nokeeVersion}")
      }
    }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

// Doc https://docs.gradle.org/7.2/userguide/platforms.html
// API https://docs.gradle.org/7.2/javadoc/org/gradle/api/initialization/dsl/VersionCatalogBuilder.html

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")



rootProject.name = "sandbox"

include("kotlin")
include("jmh-stuff", "jmh-panama")
include("jdk11", "jdk17", "jdk18", "jdk19")
include("cmem", "swift-app", "swift-library")
include("jmh-panama")
include("graal:run-with-graal", "graal:run-with-libgraal")

val os = DefaultNativePlatform.getCurrentOperatingSystem()
if (os.isMacOsX) {
  include("swift-app", "swift-library")
}
